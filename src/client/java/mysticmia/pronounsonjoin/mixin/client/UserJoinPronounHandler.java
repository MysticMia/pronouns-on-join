package mysticmia.pronounsonjoin.mixin.client;

import mysticmia.pronounsonjoin.PronounRetriever;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ClientPacketListener.class)
public class UserJoinPronounHandler implements PronounRetriever {
    @Inject(at = @At("HEAD"), method = "applyPlayerInfoUpdate")
    private void applyPlayerInfoUpdate(
            ClientboundPlayerInfoUpdatePacket.Action action,
            ClientboundPlayerInfoUpdatePacket.Entry receivedEntry,
            PlayerInfo currentEntry,
            CallbackInfo ci
    ) {
        if (Objects.requireNonNull(action) == ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED) {
            if (receivedEntry.listed()) {
                PronounRetriever.onUserJoinEvent(receivedEntry, currentEntry);
            }
        }
    }
}
