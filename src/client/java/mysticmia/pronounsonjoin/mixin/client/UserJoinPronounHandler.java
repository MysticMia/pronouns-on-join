package mysticmia.pronounsonjoin.mixin.client;

import mysticmia.pronounsonjoin.PronounRetriever;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ClientPlayNetworkHandler.class)
public class UserJoinPronounHandler implements PronounRetriever {
    @Inject(at = @At("HEAD"), method = "handlePlayerListAction")
    private void handlePlayerListAction(PlayerListS2CPacket.Action action, PlayerListS2CPacket.Entry receivedEntry, PlayerListEntry currentEntry, CallbackInfo ci) {
        if (Objects.requireNonNull(action) == PlayerListS2CPacket.Action.UPDATE_LISTED) {
            if (receivedEntry.listed()) {
                PronounRetriever.onUserJoinEvent(receivedEntry, currentEntry);
            }
        }
    }
}