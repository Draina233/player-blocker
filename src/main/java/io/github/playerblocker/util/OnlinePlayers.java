package io.github.playerblocker.util;

import io.github.playerblocker.mixin.ClientPacketListenerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The one honest answer to "who is on this server right now".
 *
 * <p>Three collections look like they answer that question and two of them lie:</p>
 * <ul>
 *   <li>{@code ClientLevel#players()} — only tracked entities, i.e. players near enough to be
 *       rendered. Standing on opposite corners of the map is enough to lose someone.</li>
 *   <li>{@code getOnlinePlayers()} / {@code getListedOnlinePlayers()} — complete, but filtered by
 *       this mod, so blocked players are missing exactly when the UI needs them.</li>
 *   <li>{@code playerInfoMap} — complete and untouched. This is what we read.</li>
 * </ul>
 */
public final class OnlinePlayers {

    private OnlinePlayers() {
    }

    /** Every player the server has told us about, blocked or not, sorted by name. */
    public static List<PlayerInfo> all() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        Map<UUID, PlayerInfo> map = ((ClientPacketListenerAccessor) connection).pb$playerInfoMap();
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        List<PlayerInfo> out = new ArrayList<>(map.size());
        for (PlayerInfo info : map.values()) {
            if (info != null && nameOf(info) != null) {
                out.add(info);
            }
        }
        out.sort(Comparator.comparing(OnlinePlayers::nameOf, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** Profile name, or null when the profile is somehow incomplete. */
    public static String nameOf(PlayerInfo info) {
        if (info == null || info.getProfile() == null) {
            return null;
        }
        String name = info.getProfile().name();
        return name == null || name.isBlank() ? null : name;
    }
}
