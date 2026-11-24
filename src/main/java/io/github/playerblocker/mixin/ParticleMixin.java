package io.github.playerblocker.mixin;

import io.github.playerblocker.util.HiddenEntities;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops particles spawned inside a hidden player's hitbox: potion swirls, totem pops, flames.
 *
 * <p>Particles carry no owner, so position is the only signal. The check is a tight hitbox test
 * and short-circuits on a channel bitmask, which matters because this method runs hundreds of
 * times per tick for weather and ambient effects.</p>
 */
@Mixin(ClientLevel.class)
public class ParticleMixin {

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void pb$hideEntityParticles(ParticleOptions particle, double x, double y, double z,
                                        double dx, double dy, double dz, CallbackInfo ci) {
        if (HiddenEntities.particleBelongsToBlockedPlayer(x, y, z)) {
            ci.cancel();
        }
    }
}
