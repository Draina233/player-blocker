package io.github.playerblocker.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.playerblocker.render.HiddenRenderStates;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Single choke point for "do not draw this entity at all".
 *
 * <p>Deliberately <b>not</b> injected into {@code EntityRenderer.submit}: {@code LivingEntityRenderer}
 * overrides that method and the player renderer inherits from it, so a parent-class injection
 * would silently never run for players. {@code EntityRenderDispatcher.submit} is the one place
 * {@code LevelRenderer} calls for every entity regardless of renderer subclass, and it is where
 * the model, name tag, flame and ground shadow are all emitted.</p>
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherSubmitMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void playerblocker$skipHidden(EntityRenderState state, CameraRenderState cameraState,
                                        double x, double y, double z, PoseStack poseStack,
                                        SubmitNodeCollector collector, CallbackInfo ci) {
        if (HiddenRenderStates.isHidden(state)) {
            ci.cancel();
        }
    }
}
