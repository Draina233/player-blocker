package io.github.playerblocker.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * Raw view of the connection's known-player map.
 *
 * <p>{@code ClientLevel#players()} only holds entities the server has actually spawned for us,
 * i.e. whoever is close enough to be tracked — a player standing across the map is online and
 * listed in the tab list, but has no client entity at all. Anything that has to enumerate
 * <em>everyone on the server</em> (the {@code /pb add} picker, its tab completion) therefore
 * cannot use the level; this map is the data the tab list itself is built from.</p>
 *
 * <p>Deliberately unfiltered: the picker must still show players that are already blocked,
 * otherwise re-adding or un-blocking the person you just blocked would be impossible.</p>
 */
@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {

    @Accessor("playerInfoMap")
    Map<UUID, PlayerInfo> pb$playerInfoMap();
}
