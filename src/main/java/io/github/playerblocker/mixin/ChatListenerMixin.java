package io.github.playerblocker.mixin;

import com.mojang.authlib.GameProfile;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import io.github.playerblocker.text.ChatTexts;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;
import java.util.UUID;

/**
 * Chat suppression at the display gate, never at the packet layer.
 *
 * <p>Vanilla already routes every incoming player message through
 * {@code showMessageToPlayer}, whose first act is a chain of "should I show this" guards —
 * including one for the vanilla block list. Resolving our own decisions there means the message
 * is fully validated, signature-chained and acknowledged as usual, and only its rendering is
 * skipped. Nothing in the protocol is disturbed, so there is no way for the server or the
 * message-signature chain to notice.</p>
 */
@Mixin(ChatListener.class)
public class ChatListenerMixin {

    /** Player chat. The sender UUID is authoritative, so this channel is fully deterministic. */
    @Inject(method = "showMessageToPlayer", at = @At("HEAD"), cancellable = true)
    private void pb$hidePlayerChat(ChatType.Bound bound, PlayerChatMessage message, Component content,
                                   GameProfile sender, boolean onlyShowSecure, Instant timestamp,
                                   CallbackInfoReturnable<Boolean> cir) {
        UUID author = message.sender();
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.hides(author, HideTarget.CHAT)) {
            return;
        }
        String name = sender != null ? sender.name() : manager.displayName(author);
        if (manager.dryRun()) {
            manager.recordDryRunHit("chat", name);
            return;
        }
        manager.logHidden("chat", name, content.getString());
        cir.setReturnValue(false);
    }

    /**
     * System messages: private messages, join/leave notices, advancement broadcasts, and anything
     * else a server formats with a player name. This is the heuristic channel — no UUID exists on
     * this path, so it works from the translation key when it can and from the text when it must.
     */
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void pb$hideSystemMessage(Component text, boolean overlay, CallbackInfo ci) {
        if (overlay) {
            // Action bar: transient, and often a UI prompt rather than conversation. Leave it.
            return;
        }
        String name = matchedBlockedName(text);
        if (name == null) {
            return;
        }
        BlocklistManager manager = BlocklistManager.get();
        if (manager.dryRun()) {
            manager.recordDryRunHit("system", name);
            return;
        }
        manager.logHidden("system", name, text.getString());
        ci.cancel();
    }

    /** The blocked player named by this message, or null when nobody blocked is involved. */
    private static String matchedBlockedName(Component text) {
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.masterEnabled() || !manager.hasEntries()) {
            return null;
        }
        // Tier 1: the translation key tells us exactly which argument is the sender.
        String direct = ChatTexts.senderName(text);
        if (direct != null && manager.hidesName(direct, HideTarget.SYSTEM)) {
            return direct;
        }
        // Tier 2: plain server text, whole-token match only.
        for (String name : manager.blockedNames().keySet()) {
            if (manager.hidesName(name, HideTarget.SYSTEM) && ChatTexts.mentions(text, name)) {
                return name;
            }
        }
        return null;
    }
}
