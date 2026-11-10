package dev.transparentinventory.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * One-step safety net for the config file: each save copies the previous
 * version to a sibling {@code .bak} before the new one replaces it.
 *
 * <p>Only the most recent previous version is kept, so the disk cost stays at
 * one extra file. Failures never block the save itself.
 */
public final class ConfigBackups {

    public static final String SUFFIX = ".bak";

    private ConfigBackups() {
    }

    /** The sibling path that holds the previous version. */
    public static Path backupPath(Path config) {
        return config == null ? null : config.resolveSibling(config.getFileName() + SUFFIX);
    }

    /**
     * Copies the current config to its backup path.
     *
     * @return true when a previous version was copied; false when there is no
     *         file yet or the copy failed
     */
    public static boolean copy(Path config) {
        Path target = backupPath(config);
        if (config == null || target == null || !Files.isRegularFile(config)) return false;
        try {
            Files.copy(config, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    /** True when a backup file exists and can be read. */
    public static boolean exists(Path config) {
        Path target = backupPath(config);
        return target != null && Files.isRegularFile(target) && Files.isReadable(target);
    }
}
