package io.github.playerblocker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * On-disk blocklist. Written atomically with a rolling backup so a crash can never leave an
 * unparseable config behind.
 */
public final class BlocklistConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("playerblocker/config");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** Entry count at the last successful write, used to shout when a list is about to be wiped. */
    private static int lastPersistedCount = -1;
    /** Set while writing the result of an explicit removal, so emptying the list is not flagged. */
    private static boolean removingOnPurpose;

    public int version = 1;
    public Settings settings = new Settings();
    public List<BlockEntry> entries = new ArrayList<>();

    /** Global switches, independent of any single entry. */
    public static final class Settings {
        /** Master switch; useful to bind to a hotkey for "reveal everything". */
        public boolean enabled = true;
        /** Record hits without hiding anything. Use before trusting a new entry. */
        public boolean dryRun = false;
        /** Mirror hidden chat into logs/playerblocker-hidden.log so nothing is silently lost. */
        public boolean logHiddenChat = true;
        /** Log every chat line and suggestion the filters inspect. Toggle with /pb debug. */
        public boolean debug = false;
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("playerblocker").resolve("blocklist.json");
    }

    public static Path hiddenLogPath() {
        return FabricLoader.getInstance().getGameDir().resolve("logs").resolve("playerblocker-hidden.log");
    }

    public static BlocklistConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            BlocklistConfig fresh = new BlocklistConfig();
            fresh.save();
            return fresh;
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            BlocklistConfig cfg = GSON.fromJson(r, BlocklistConfig.class);
            if (cfg == null) {
                throw new IOException("empty config");
            }
            cfg.normalise();
            LOGGER.info("[playerblocker] loaded {} entr{} from {}",
                    cfg.entries.size(), cfg.entries.size() == 1 ? "y" : "ies", path);
            return cfg;
        } catch (Exception e) {
            LOGGER.error("[playerblocker] config unreadable, starting empty (file kept as .broken)", e);
            try {
                Files.copy(path, path.resolveSibling("blocklist.json.broken"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // best effort only
            }
            return new BlocklistConfig();
        }
    }

    /** Fills in defaults that a hand-edited or older file may be missing. */
    public void normalise() {
        if (settings == null) {
            settings = new Settings();
        }
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entries.removeIf(e -> e == null || e.uuid == null || e.uuid.isBlank());
        for (BlockEntry e : entries) {
            if (e.targets == null) {
                e.targets = new java.util.LinkedHashMap<>();
            }
            for (HideTarget t : HideTarget.values()) {
                e.targets.putIfAbsent(t.name(), t.defaultEnabled());
            }
            if (e.scope == null) {
                e.scope = "global";
            }
        }
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            // A shrinking-to-zero write is the one mistake that loses data silently, so say it
            // out loud instead of letting it disappear into a well-formed empty file.
            if (entries.isEmpty() && lastPersistedCount > 0 && !removingOnPurpose) {
                LOGGER.warn("[playerblocker] writing an EMPTY blocklist over {} entr{} (was {} on disk)",
                        lastPersistedCount, lastPersistedCount == 1 ? "y" : "ies", path);
            }
            if (Files.exists(path)) {
                Path backup = path.resolveSibling("blocklist.json.bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            Path tmp = path.resolveSibling("blocklist.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            lastPersistedCount = entries.size();
            LOGGER.info("[playerblocker] saved {} entr{}", entries.size(), entries.size() == 1 ? "y" : "ies");
        } catch (IOException e) {
            LOGGER.error("[playerblocker] failed to save config", e);
        }
    }

    public Optional<BlockEntry> find(String uuid) {
        String key = uuid.toLowerCase(Locale.ROOT);
        return entries.stream().filter(e -> e.uuid.toLowerCase(Locale.ROOT).equals(key)).findFirst();
    }

    public Optional<BlockEntry> findByName(String name) {
        return entries.stream().filter(e -> e.name != null && e.name.equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Adds or replaces the entry for a player. Keyed on UUID alone: one player has one entry,
     * and its scope is a property of that entry rather than a second dimension.
     */
    public BlockEntry addOrReplace(BlockEntry entry) {
        entries.removeIf(e -> e.uuid.equalsIgnoreCase(entry.uuid));
        entries.add(entry);
        save();
        return entry;
    }

    /** Re-points the scope of an existing entry. Returns false when the player is not listed. */
    public boolean setScope(String uuid, String scope) {
        Optional<BlockEntry> entry = find(uuid);
        if (entry.isEmpty()) {
            return false;
        }
        entry.get().scope = scope;
        save();
        return true;
    }

    public boolean remove(String uuid) {
        boolean removed = entries.removeIf(e -> e.uuid.equalsIgnoreCase(uuid));
        if (removed) {
            // Emptying the list through an explicit removal is the intended outcome, not the
            // accident the save() guard is looking for.
            removingOnPurpose = true;
            try {
                save();
            } finally {
                removingOnPurpose = false;
            }
        }
        return removed;
    }

    /** Appends a line to the local "what was hidden" log. Never uploaded anywhere. */
    public void logHidden(String kind, String name, String content) {
        if (!settings.logHiddenChat) {
            return;
        }
        Path p = hiddenLogPath();
        try {
            Files.createDirectories(p.getParent());
            String line = "[" + Instant.now() + "] [" + kind + "] <" + name + "> " + content + System.lineSeparator();
            Files.writeString(p, line, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.warn("[playerblocker] could not append hidden log", e);
        }
    }
}
