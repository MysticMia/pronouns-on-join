package mysticmia.pronounsonjoin.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.util.Identifier;

public class PronounsOnJoinConfig {
    private static final String CONFIG_FILE_NAME = "pronouns-on-join-config.json5";

    @SerialEntry(comment = "Whether to send someone's pronouns when they join the server.")
    public boolean showJoinMessages = true;

    public static ConfigClassHandler<PronounsOnJoinConfig> HANDLER = ConfigClassHandler.createBuilder(PronounsOnJoinConfig.class)
            .id(Identifier.of("mysticmia.pronounsonjoin", CONFIG_FILE_NAME))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve(CONFIG_FILE_NAME))
                    .setJson5(true)
                    .build())
            .build();
}
