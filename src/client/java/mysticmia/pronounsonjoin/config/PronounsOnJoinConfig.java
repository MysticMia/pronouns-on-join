package mysticmia.pronounsonjoin.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.util.Identifier;

import java.awt.*;

public class PronounsOnJoinConfig {
    private static final String CONFIG_FILE_NAME = "pronouns-on-join-config.json5";

    @SerialEntry(comment = "Whether to send someone's pronouns when they join the server.")
    public boolean showJoinMessages = true;

    //#region Colors
    @SerialEntry
    public Color themeColor = new Color(16777045);

    @SerialEntry
    public Color themeReference = new Color(16755200);

    @SerialEntry
    public Color themeEdited = new Color(16777215);

    @SerialEntry
    public Color themeError = new Color(16733525);

    @SerialEntry
    public Color themeUnknownPronouns = new Color(11184810);
    //#endregion Colors

    public static ConfigClassHandler<PronounsOnJoinConfig> HANDLER = ConfigClassHandler.createBuilder(PronounsOnJoinConfig.class)
            .id(Identifier.of("mysticmia.pronounsonjoin", CONFIG_FILE_NAME))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve(CONFIG_FILE_NAME))
                    .setJson5(true)
                    .build())
            .build();
}
