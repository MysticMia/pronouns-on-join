package mysticmia.pronounsonjoin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import mysticmia.pronounsonjoin.config.PronounsOnJoinConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public interface PronounRetriever
{
    String PRONOUN_DB_URL = "https://pronoundb.org/api/v2/lookup?platform=minecraft&ids=";
    ModContainer modContainer = FabricLoader.getInstance().getModContainer("pronouns-on-join").orElseThrow();
    Logger LOGGER = LoggerFactory.getLogger(modContainer.getMetadata().getName());

    // This array is a workaround to get static mutable variable in an interface. (cuz interface fields are always 'final')
    Map<UUID, Text> playersPronounFetchQueued = new HashMap<>();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    AtomicReference<ScheduledFuture<?>> lastScheduledFuture = new AtomicReference<>(); // AI wrote this

    PronounsOnJoinConfig config = PronounsOnJoinConfig.HANDLER.instance();

    static Style intToStyle(int color) { return Style.EMPTY.withColor(color); }
    static Style getThemeReference() { return intToStyle(config.themeReference.getRGB()); } // for feedback to global variables
    static Style getThemeText() { return intToStyle(config.themeColor.getRGB()); } // default text color
    static Style getThemeUnknownPronouns() { return intToStyle(config.themeUnknownPronouns.getRGB()); }

    private static void handleMultipleJoinEvents() {
        Map<UUID, Text> playerPronounFetching = new HashMap<>(playersPronounFetchQueued);
        playersPronounFetchQueued.clear();
        ArrayList<String> playerUUIDs = new ArrayList<>();
        for (UUID uuid : playerPronounFetching.keySet()) {
            playerUUIDs.add(uuid.toString());
        }
        fetchPronouns(playerUUIDs.toArray(new String[0]))
                .thenAccept(pronounResponse -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    for (Map.Entry<UUID, Text> entry : playerPronounFetching.entrySet()) {
                        UUID playerID = entry.getKey();
                        String playerName = entry.getValue().getString();
                        ArrayList<String> pronouns = getUserPronouns(pronounResponse, playerID);

                        MutableText pronounMessage = Text.literal(playerName); // colored later to make unknown players gray
                        if (!pronouns.isEmpty()) {
                            pronounMessage.setStyle(getThemeReference());
                            String playerPronouns = String.join("/", pronouns);
                            String pronounString = playerPronouns + " (from PronounDB)";
                            setPronouns(playerID, pronounString); // store so it won't have to do the call in the future
                            pronounMessage.append( Text.literal(" goes by: ").setStyle(getThemeText()) );
                            pronounMessage.append( Text.literal(pronounString).setStyle(getThemeReference()) );
                        } else {
                            pronounMessage.setStyle(getThemeUnknownPronouns());
                            pronounMessage.append( Text.literal(" does not have any pronouns.").setStyle(getThemeUnknownPronouns()) );
                            setPronouns(playerID, "");
                            // "" (empty string) = no pronouns. Prevents future API calls
                        }
                        if (client.player != null) {
                            client.execute(() -> client.player.sendMessage(pronounMessage, false));
                        }
                    }
                }).exceptionally(ex -> {
                    LOGGER.error(ex.toString());
                    return null;
                });
    }

    static void onUserJoinEvent(PlayerListS2CPacket.Entry receivedEntry, PlayerListEntry currentEntry) {
        if (!config.showJoinMessages) {
            return;
        }

        UUID playerID = currentEntry.getProfile().getId();
        MutableText response = Text.literal(currentEntry.getProfile().getName());

        // check if player already has pronoun overrides
        String pronounString = getPronouns(playerID);

        if (pronounString != null) {
            if (pronounString.isEmpty()) { // act
                response.setStyle(getThemeUnknownPronouns());
                response.append( Text.literal(" does not have any pronouns.").setStyle(getThemeUnknownPronouns()) );
            } else {
                response.setStyle(getThemeReference());
                response.append( Text.literal(" goes by: ").setStyle(getThemeText()) );
                response.append( Text.literal(pronounString).setStyle(getThemeReference()) );
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.execute(() -> client.player.sendMessage(response, false));
            }
            return;
        }

        // if the player does not have pronoun overrides yet, fetch pronouns from API.
        // Check if previous API calls are queued.
        playersPronounFetchQueued.put(playerID, response);

        // Cancel the previously scheduled task if it exists
        ScheduledFuture<?> previousTask = lastScheduledFuture.getAndSet(null);
        if (previousTask != null) {
            previousTask.cancel(false);
        }

        // Schedule a new task to run after 20 milliseconds
        ScheduledFuture<?> newTask = scheduler.schedule(PronounRetriever::handleMultipleJoinEvents, 20, TimeUnit.MILLISECONDS);
        lastScheduledFuture.set(newTask);
    }

    private static CompletableFuture<Map<UUID, Map<String, Object>>> fetchPronouns(String[] ids) {
        return fetchPronouns(String.join(",", ids));
    }

    // Make the API call to PronounDB
    private static CompletableFuture<Map<UUID, Map<String, Object>>> fetchPronouns(String ids) {
        // Build the URL for the API request
        String apiUrl = PRONOUN_DB_URL + ids;

        // Create HttpClient to perform the request
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl)).header("User-Agent", modContainer.getMetadata().getName() + " " + modContainer.getMetadata().getVersion() + " Minecraft Mod")
                    .build();

            // Perform the asynchronous HTTP request
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(response -> {
//                        LOGGER.info("User ids: {}", ids);
//                        LOGGER.info("Response: {}", response);
                        // Parse the JSON response
                        Type type = new TypeToken<Map<UUID, Map<String, Object>>>() {}.getType();
                        return new Gson().fromJson(response, type);
                    });
        }
    }

    /**
     * Helper function to parse PronounDB response to a list of pronouns.
     * @param userPronounData The data for a specific UUID from PronounDB.
     * @param uuid The matching UUID of the player.
     * @return A list of the user's preferred pronouns.
     */
    private static ArrayList<String> getUserPronouns(Map<UUID, Map<String, Object>> userPronounData, UUID uuid) {
        Map<String, Object> userData = userPronounData.get(uuid);
        Map<?, ?> sets = null;
        ArrayList<String> pronouns = new ArrayList<>();
        try {
            sets = (Map<?, ?>) userData.get("sets");
            pronouns = (ArrayList<String>) sets.get("en");
        } catch (ClassCastException e) {
            LOGGER.error("Tried to cast pronouns key 'sets' to a map of k:language and v:String[pronouns] but failed!\n{}", e.toString());
            // make sure not to return an empty list, since it may not actually be empty, but rather be unimplemented!
            return new ArrayList<String>(List.of("Error 'a'"));
        } catch (NullPointerException e) {
            if (sets == null) {
                LOGGER.warn("Tried to fetch language 'en' from user {}, but this user doesn't have any pronouns set!\n{}", uuid, e.toString());
                // don't return custom pronouns, but leave it empty as it already is. It returns an empty array at the end already.
            } else {
                LOGGER.warn("Tried to fetch language 'en' from user {}, but got languages {} instead!\n{}", uuid, sets.keySet().toArray(), e.toString());
                // make sure not to return an empty list, since it may not actually be empty, but rather be unimplemented!
                return new ArrayList<String>(List.of("Error 'b'"));
            }
        }
        return pronouns;
    }

    /**
     * Helper function to read the config file of pronoun data.
     * @return A map of &lt;UUID, pronouns&gt; of everything.
     */
    private static @NotNull Map<UUID, String> _getPronounJson() {
        Type type = new TypeToken<Map<UUID,String>>() {}.getType();
        Gson gson = new Gson();
        File file = FabricLoader.getInstance().getConfigDir().resolve("pronouns-on-join.json").toFile();
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            Map<UUID, String> result = gson.fromJson(reader, type);
            if (result == null)
                return new HashMap<>();
            return result;
        } catch (FileNotFoundException e) {
            try {
                if (!file.createNewFile()) throw new IOException("Error 'd': File might already exist?");
            } catch (IOException e1) {
                LOGGER.error("Could not create new pronouns file! {}", e.toString());
            }
            return new HashMap<>();
        }
    }

    /**
     * Get the pronouns from a list of users, from the json file.
     * @param ids A list of user's UUIDs to search for.
     * @return A map of &lt;UUID, pronouns&gt; of the specified ids.
     */
    static Map<UUID, String> getPronounsBulk(ArrayList<UUID> ids) {
        HashMap<UUID, String> pronouns = new HashMap<>();
        Map<UUID, String> pronounOverrides = _getPronounJson();
        for (Map.Entry<UUID, String> entry : pronounOverrides.entrySet()) {
            if (ids.contains(entry.getKey()) && !Objects.equals(entry.getValue(), "")) {
                pronouns.put(entry.getKey(), entry.getValue());
            }
        }
        return pronouns;

    }

    /**
     * Get a single user's pronouns, based on their UUID, from the json file.
     * @param id The UUID to search for.
     * @return A string of the user's pronouns.
     */
    static String getPronouns(UUID id) {
        Map<UUID, String> pronounOverrides = _getPronounJson();
        String pronouns = pronounOverrides.get(id);
//        if (Objects.equals(pronouns, "")) {
//            // empty string implies data is reset or not existing.
//            // Used to prevent repeat API calls to PronounDB, assuming they don't have any data on the site.
//            return null;
//        }
        return pronouns;
    }

    /**
     * A helper function to modify the pronoun config file.
     * @param pronounOverrides The data to store into the file.
     * @return Boolean of whether the data write succeeded.
     */
    private static boolean _updatePronounsFile(Map<UUID, String> pronounOverrides) {
        Gson gson = new Gson();
        try {
            try (FileWriter writer = new FileWriter(
                    FabricLoader.getInstance().getConfigDir().resolve("pronouns-on-join.json").toFile()
            )) {
                gson.toJson(pronounOverrides, writer);
            }
        }
        catch (IOException e) {
            LOGGER.warn("Couldn't store new pronouns to json file!");
            return false;
        }
        return true;
    }

    /**
     * Set a user's pronouns to a value
     * @param id The UUID to search for.
     * @param value The new value (pronouns) to set for the user
     */
    static boolean setPronouns(UUID id, String value) {
        Map<UUID, String> pronounOverrides = _getPronounJson();
        pronounOverrides.put(id, value);

        return _updatePronounsFile(pronounOverrides);
    }
}
