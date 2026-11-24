package io.github.playerblocker.mixin;

import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import io.github.playerblocker.render.HiddenRenderStates;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Decides "hidden or not" while the entity reference is still available.
 *
 * <p>Runs on the version-independent part of the pipeline: {@code extractEntity} is called for
 * every entity that is about to be submitted, so a single hook covers model, held items, armour,
 * name tag and shadow at once.</p>
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void playerblocker$markHidden(Entity entity, float partialTick,
                                        CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (state == null) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }

        BlocklistManager manager = BlocklistManager.get();
        boolean hideModel = manager.hides(player.getUUID(), HideTarget.MODEL);
        boolean hideName = manager.hides(player.getUUID(), HideTarget.NAMETAG);
        boolean hideOutline = manager.hides(player.getUUID(), HideTarget.OUTLINE);

        HiddenRenderStates.set(state, hideModel);

        if (hideModel) {
            // Belt and braces: even if another mod submits this state directly, drop the
            // outline and the ground shadow so nothing hints at a presence there.
            state.outlineColor = EntityRenderState.NO_OUTLINE;
            state.shadowRadius = 0.0F;
            state.shadowPieces.clear();
        } else {
            if (hideName) {
                state.nameTag = null;
                state.scoreText = null;
            }
            if (hideOutline) {
                state.outlineColor = EntityRenderState.NO_OUTLINE;
            }
        }
    }
}
