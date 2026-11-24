package io.github.playerblocker.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes blocked players from server-provided command completion.
 *
 * <p>{@code /tpa}, {@code /msg}, {@code /pay} and plugin commands do not ask the client who is
 * online: the server ships the candidate list in a packet, and the client just renders it.
 * Trimming the client's player collections has no effect there, which is why the name kept
 * showing up in completion while the tab list was already clean.</p>
 *
 * <p>{@code ClientSuggestionProvider#completeCustomSuggestions} is the single hand-off point
 * between that packet and the completion popup, so filtering the list here covers every command
 * at once — including ones added by other mods, and without rebuilding network packets. The
 * entries carry no UUID, so this uses the boundary-checked name match.</p>
 */
@Mixin(ClientSuggestionProvider.class)
public class ClientSuggestionProviderMixin {

    @ModifyVariable(method = "completeCustomSuggestions", at = @At("HEAD"), argsOnly = true)
    private Suggestions pb$filterServerSuggestions(Suggestions suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return suggestions;
        }
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.masterEnabled()) {
            return suggestions;
        }
        List<Suggestion> source = suggestions.getList();
        List<Suggestion> kept = null;
        for (int i = 0; i < source.size(); i++) {
            Suggestion suggestion = source.get(i);
            boolean blocked = manager.matchesText(suggestion.getText(),
                    HideTarget.SUGGESTIONS, HideTarget.SUGGESTIONS) != null;
            if (blocked) {
                if (kept == null) {
                    kept = new ArrayList<>(source.subList(0, i));
                }
            } else if (kept != null) {
                kept.add(suggestion);
            }
        }
        if (kept == null) {
            return suggestions;
        }
        return new Suggestions(suggestions.getRange(), kept);
    }
}
