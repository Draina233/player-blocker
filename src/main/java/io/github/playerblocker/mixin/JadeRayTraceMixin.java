package io.github.playerblocker.mixin;

import io.github.playerblocker.config.HideTarget;
import io.github.playerblocker.util.HiddenEntities;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops Jade from identifying the blocked player under the crosshair.
 *
 * <p>This one cannot be solved by hiding the model: Jade raycasts the world and only then draws
 * a tooltip, and a raycast does not care whether anything was rendered. Its own entity predicate
 * {@code canBeTarget} is the single gate every candidate passes through, so vetoing there means
 * the crosshair simply finds nothing — which is what "he is not here" should look like.</p>
 */
@Mixin(targets = "snownee.jade.overlay.RayTracing")
public class JadeRayTraceMixin {

    @Inject(method = "canBeTarget", at = @At("HEAD"), cancellable = true)
    private void pb$skipBlockedTarget(Entity candidate, Entity viewer, CallbackInfoReturnable<Boolean> cir) {
        if (HiddenEntities.blocked(candidate, HideTarget.TOOLTIP)) {
            cir.setReturnValue(false);
        }
    }
}
