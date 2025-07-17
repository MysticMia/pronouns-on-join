package mysticmia.pronounsonjoin.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class YaclConfigScreenFactoryManager {

    public static YetAnotherConfigLib getModConfig() {
        return YetAnotherConfigLib.create(PronounsOnJoinConfig.HANDLER, (
            (defaults, config, builder) -> builder
                .title(Text.literal("Main settings"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Main toggles"))
                        .tooltip(Text.of("Todo1."))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Todo2."))
                                .description(OptionDescription.of(
                                        Text.literal("Todo3."))
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
                        .build()
                )
                .save(PronounsOnJoinConfig.HANDLER::save)
            )
        );
    }

    public static Screen createScreen(Screen parentScreen) {
        return getModConfig().generateScreen(parentScreen);
    }
}
