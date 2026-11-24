package io.github.playerblocker.mixin;

import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drops the blocked player's sidebar scoreboard line.
 *
 * <p>Hooked at the filter predicate that decides which score entries survive, not at the drawing
 * code. That matters: the sidebar's column layout is computed from the surviving array, so
 * removing a line here shortens the box cleanly instead of leaving a blank row or an off-by-one
 * offset. Only the name is available on this path, so it is a text match — which is exactly why
 * this channel ships disabled by default.</p>
 */
@Mixin(Hud.class)
public class SidebarMixin {

    @Inject(method = "lambda$displayScoreboardSidebar$0", at = @At("HEAD"), cancellable = true)
    private static void pb$hideSidebarLine(PlayerScoreEntry entry, CallbackInfoReturnable<Boolean> cir) {
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.anyHas(HideTarget.SIDEBAR)) {
            return;
        }
        String owner = entry.owner();
        if (manager.hidesName(owner, HideTarget.SIDEBAR)
                || manager.hidesName(entry.ownerName().getString(), HideTarget.SIDEBAR)) {
            cir.setReturnValue(false);
        }
    }
}
