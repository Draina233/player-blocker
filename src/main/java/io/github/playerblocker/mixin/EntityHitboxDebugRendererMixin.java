package io.github.playerblocker.mixin;

import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the F3+B hitbox outline of a blocked player.
 *
 * <p>Everything else this mod hides is presentation: body, name, shadow, particles. The debug
 * hitbox is not presentation — it is a wireframe drawn straight from the bounding box, so it
 * survived the render cancellation and kept walking around the world as a floating white box,
 * which defeats the point.</p>
 *
 * <p>{@code EntityHitboxDebugRenderer#emitGizmos} iterates the visible entities and calls
 * {@code showHitboxes(Entity, float, boolean)} once per entity, so cancelling that one call
 * removes exactly one entity's outline and leaves every other hitbox untouched. The renderer is
 * only registered while the overlay is switched on, which means the guard below costs nothing in
 * normal play.</p>
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
    private void pb$hideBlockedHitbox(Entity entity, float partialTick, boolean serverEntity,
                                      CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.hasEntries() || !manager.masterEnabled()) {
            return;
        }
        // MODEL is accepted too: hiding someone's body but leaving their hitbox on screen would
        // make the switch pointless for anyone who set this up before the channel existed.
        if (manager.hidesAny(player.getUUID(), HideTarget.HITBOX, HideTarget.MODEL)) {
            ci.cancel();
        }
    }
}
