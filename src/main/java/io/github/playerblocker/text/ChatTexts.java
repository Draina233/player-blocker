package io.github.playerblocker.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Locale;
import java.util.Set;

/**
 * Text helpers for the one channel that has no UUID to work with.
 *
 * <p>Two tiers, safest first:</p>
 * <ol>
 *   <li><b>Translation key whitelist.</b> Vanilla builds whispers, join/leave notices and
 *       advancement broadcasts from a known key with the sender as {@code args[0]}. When the key
 *       matches, the name comes straight out of the argument — no guessing.</li>
 *   <li><b>Whole-token match.</b> Servers that send plain strings (very common on Chinese
 *       servers) get a boundary-checked name match instead. {@code Alex} will not match
 *       {@code Alex2} or {@code Alexander}.</li>
 * </ol>
 */
public final class ChatTexts {

    /**
     * Keys whose first argument is the acting player. Deliberately short: every entry is a
     * promise that arg[0] is a name, which is what keeps tier 1 free of false positives.
     */
    private static final Set<String> SENDER_FIRST_KEYS = Set.of(
            "commands.message.display.incoming",
            "multiplayer.player.joined",
            "multiplayer.player.joined.renamed",
            "multiplayer.player.left",
            "chat.type.emote",
            "chat.type.advancement.task",
            "chat.type.advancement.challenge",
            "chat.type.advancement.goal"
    );

    private ChatTexts() {
    }

    /** Sender name taken from a known translation key, or null when the key is not whitelisted. */
    public static String senderName(Component text) {
        if (text == null) {
            return null;
        }
        if (text.getContents() instanceof TranslatableContents contents
                && SENDER_FIRST_KEYS.contains(contents.getKey())) {
            Object[] args = contents.getArgs();
            if (args.length > 0 && args[0] != null) {
                if (args[0] instanceof Component component) {
                    return component.getString();
                }
                return String.valueOf(args[0]);
            }
        }
        return null;
    }

    /** True when the rendered text contains the name as a whole token, case-insensitively. */
    public static boolean mentions(Component text, String name) {
        if (text == null || name == null || name.isEmpty()) {
            return false;
        }
        return containsToken(text.getString(), name);
    }

    /** True when {@code haystack} contains {@code needle} as a whole token, case-insensitively. */
    public static boolean containsToken(String haystack, String needle) {
        if (haystack.isEmpty() || needle.isEmpty()) {
            return false;
        }
        String hay = haystack.toLowerCase(Locale.ROOT);
        String pin = needle.toLowerCase(Locale.ROOT);
        int from = 0;
        while (from <= hay.length() - pin.length()) {
            int idx = hay.indexOf(pin, from);
            if (idx < 0) {
                return false;
            }
            int end = idx + pin.length();
            boolean leftOk = idx == 0 || !isWordChar(hay.charAt(idx - 1));
            boolean rightOk = end == hay.length() || !isWordChar(hay.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            from = idx + 1;
        }
        return false;
    }

    /** Word characters for boundary purposes: ASCII letters, digits and underscore. */
    private static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }
}
