package io.github.playerblocker.mixin;

import io.github.playerblocker.PlayerBlockerClient;
import io.github.playerblocker.config.BlocklistManager;
import io.github.playerblocker.config.HideTarget;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Last gate before a line enters the chat box.
 *
 * <p>{@code ChatComponent} has exactly one private funnel — {@code addMessage(Component,
 * MessageSignature, GuiMessageSource, GuiMessageTag)} — and the three public entry points
 * ({@code addPlayerMessage}, {@code addServerSystemMessage}, {@code addClientSystemMessage})
 * all delegate to it. Filtering here therefore covers every kind of line regardless of which
 * packet path delivered it, including the plain-text chat that plugin servers send as system
 * chat, where no sender UUID exists at all.</p>
 *
 * <p>{@code SYSTEM_CLIENT} is skipped on purpose: that is our own command feedback, and hiding
 * "blocked X" from ourselves would be actively confusing.</p>
 *
 * <p>This sits after signature verification, so nothing on the wire changes — the message is
 * still validated, chained and acknowledged; only the line never reaches the message list.</p>
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    private void pb$hideBlockedChat(Component content, MessageSignature signature,
                                    GuiMessageSource source, GuiMessageTag tag,
                                    CallbackInfo ci) {
        if (content == null || source == GuiMessageSource.SYSTEM_CLIENT) {
            return;
        }
        BlocklistManager manager = BlocklistManager.get();
        if (!manager.masterEnabled()) {
            return;
        }
        String text = content.getString();
        if (manager.debug()) {
            PlayerBlockerClient.LOGGER.info("[pb/debug] chat({}) {}", source, text);
        }
        // A player line answers to CHAT, a server line to SYSTEM; accept either so a server that
        // relays chat as system text is still covered when only one of the two is switched on.
        String matched = manager.matchesText(text, HideTarget.CHAT, HideTarget.SYSTEM);
        if (matched == null) {
            return;
        }
        if (manager.dryRun()) {
            manager.recordDryRunHit("chat", matched);
            return;
        }
        manager.logHidden(source == GuiMessageSource.PLAYER ? "chat" : "system", matched, text);
        ci.cancel();
    }
}
