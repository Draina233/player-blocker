package io.github.playerblocker.mixin;

import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Removes blocked players from the player-list data source, not from its rendering.
 *
 * <p>This distinction is what makes the mod work next to BetterTab: BetterTab throws away the
 * vanilla tab renderer and rebuilds the whole list itself, but it builds it from
 * {@code getPlayerInfos()}, which is a sort over {@code getListedOnlinePlayers()}. Filter that
 * one collection and every consumer — vanilla tab, BetterTab, and anything else built on top —
 * sees a shorter list without knowing why.</p>
 */
@Mixin(value = ClientPacketListener.class, priority = 900)
public class ClientPacketListenerMixin {

    /** Tab list source. Also feeds BetterTab's rebuild. */
    @Inject(method = "getListedOnlinePlayers", at = @At("RETURN"), cancellable = true)
    private void pb$filterTabList(CallbackInfoReturnable<Collection<PlayerInfo>> cir) {
        Collection<PlayerInfo> filtered = filter(cir.getReturnValue(),
                HideTarget.TAB, HideTarget.TAB);
        if (filtered != cir.getReturnValue()) {
            cir.setReturnValue(filtered);
        }
    }

    /**
     * Every online player, regardless of tab-list visibility. Used by command suggestions and
     * the social-interactions screen, so it answers to both of those channels.
     */
    @Inject(method = "getOnlinePlayers", at = @At("RETURN"), cancellable = true)
    private void pb$filterOnlinePlayers(CallbackInfoReturnable<Collection<PlayerInfo>> cir) {
        Collection<PlayerInfo> filtered = filter(cir.getReturnValue(),
                HideTarget.SUGGESTIONS, HideTarget.TAB);
        if (filtered != cir.getReturnValue()) {
            cir.setReturnValue(filtered);
        }
    }

    /**
     * Returns the original collection untouched when nothing is hidden, so the common case costs
     * one scan and no allocation.
     */
    private static Collection<PlayerInfo> filter(Collection<PlayerInfo> src,
                                                 HideTarget first, HideTarget second) {
        if (src == null || src.isEmpty()) {
            return src;
        }
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.hasEntries() || !manager.masterEnabled()) {
            return src;
        }

        boolean anyHidden = false;
        for (PlayerInfo info : src) {
            UUID id = info.getProfile().id();
            if (manager.hidesAny(id, first, second)) {
                anyHidden = true;
                break;
            }
        }
        if (!anyHidden) {
            return src;
        }

        List<PlayerInfo> out = new ArrayList<>(Math.max(1, src.size() - 1));
        for (PlayerInfo info : src) {
            if (!manager.hidesAny(info.getProfile().id(), first, second)) {
                out.add(info);
            }
        }
        return out;
    }
}
