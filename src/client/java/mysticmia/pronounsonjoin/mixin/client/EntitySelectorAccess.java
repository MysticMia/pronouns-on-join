package mysticmia.pronounsonjoin.mixin.client;

import net.minecraft.command.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccess {
    @Accessor("playerName")
    String getPlayerName();
}
