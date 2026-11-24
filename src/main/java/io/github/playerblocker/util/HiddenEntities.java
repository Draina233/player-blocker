package io.github.playerblocker.util;

import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Shared "is this entity the blocked player" checks.
 *
 * <p>Every caller here runs on a hot path (per sound, per particle, per frame), so each one
 * starts with the cheapest possible rejection: channel bitmask first, entity type second, UUID
 * lookup last.</p>
 */
public final class HiddenEntities {

    private HiddenEntities() {
    }

    /** True when this entity is a player whose given channel is suppressed. */
    public static boolean blocked(Entity entity, HideTarget target) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.anyHas(target)) {
            return false;
        }
        return manager.hides(player.getUUID(), target);
    }

    /**
     * True when a particle is spawned inside a blocked player's hitbox.
     *
     * <p>Particles carry no owner: the server sends a position and nothing else. Position is the
     * only signal available, so this is deliberately a tight test — the particle must spawn
     * inside the (slightly inflated) box of the hidden player, which is where potion swirls,
     * totem pops and burning flames actually originate.</p>
     */
    public static boolean particleBelongsToBlockedPlayer(double x, double y, double z) {
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.anyHas(HideTarget.PARTICLES)) {
            return false;
        }
        if (!(Minecraft.getInstance().level instanceof ClientLevel level)) {
            return false;
        }
        for (AbstractClientPlayer player : level.players()) {
            if (!manager.hides(player.getUUID(), HideTarget.PARTICLES)) {
                continue;
            }
            if (player.getBoundingBox().inflate(0.6D).contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }
}
