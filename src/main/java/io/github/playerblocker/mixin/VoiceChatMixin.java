package io.github.playerblocker.mixin;

import de.maxhenkel.voicechat.voice.common.SoundPacket;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mutes the blocked player's proximity voice audio in Simple Voice Chat.
 *
 * <p>Every incoming voice frame on the client funnels through {@code processSoundPacket}, and
 * each frame carries the speaker's UUID. Dropping the frame before it reaches an audio channel
 * silences that one player without touching anyone else's audio — the same effect as muting him
 * by hand, minus the per-session chore.</p>
 *
 * <p>Compiled against the installed voice chat jar; only applied when the mod is present.</p>
 */
@Mixin(targets = "de.maxhenkel.voicechat.voice.client.ClientVoicechat")
public class VoiceChatMixin {

    @Inject(method = "processSoundPacket", at = @At("HEAD"), cancellable = true)
    private void pb$muteBlockedPlayer(SoundPacket<?> packet, CallbackInfo ci) {
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.anyHas(HideTarget.VOICE)) {
            return;
        }
        UUID sender = packet.getSender();
        if (sender != null && manager.hides(sender, HideTarget.VOICE)) {
            ci.cancel();
        }
    }
}
