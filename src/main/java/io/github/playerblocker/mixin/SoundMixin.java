package io.github.playerblocker.mixin;

import io.github.playerblocker.config.HideTarget;
import io.github.playerblocker.util.HiddenEntities;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops entity-bound sounds: footsteps, hurt, attack, item pickup, eating.
 *
 * <p>Hooked at the client level rather than at the packet, so it also catches sounds the client
 * starts on its own (sprinting, landing) — those never come from a packet and would otherwise
 * leak his presence just as loudly.</p>
 */
@Mixin(ClientLevel.class)
public class SoundMixin {

    @Inject(
            method = "playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void pb$muteEntitySound(Entity except, Entity source,
                                    net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound,
                                    net.minecraft.sounds.SoundSource category,
                                    float volume, float pitch, long seed, CallbackInfo ci) {
        // The two entity arguments are (listener exception, emitter); check both so the mixin
        // keeps working whichever slot the emitter occupies.
        if (HiddenEntities.blocked(source, HideTarget.SOUND)
                || HiddenEntities.blocked(except, HideTarget.SOUND)) {
            ci.cancel();
        }
    }

    /** Second route: entity sounds that are played locally rather than seeded from the server. */
    @Inject(
            method = "playLocalSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void pb$muteLocalEntitySound(Entity entity, net.minecraft.sounds.SoundEvent sound,
                                         net.minecraft.sounds.SoundSource category,
                                         float volume, float pitch, CallbackInfo ci) {
        if (HiddenEntities.blocked(entity, HideTarget.SOUND)) {
            ci.cancel();
        }
    }
}
