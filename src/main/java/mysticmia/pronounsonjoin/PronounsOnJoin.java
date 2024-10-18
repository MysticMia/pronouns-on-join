package mysticmia.pronounsonjoin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Unique;

public class PronounsOnJoin implements ModInitializer {
	public static final String MOD_ID = "pronouns-on-join";
	public static final ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).get();

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(modContainer.getMetadata().getName());

	@Override
	public void onInitialize() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
//		LOGGER.info("{} is initializing server in version {}!", modContainer.getMetadata().getName(), modContainer.getMetadata().getVersion());
	}
}