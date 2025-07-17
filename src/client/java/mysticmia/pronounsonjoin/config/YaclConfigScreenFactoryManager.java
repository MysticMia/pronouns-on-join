package mysticmia.pronounsonjoin.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

public class YaclConfigScreenFactoryManager {

    private static ConfigCategory getMainSettings(PronounsOnJoinConfig config) {
        return ConfigCategory.createBuilder()
                .name(Text.literal("Main toggles"))
                .tooltip(Text.of("A selection of the most important toggles"))
                .group(OptionGroup.createBuilder()
                        .name(Text.literal("Todo1."))
                        .description(OptionDescription.of(
                                Text.literal("Todo2."))
                        )
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Join messages"))
                                .description(OptionDescription.of(
                                        Text.literal("Enable to send someone's pronouns when they join the server."))
                                )
                                .binding(
                                        true,
                                        () -> config.showJoinMessages,
                                        newVal -> config.showJoinMessages = newVal
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )
                        .build()
                )
                .build();
    }

    private static ConfigCategory getColorSettings(PronounsOnJoinConfig config) {
        return ConfigCategory.createBuilder()
                .name(Text.literal("Colors"))
                .tooltip(Text.of("Todo3"))
                .group(OptionGroup.createBuilder()
                        .name(Text.literal("Chat colors"))
                        .description(OptionDescription.of(
                                Text.literal("Customize the chat colors :)"))
                        )
                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Default text color"))
                                .description(OptionDescription.of(
                                        Text.literal("The color of most text."))
                                )
                                .binding(
                                        new Color(16777045),
                                        () -> config.themeColor,
                                        newVal -> config.themeColor = newVal
                                )
                                .controller(ColorControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Reference text color"))
                                .description(OptionDescription.of(
                                        Text.literal("The color of references to chat commands or usernames."))
                                )
                                .binding(
                                        new Color(16755200),
                                        () -> config.themeReference,
                                        newVal -> config.themeReference = newVal
                                )
                                .controller(ColorControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Edited text color"))
                                .description(OptionDescription.of(
                                        Text.literal("The color of pronouns after you add or update them."))
                                )
                                .binding(
                                        new Color(16777215),
                                        () -> config.themeEdited,
                                        newVal -> config.themeEdited = newVal
                                )
                                .controller(ColorControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Error text color"))
                                .description(OptionDescription.of(
                                        Text.literal("The color of error and warning messages."))
                                )
                                .binding(
                                        new Color(16733525),
                                        () -> config.themeError,
                                        newVal -> config.themeError = newVal
                                )
                                .controller(ColorControllerBuilder::create)
                                .build()
                        )
                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Unknown pronoun color"))
                                .description(OptionDescription.of(
                                        Text.literal("The color of the join message when a player's pronouns are not known."))
                                )
                                .binding(
                                        new Color(11184810),
                                        () -> config.themeUnknownPronouns,
                                        newVal -> config.themeUnknownPronouns = newVal
                                )
                                .controller(ColorControllerBuilder::create)
                                .build()
                        )
                        .build()

                )
                .build();
    }

    public static YetAnotherConfigLib getModConfig() {
        return YetAnotherConfigLib.create(PronounsOnJoinConfig.HANDLER, (
            (defaults, config, builder) -> builder
                .title(Text.literal("Settings"))
                .category(getMainSettings(config))
                .category(getColorSettings(config))
                .save(PronounsOnJoinConfig.HANDLER::save)
            )
        );
    }

    public static Screen createScreen(Screen parentScreen) {
        return getModConfig().generateScreen(parentScreen);
    }
}
