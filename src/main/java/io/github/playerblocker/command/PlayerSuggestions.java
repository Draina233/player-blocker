package io.github.playerblocker.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.playerblocker.config.BlockEntry;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.util.OnlinePlayers;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab completion for the {@code /pb} arguments.
 *
 * <p>Backed by {@link OnlinePlayers}, which reads the connection's player map rather than the
 * client level: the level only knows about entities the server has spawned, so anyone standing
 * far away — still very much online and still in the tab list — used to be unreachable from
 * {@code /pb add}. The map is also deliberately unfiltered, so blocked players remain
 * completable, which is what {@code /pb remove} needs.</p>
 */
public final class PlayerSuggestions {

    /** Everyone the server has told us about, blocked or not. */
    public static final SuggestionProvider<FabricClientCommandSource> ONLINE = (ctx, builder) -> {
        List<String> names = new ArrayList<>();
        for (PlayerInfo info : OnlinePlayers.all()) {
            String name = OnlinePlayers.nameOf(info);
            if (name != null) {
                names.add(name);
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    /** Everyone in the blocklist, including entries whose player is currently offline. */
    public static final SuggestionProvider<FabricClientCommandSource> BLOCKED = (ctx, builder) -> {
        List<String> names = new ArrayList<>();
        for (BlockEntry entry : BlocklistManager.get().config().entries) {
            if (entry.name != null && !entry.name.isBlank()) {
                names.add(entry.name);
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private PlayerSuggestions() {
    }
}
