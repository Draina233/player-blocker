package io.github.playerblocker.mixin;

import io.github.playerblocker.config.HideTarget;
import io.github.playerblocker.util.HiddenEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes the blocked player's dot from the Xaero minimap entity radar.
 *
 * <p>{@code RadarStateUpdater.update} walks every candidate entity and asks {@code isInvisibleTo}
 * whether it should be drawn. Answering "yes, invisible" there is the intended extension point —
 * it is the same path Xaero uses for potion-based invisibility, so the radar state stays
 * internally consistent instead of us ripping entities out of a list it maintains itself.</p>
 *
 * <p>Addressed by string so this mod compiles without Xaero present; the mixin is only applied
 * when the mod is loaded (see {@code PlayerBlockerMixinPlugin}).</p>
 */
@Mixin(targets = "xaero.hud.minimap.radar.state.RadarStateUpdater")
public class XaeroRadarMixin {

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void pb$hideFromRadar(Entity entity, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (HiddenEntities.blocked(entity, HideTarget.MINIMAP)) {
            cir.setReturnValue(true);
        }
    }
}
