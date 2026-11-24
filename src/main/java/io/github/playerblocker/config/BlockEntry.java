package io.github.playerblocker.config;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single blocked player.
 *
 * <p>UUID is the primary key: names change, and on offline-mode servers the UUID is derived
 * from the name anyway. The name is kept only as a snapshot for the UI and for servers that
 * hand out unstable UUIDs.</p>
 */
public final class BlockEntry {

    /** Applies to every server and every world. This is the default. */
    public static final String SCOPE_GLOBAL = "global";

    /** Canonical UUID string, lowercase with dashes. */
    public String uuid;
    /** Name at the moment the entry was created; display only. */
    public String name;
    /**
     * {@code "global"}, or {@code "server:host:port"}.
     *
     * <p>Global is the default: once someone is blocked they stay blocked wherever you meet
     * them. Per-server scoping is opt-in, for the case where two servers happen to use the
     * same name for different people.</p>
     */
    public String scope = SCOPE_GLOBAL;
    /** Free-form note, shown in the GUI. */
    public String note = "";
    /** Epoch millis. */
    public long createdAt;
    /** Enabled channels; stored as a name list so removed enum values degrade gracefully. */
    public Map<String, Boolean> targets = new LinkedHashMap<>();

    public BlockEntry() {
    }

    public BlockEntry(String uuid, String name, String scope) {
        this.uuid = uuid;
        this.name = name;
        this.scope = scope;
        this.createdAt = System.currentTimeMillis();
        for (HideTarget t : HideTarget.values()) {
            targets.put(t.name(), t.defaultEnabled());
        }
    }

    public boolean enabled(HideTarget target) {
        Boolean v = targets.get(target.name());
        return v != null ? v : target.defaultEnabled();
    }

    public void set(HideTarget target, boolean value) {
        targets.put(target.name(), value);
    }

    public EnumSet<HideTarget> enabledTargets() {
        EnumSet<HideTarget> set = EnumSet.noneOf(HideTarget.class);
        for (HideTarget t : HideTarget.values()) {
            if (enabled(t)) {
                set.add(t);
            }
        }
        return set;
    }

    /** True when this entry applies to the given scope string. */
    public boolean matchesScope(String currentScope) {
        return scope == null || SCOPE_GLOBAL.equals(scope) || scope.equals(currentScope);
    }

    /** True when this entry is limited to one server rather than applied everywhere. */
    public boolean isServerScoped() {
        return scope != null && !SCOPE_GLOBAL.equals(scope);
    }

    public BlockEntry copy() {
        BlockEntry e = new BlockEntry();
        e.uuid = uuid;
        e.name = name;
        e.scope = scope;
        e.note = note;
        e.createdAt = createdAt;
        e.targets = new LinkedHashMap<>(targets);
        return e;
    }
}
