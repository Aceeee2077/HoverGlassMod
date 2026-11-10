package dev.transparentinventory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

/**
 * Loads and persists the client config from {@code config/transparent_inventory.json}.
 *
 * Parse failures use defaults in memory and preserve the original file for recovery.
 */
public final class ConfigLoader {

    public static final String FILE_NAME = "transparent_inventory.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean loaded;

    private ConfigLoader() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        InventoryConfig config = null;
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, InventoryConfig.class);
            } catch (IOException | RuntimeException ignored) {
                // Includes JsonSyntaxException from malformed JSON files.
                config = null;
            }
        }
        if (config == null) {
            config = new InventoryConfig();
            if (!Files.exists(path)) writeDefault(path, config);
            else quarantine(path);
        }
        config.sanitize();
        InventoryConfig.setInstance(config);
    }

    /**
     * Serializes a config exactly as it would be written, so callers can compare
     * two states without touching the disk.
     *
     * @return the JSON text, or an empty string for {@code null}
     */
    public static String toJson(InventoryConfig config) {
        return config == null ? "" : GSON.toJson(config);
    }

    public static synchronized boolean save() {
        InventoryConfig.get().sanitize();
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        // Keep the previous file so a bad edit or paste can be rolled back.
        ConfigBackups.copy(path);
        return writeDefault(path, InventoryConfig.get());
    }

    /**
     * Reads the previous version written by the last save.
     *
     * @return a sanitized config, or {@code null} when there is no backup or it
     *         cannot be parsed
     */
    public static synchronized InventoryConfig loadBackup() {
        Path backup = ConfigBackups.backupPath(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
        if (backup == null || !Files.isRegularFile(backup)) return null;
        try (Reader reader = Files.newBufferedReader(backup, StandardCharsets.UTF_8)) {
            InventoryConfig config = GSON.fromJson(reader, InventoryConfig.class);
            if (config == null) return null;
            config.sanitize();
            return config;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    /** When the previous version was written, or {@code -1} when there is none. */
    public static synchronized long backupMillis() {
        return ConfigBackups.backupMillis(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
    }

    /** Number of rotating snapshots available to restore; zero when there are none. */
    public static synchronized int snapshotCount() {
        return ConfigBackups.snapshotCount(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
    }

    /**
     * Reads one rotating snapshot, which covers the case where several bad saves
     * have already pushed the plain {@code .bak} out of reach.
     *
     * @param index 0 is the newest snapshot; older ones follow
     * @return a sanitized config, or {@code null} when that snapshot does not exist
     *         or cannot be parsed
     */
    public static synchronized InventoryConfig loadSnapshot(int index) {
        var snapshots = ConfigBackups.snapshots(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
        if (index < 0 || index >= snapshots.size()) return null;
        try (Reader reader = Files.newBufferedReader(snapshots.get(index), StandardCharsets.UTF_8)) {
            InventoryConfig config = GSON.fromJson(reader, InventoryConfig.class);
            if (config == null) return null;
            config.sanitize();
            return config;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    /** When the snapshot at {@code index} was taken, or {@code -1} when there is none. */
    public static synchronized long snapshotMillis(int index) {
        var snapshots = ConfigBackups.snapshots(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
        return index >= 0 && index < snapshots.size() ? ConfigBackups.epochMillis(snapshots.get(index)) : -1;
    }

    /**
     * Moves an unreadable config aside so defaults can be written without destroying
     * the user's file. If the move fails the original is left untouched and only
     * defaults are used for this session.
     */
    private static void quarantine(Path path) {
        Path backup = path.resolveSibling(ConfigFileRecovery.backupName(
                String.valueOf(path.getFileName()), System.currentTimeMillis()));
        try {
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            ModClient.LOGGER.warn("Could not parse {}. Using defaults; the original was moved to {}.", path, backup);
        } catch (IOException | RuntimeException ignored) {
            ModClient.LOGGER.warn("Could not parse {}. Using defaults for this session; it will be replaced on save.", path);
        }
    }

    private static boolean writeDefault(Path path, InventoryConfig config) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ignored) {
            ModClient.LOGGER.warn("Could not save {}. Settings are active for this session.", path);
            return false;
        }
    }
}
