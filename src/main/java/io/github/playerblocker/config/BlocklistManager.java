package io.github.playerblocker.config;

import io.github.playerblocker.text.ChatTexts;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime index over {@link BlocklistConfig}.
 *
 * <p>Every hot path in this mod funnels through {@link #hides(UUID, HideTarget)}, so it must be
 * allocation-free and O(1): a single HashMap lookup plus an EnumSet test. No string work, no
 * streams, no Optional on the render thread.</p>
 *
 * <p>Mutation rule: commands edit the in-memory config and then call {@link #rebuildInPlace()}.
 * They must never re-read the file to pick up their own change — that turned a transient read
 * problem into permanent data loss once already.</p>
 */
public final class BlocklistManager {

    private static volatile BlocklistManager instance;

    private BlocklistConfig config;
    /** uuid -> enabled channels. Absent key means "not blocked". */
    private Map<UUID, EnumSet<HideTarget>> index = Map.of();
    /** Lower-cased name -> enabled channels, for text channels where only a name is available. */
    private Map<String, EnumSet<HideTarget>> byName = Map.of();
    /** Union of every enabled channel, as a bitmask. Lets hot paths skip work with one test. */
    private int channelMask;
    /** Scope of the server currently joined, e.g. "server:mc.incrafttime.top". */
    private String currentScope = BlockEntry.SCOPE_GLOBAL;
    /** Temporary full reveal, bound to a hotkey. */
    private boolean revealOverride;
    private boolean dryRunHits;

    private BlocklistManager() {
    }

    public static BlocklistManager get() {
        BlocklistManager m = instance;
        if (m == null) {
            synchronized (BlocklistManager.class) {
                m = instance;
                if (m == null) {
                    m = new BlocklistManager();
                    m.config = BlocklistConfig.load();
                    m.rebuild();
                    instance = m;
                }
            }
        }
        return m;
    }

    public BlocklistConfig config() {
        return config;
    }

    /** Re-read from disk. Only for an explicit user-requested reload. */
    public void reload() {
        config = BlocklistConfig.load();
        rebuild();
    }

    /** Rebuild the lookup tables from the in-memory config, without touching the disk. */
    public void rebuildInPlace() {
        rebuild();
    }

    public void setScope(String scope) {
        String next = scope == null ? BlockEntry.SCOPE_GLOBAL : scope;
        if (!next.equals(currentScope)) {
            currentScope = next;
            rebuild();
        }
    }

    public String scope() {
        return currentScope;
    }

    /** Called when the "reveal everything" key is held. */
    public void setRevealOverride(boolean value) {
        revealOverride = value;
    }

    public boolean revealOverride() {
        return revealOverride;
    }

    public boolean dryRun() {
        return config.settings.dryRun;
    }

    public boolean masterEnabled() {
        return config.settings.enabled && !revealOverride;
    }

    /** Fast path for hot callers: skip all work when nothing is blocked at all. */
    public boolean hasEntries() {
        return !index.isEmpty();
    }

    /**
     * True when at least one blocked player has this channel enabled. Cheaper than looking up a
     * UUID, which is what per-particle and per-sound call sites need.
     */
    public boolean anyHas(HideTarget target) {
        return (channelMask & (1 << target.ordinal())) != 0;
    }

    private void rebuild() {
        Map<UUID, EnumSet<HideTarget>> next = new HashMap<>();
        Map<String, EnumSet<HideTarget>> names = new HashMap<>();
        int mask = 0;
        if (config != null) {
            for (BlockEntry e : config.entries) {
                if (!e.matchesScope(currentScope)) {
                    continue;
                }
                EnumSet<HideTarget> targets = e.enabledTargets();
                UUID id = parseUuid(e.uuid);
                if (id != null) {
                    next.put(id, targets);
                    for (HideTarget t : targets) {
                        mask |= 1 << t.ordinal();
                    }
                }
                if (e.name != null && !e.name.isBlank()) {
                    names.put(e.name.toLowerCase(Locale.ROOT), targets);
                }
            }
        }
        index = next;
        byName = names;
        channelMask = mask;
    }

    /** Parses a canonical UUID string; returns null when the value is not a UUID. */
    public static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    /** True when this player must be hidden for the given channel. */
    public boolean hides(UUID player, HideTarget target) {
        if (!masterEnabled()) {
            return false;
        }
        EnumSet<HideTarget> set = index.get(player);
        return set != null && set.contains(target);
    }

    /** True when this player must be hidden for any of the given channels. */
    public boolean hidesAny(UUID player, HideTarget a, HideTarget b) {
        if (!masterEnabled()) {
            return false;
        }
        EnumSet<HideTarget> set = index.get(player);
        return set != null && (set.contains(a) || set.contains(b));
    }

    public boolean hidesAny(UUID player) {
        return masterEnabled() && index.containsKey(player);
    }

    /** Text-channel fallback: matches a bare name when no UUID is available. */
    public boolean hidesName(String name, HideTarget target) {
        if (name == null || name.isEmpty() || !masterEnabled()) {
            return false;
        }
        EnumSet<HideTarget> set = byName.get(name.toLowerCase(Locale.ROOT));
        return set != null && set.contains(target);
    }

    /** Snapshot of blocked names, lower-cased. Used by the text heuristic. */
    public Map<String, EnumSet<HideTarget>> blockedNames() {
        return byName;
    }

    /**
     * Returns the blocked player whose name appears in {@code text} as a whole token, or null.
     *
     * <p>This is the fallback for channels that carry no UUID: chat relayed as plain server text,
     * whispers, join/leave notices, and server-provided command suggestions. Matching is
     * boundary-checked, so {@code Alex} never matches {@code Alex2} or {@code Alexander}, and it
     * is case-insensitive because servers freely change name casing.</p>
     *
     * <p>Two channels are accepted so that either switch is enough: a line counts as hidden when
     * the entry has <em>either</em> channel enabled.</p>
     */
    public String matchesText(String text, HideTarget first, HideTarget second) {
        if (text == null || text.isEmpty() || config == null || !masterEnabled()) {
            return null;
        }
        for (BlockEntry entry : config.entries) {
            if (entry.name == null || entry.name.isBlank() || !entry.matchesScope(currentScope)) {
                continue;
            }
            if (!entry.enabled(first) && !entry.enabled(second)) {
                continue;
            }
            if (ChatTexts.containsToken(text, entry.name)) {
                return entry.name;
            }
        }
        return null;
    }

    /** Verbose logging of what the chat/suggestion filters see. Off by default. */
    public boolean debug() {
        return config.settings.debug;
    }

    /** Display name for a blocked player, or the raw UUID when the snapshot name is gone. */
    public String displayName(UUID id) {
        for (BlockEntry e : config.entries) {
            UUID parsed = parseUuid(e.uuid);
            if (parsed != null && parsed.equals(id) && e.name != null) {
                return e.name;
            }
        }
        return String.valueOf(id);
    }

    /** Writes a suppressed line into the local hidden log, if the user kept that enabled. */
    public void logHidden(String kind, String player, String content) {
        if (config.settings.logHiddenChat) {
            config.logHidden(kind, player, content);
        }
    }

    /** Records a would-be hide for dry-run mode. */
    public void recordDryRunHit(String kind, String player) {
        if (config.settings.dryRun) {
            dryRunHits = true;
        }
    }

    public boolean hadDryRunHits() {
        return dryRunHits;
    }

    public void clearDryRunHits() {
        dryRunHits = false;
    }

    public List<BlockEntry> entriesForScope() {
        List<BlockEntry> out = new ArrayList<>();
        for (BlockEntry e : config.entries) {
            if (e.matchesScope(currentScope)) {
                out.add(e);
            }
        }
        return out;
    }
}
