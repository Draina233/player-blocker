package io.github.playerblocker.config;

/**
 * One independent interception channel.
 *
 * <p>Channels are deliberately split so a user can suppress someone visually while still
 * receiving their chat, or vice versa. Defaults are chosen by risk: anything that is purely
 * heuristic ships disabled, anything anchored on a UUID or a known translation key ships on.</p>
 */
public enum HideTarget {
    /** Player model, held items, armour, cape. */
    MODEL(true),
    /** Floating name tag above the head. */
    NAMETAG(true),
    /** Tab / player list entry (also covers BetterTab, which rebuilds the list from the same source). */
    TAB(true),
    /** Chat lines authored by the player, including signed player chat. */
    CHAT(true),
    /** Entity-bound sounds: footsteps, hurt, attack, item pickup. */
    SOUND(true),
    /** Particles spawned by the entity (potion swirl, totem). */
    PARTICLES(false),
    /** Command suggestions, so typing their name does not surface them. */
    SUGGESTIONS(true),
    /** Proximity voice chat audio (Simple Voice Chat, soft dependency). */
    VOICE(true),
    /** Minimap / world map entity radar (Xaero, soft dependency). */
    MINIMAP(true),
    /** Crosshair entity tooltip (Jade, soft dependency). Raycasts ignore rendering. */
    TOOLTIP(true),
    /** Glowing outline and entity shadow. */
    OUTLINE(true),
    /**
     * System messages that name the player: private messages, join/leave notices, advancement
     * broadcasts. Anchored on a whitelist of translation keys first; servers that send plain
     * text fall back to whole-word name matching, which is why every hit is written to the
     * hidden log.
     */
    SYSTEM(true),
    /** Sidebar scoreboard / /list output. Text heuristic, highest false-positive risk. */
    SIDEBAR(false),
    /**
     * The F3+B entity hitbox outline.
     *
     * <p>Purely a debug overlay, but it is the one thing that survives every other channel: the
     * body, the name and the shadow can all be gone while a white box still walks around and
     * gives the position away. Cancelling it costs nothing when the overlay is off.</p>
     *
     * <p>Answers to {@code MODEL} as well, so an existing entry keeps working without anyone
     * having to know this switch exists.</p>
     */
    HITBOX(true);

    private final boolean defaultEnabled;

    HideTarget(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }
}
