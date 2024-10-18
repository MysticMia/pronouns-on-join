package mysticmia.pronounsonjoin;

// AI wrote half of this
// I have no clue what im doing
//    - Mia, 2024-10-03
// I'm procrastinating for my Haskell test, while playing minecraft, instead of
// playing VRChat like I wanted to.

// also i could use this website to find offline people's UUIDs
// https://playerdb.co/api/player/minecraft/skulltron300
// for example. It also works the other way around! (filling in uuid to get username)

// Also I want to add colors to pronouns when people join:
//  Gray out if people don't have pronouns (light gray, i guess; still readable)

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import mysticmia.pronounsonjoin.mixin.client.EntitySelectorAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.*;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class PronounsOnJoinClient implements ClientModInitializer {
    public static final ModContainer modContainer = FabricLoader.getInstance().getModContainer("pronouns-on-join").get();
    public static final Logger LOGGER = LoggerFactory.getLogger(modContainer.getMetadata().getName());

    static Formatting themeText = Formatting.YELLOW; // default text color
    static Formatting themeReference = Formatting.GOLD; // for feedback to global variables
    static Formatting themeEdited = Formatting.WHITE; // for feedback when someone makes a change
    static Formatting themeError = Formatting.RED; // text for errors

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        LOGGER.info("{} is initializing client in version {}!", modContainer.getMetadata().getName(), modContainer.getMetadata().getVersion());
        // Register the client-side command listener for /pronouns
        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("pronouns")
                .then(literal("help")
                        .executes(this::sendPronounHelp)
                )
                .then(literal("list")
                        .executes(this::sendPronounList)
                        .then(literal("unknown")
                                .executes(this::listPlayersUnknownPronouns))
                )
                .then(literal("check")
                        .then(argument("player", EntityArgumentType.player())
                            .executes(this::checkPronounFromPlayer)
                        )
                )
                .then(literal("set")
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("pronouns", StringArgumentType.greedyString())
                                    .executes(this::setPlayerPronouns)
                                )
                        )
                )
        );
    }

    private static Map<UUID, String> _getPlayers() {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert world != null;
        HashMap<UUID, String> players = new HashMap<>();
        LOGGER.info("--a1");
        for (AbstractClientPlayerEntity player : world.getPlayers()) {
            LOGGER.info("{} --- {}", player.getGameProfile().getName(), player.getUuid());
            players.put(player.getUuid(), player.getGameProfile().getName());
        }
        LOGGER.info("--a2");
//        return players;
//    }
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        assert handler != null;
        Object[] playerList = handler.getPlayerList().toArray();
//        HashMap<String, UUID> players = new HashMap<>();
//        players.clear();
        LOGGER.info("--b1");
        for (Object playerlistEntry : playerList) {
            LOGGER.info("{} --- {}", ((PlayerListEntry)playerlistEntry).getProfile().getName(), ((PlayerListEntry)playerlistEntry).getProfile().getId());
            players.put(
                    ((PlayerListEntry) playerlistEntry).getProfile().getId(),
                    ((PlayerListEntry) playerlistEntry).getProfile().getName()
            );
        }
        LOGGER.info("--b2");
//        return players;

//        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
//        assert handler != null;
        /*Object[]*/ playerList = handler.getListedPlayerListEntries().toArray();
//        Object[] playerList = handler.getPlayerList().toArray();
//        HashMap<String, UUID> players = new HashMap<>();
//        players.clear();
//        LOGGER.info("--c1");
        for (Object playerlistEntry : playerList) {
            LOGGER.info("{} --- {}", ((PlayerListEntry)playerlistEntry).getProfile().getName(), ((PlayerListEntry)playerlistEntry).getProfile().getId());
            players.put(
                    ((PlayerListEntry) playerlistEntry).getProfile().getId(),
                    ((PlayerListEntry) playerlistEntry).getProfile().getName()
            );
        }
//        LOGGER.info("--c2");
//        return players;
//        LOGGER.info("--d1");
        Object[] ids = handler.getPlayerUuids().toArray(); // Make an array with the UUID of every player
        for (Object id : ids) { // for loop that runs each UUID into getPlayerListEntry and gets the name
//            LOGGER.info("{} --- {}", Objects.requireNonNull(handler.getPlayerListEntry((UUID)id)).getProfile().getName(), id);
            players.put(
                    (UUID)id,
                    Objects.requireNonNull(handler.getPlayerListEntry((UUID)id)).getProfile().getName()
            );
        }
//        LOGGER.info("--d2");
        return players;
    }

    private static CompletableFuture<UUID> fetchPlayerUuid(String playerName) {
        String apiUrl = "https://playerdb.co/api/player/minecraft/" + playerName;

        // Create HttpClient to perform the request
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl)).header("User-Agent", modContainer.getMetadata().getName() + " " + modContainer.getMetadata().getVersion() + " Minecraft Mod")
                    .build();

            // Perform the asynchronous HTTP request
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(response -> {
                        // Parse the JSON response
                        Type type = new TypeToken<Map<String, Object>>() {}.getType();
                        Map<String, Object> responseJson = new Gson().fromJson(response, type);
                        // code, message, = string; data = dict (see below), success = bool
                        //   player
                        //     meta = dict<string,int>, username, id (dashes), raw_id (no dashes), avatar, skin_texture, = string.
                        //     properties = list<dict<string,string>>, name_history = list<?>

                        if (!(boolean)responseJson.get("success")) {
                            LOGGER.info("Success = False, aborting");
                            return null;
                        }
                        Map<String, Object> dataData = (Map<String, Object>)responseJson.get("data");
                        Map<String, Object> playerData = (Map<String, Object>)dataData.get("player");
                        return UUID.fromString((String)playerData.get("id"));
                        //return UUID.fromString(id);
                    });
        }
    }

    private static CompletableFuture<UUID> getUuidFromPlayerListOrApi(String playerName) {
        for (Map.Entry<UUID, String> playerEntry : _getPlayers().entrySet()) {
            if (playerEntry.getValue().equals(playerName)) {
                return CompletableFuture.completedFuture(playerEntry.getKey());
            }
        }
        return fetchPlayerUuid(playerName);
    }

    private int sendPronounHelp(@NotNull CommandContext<FabricClientCommandSource> context) {
        // return feedback to chat
        MutableText commandResponse = Text.literal("This mod lets you see the pronouns of whoever joins your world:\n").formatted(themeText);
        commandResponse.append( Text.literal(" /pronouns help").formatted(themeReference) );
        commandResponse.append( Text.literal("  - brings up this help page!\n").formatted(themeText) );
        commandResponse.append( Text.literal(" /pronouns list [\"unknown\"]").formatted(themeReference) );
        commandResponse.append( Text.literal("  - lets you see the pronouns (or lack thereof!) of everyone in the world.\n").formatted(themeText) );
        commandResponse.append( Text.literal(" /pronouns check <player>").formatted(themeReference) );
        commandResponse.append( Text.literal("  - check the pronouns of a specific player (if you don't want to clog up your entire chat with the full player list every time...\n").formatted(themeText) );
        commandResponse.append( Text.literal(" /pronouns set <player> <pronouns>").formatted(themeReference) );
        commandResponse.append( Text.literal("  - Assign pronouns to a player!").formatted(themeText) );
        context.getSource().sendFeedback(commandResponse);
        return 1;
    }

    private int sendPronounList(@NotNull CommandContext<FabricClientCommandSource> context) {
        // fetch pronouns (and store a mapping to convert UUIDs back to readable usernames)
        Map<UUID, String> onlinePlayers = _getPlayers();
        Map<UUID, Text> playerUUIDmap = new HashMap<>();
        ArrayList<UUID> uuids = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : onlinePlayers.entrySet()) {
            playerUUIDmap.put(entry.getKey(), Text.literal(entry.getValue()));
            uuids.add(entry.getKey());
        }

        Map<UUID, String> usersPronouns = PronounRetriever.getPronounsBulk(uuids);

        // error handling
        if (usersPronouns.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("Nobody in the server has pronouns set!").formatted(themeText));
            return 0;
        }

        // return feedback to chat
        MutableText response = Text.literal("Online Players:").formatted(themeText);
        for (Map.Entry<UUID, String> entry : usersPronouns.entrySet()) { // will be Map<uuid, pronouns>
            response.append(Text.literal("\n"));
            response.append( ((MutableText)playerUUIDmap.get(entry.getKey())).formatted(themeReference) ); // key = uuid -> Text<Player>
            response.append( Text.literal(" : ").formatted(themeText) );
            response.append( Text.literal(entry.getValue()).formatted(themeEdited) ); // value = pronouns -> String
        }
        LOGGER.info(response.getString());
        context.getSource().sendFeedback(response);
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    private int listPlayersUnknownPronouns(@NotNull CommandContext<FabricClientCommandSource> context) {
        // fetch pronouns (and store a mapping to convert UUIDs back to readable usernames)
        Map<UUID, String> onlinePlayers = _getPlayers();
        Map<UUID, MutableText> playerUUIDmap = new HashMap<>(); // key = uuid, value = username
        ArrayList<UUID> uuids = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : onlinePlayers.entrySet()) {
            playerUUIDmap.put(entry.getKey(), Text.literal(entry.getValue()));
            uuids.add(entry.getKey());
        }

        Map<UUID, String> usersPronouns = PronounRetriever.getPronounsBulk(uuids); // key = uuid, value = pronouns

        if (usersPronouns.size() == onlinePlayers.size()) {
            context.getSource().sendFeedback(Text.literal("You know everyone's pronouns!").formatted(themeText));
            return 1;
        }

        MutableText response = Text.literal("Players with unknown pronouns:\n").formatted(themeText);
        for (Map.Entry<UUID, String> entry : onlinePlayers.entrySet()) { // key = uuid, value = username
            if (!usersPronouns.containsKey(entry.getKey())) {
                response.append( playerUUIDmap.get(entry.getKey()).formatted(themeReference) );
                response.append( Text.literal(", ").formatted(themeText) );
            }
        }
        // return feedback to chat
        context.getSource().sendFeedback(response);
        return 1;
    }

    private int checkPronounFromPlayer(@NotNull CommandContext<FabricClientCommandSource> context) {
        // fetch player
        EntitySelector selector = context.getArgument("player", EntitySelector.class);
        String playerName = ((EntitySelectorAccess)selector).getPlayerName();
        LOGGER.info("Player name: {}", playerName);
        getUuidFromPlayerListOrApi(playerName)
                .thenAccept(playerUUID -> {
                    // error handling
                    if (handleNoPlayerFound(context, playerName, playerUUID)) return;

                    String pronouns = PronounRetriever.getPronouns(playerUUID);

                    // return feedback to chat
                    MutableText response = Text.literal(playerName).formatted(themeReference);
                    if (pronouns == null || pronouns.isEmpty()) {
                        response.append( Text.literal(" does not go by any pronouns yet.").formatted(themeText) );
                    } else {
                        response.append( Text.literal(" goes by: ").formatted(themeText) );
                        response.append( Text.literal(pronouns).formatted(themeReference) );
                    }
                    context.getSource().sendFeedback(response);
                });
        return 1; // idk, this doesn't really do much anymore I guess.
    }

    private int setPlayerPronouns(@NotNull CommandContext<FabricClientCommandSource> context) {
        // fetch player
        EntitySelector selector = context.getArgument("player", EntitySelector.class);
        String playerName = ((EntitySelectorAccess)selector).getPlayerName();
        getUuidFromPlayerListOrApi(playerName)
                .thenAccept(playerUUID -> {
                    // error handling
                    if (handleNoPlayerFound(context, playerName, playerUUID)) return;

                    // fetch previous pronouns and set new ones
                    String newPronouns = context.getArgument("pronouns", String.class);
                    String oldPronouns = PronounRetriever.getPronouns(playerUUID);
                    MutableText response = Text.literal("Changed pronouns of ").formatted(themeText);
                    response.append( Text.literal(playerName).formatted(themeReference) );

                    // more error handling
                    if (!PronounRetriever.setPronouns(playerUUID, newPronouns)) {
                        response = Text.literal("Error: Couldn't store user's new pronouns!").formatted(themeError);
                        context.getSource().sendFeedback(response);
                        return;
                    }

                    // return feedback to chat
                    if (oldPronouns != null && !oldPronouns.isEmpty()) {
                        response.append(Text.literal(" from ").formatted(themeText));
                        response.append(Text.literal(oldPronouns).formatted(themeReference));
                    }
                    response.append( Text.literal(" to ").formatted(themeText) );
                    response.append( Text.literal(newPronouns).formatted(themeEdited) );
                    response.append( Text.literal(".").formatted(themeText) );
                    context.getSource().sendFeedback(response);
                });

        return 1; // this probably serves no real purpose since it'll always be 1 due to the api call.
    }

    private boolean handleNoPlayerFound(@NotNull CommandContext<FabricClientCommandSource> context, String playerName, UUID playerUUID) {
        if (playerUUID == null) {
            MutableText response = Text.literal("Player `").formatted(themeError);
            response.append( Text.literal(playerName).formatted(themeReference) );
            response.append( Text.literal("` not found!").formatted(themeError) );
            context.getSource().sendFeedback(response);
            return true;
        }
        return false;
    }
}

