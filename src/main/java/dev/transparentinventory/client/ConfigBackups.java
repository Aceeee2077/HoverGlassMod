package dev.transparentinventory.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Safety nets for the config file: each save copies the previous version to a
 * sibling {@code .bak} before the new one replaces it, and keeps a small
 * rotating history in {@code transparent_inventory-backups/} so several bad
 * saves in a row can still be walked back.
 *
 * <p>Only the five newest snapshots are kept, so the disk cost stays bounded.
 * Failures never block the save itself.
 */
public final class ConfigBackups {

    public static final String SUFFIX = ".bak";
    /** Folder beside the config that holds the rotating snapshots. */
    public static final String DIRECTORY = "transparent_inventory-backups";
    /** Number of rotating snapshots kept; the oldest are pruned first. */
    public static final int KEEP = 5;
    public static final String ROTATION_PREFIX = "transparent_inventory-backup-";

    private ConfigBackups() {
    }

    /** The sibling path that holds the previous version. */
    public static Path backupPath(Path config) {
        return config == null ? null : config.resolveSibling(config.getFileName() + SUFFIX);
    }

    /** The folder that holds the rotating snapshots of this config file. */
    public static Path rotationDirectory(Path config) {
        if (config == null) return null;
        Path parent = config.toAbsolutePath().getParent();
        return parent == null ? null : parent.resolve(DIRECTORY);
    }

    /**
     * Copies the current config to its backup path and records a rotating snapshot.
     *
     * @return true when a previous version was copied; false when there is no
     *         file yet or the copy failed
     */
    public static boolean copy(Path config) {
        Path target = backupPath(config);
        if (config == null || target == null || !Files.isRegularFile(config)) return false;
        try {
            Files.copy(config, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
        rotate(config);
        return true;
    }

    /** Records one snapshot using the current clock; see {@link #rotate(Path, long)}. */
    public static int rotate(Path config) {
        return rotate(config, System.currentTimeMillis());
    }

    /**
     * Copies the current config into the rotation folder under a sortable
     * timestamp name and prunes everything older than the newest {@link #KEEP}.
     *
     * @param epochMillis snapshot time; also makes the test deterministic
     * @return the number of snapshots kept, or -1 when nothing could be written
     */
    public static int rotate(Path config, long epochMillis) {
        if (config == null || !Files.isRegularFile(config)) return -1;
        Path directory = rotationDirectory(config);
        if (directory == null) return -1;
        try {
            Files.createDirectories(directory);
            // Nineteen zero-padded digits keep lexicographic order chronological.
            String name = ROTATION_PREFIX + String.format(Locale.ROOT, "%019d", epochMillis) + ".json";
            Files.copy(config, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return prune(directory);
        } catch (IOException | RuntimeException ignored) {
            return -1;
        }
    }

    /** Deletes every snapshot beyond the newest {@link #KEEP}; returns how many remain. */
    private static int prune(Path directory) {
        try (var stream = Files.list(directory)) {
            List<Path> snapshots = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(ROTATION_PREFIX))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            int deleted = 0;
            for (int index = 0; index < snapshots.size() - KEEP; index++) {
                try {
                    if (Files.deleteIfExists(snapshots.get(index))) deleted++;
                } catch (IOException ignored) {
                    // Keep counting the remaining files; a locked old file is harmless.
                }
            }
            return snapshots.size() - deleted;
        } catch (IOException | RuntimeException ignored) {
            return -1;
        }
    }

    /** True when a backup file exists and can be read. */
    public static boolean exists(Path config) {
        Path target = backupPath(config);
        return target != null && Files.isRegularFile(target) && Files.isReadable(target);
    }
}
