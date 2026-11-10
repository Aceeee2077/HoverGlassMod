package dev.transparentinventory.client;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Names for quarantined config files.
 *
 * <p>A config that cannot be parsed is moved aside instead of being silently
 * overwritten by the next save, so hand edits can still be recovered. Plain Java
 * only, so the naming rules are covered by the regression suite.
 */
public final class ConfigFileRecovery {

    /** Windows forbids ':' in file names, so the ISO instant is flattened. */
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private ConfigFileRecovery() {
    }

    /**
     * Backup name for {@code fileName} at the given epoch time, for example
     * {@code transparent_inventory.json.corrupt-19700101-000000}.
     */
    public static String backupName(String fileName, long epochMillis) {
        String base = sanitize(fileName);
        return base + ".corrupt-" + STAMP.format(Instant.ofEpochMilli(epochMillis));
    }

    /** Strips any directory component and control characters from a file name. */
    private static String sanitize(String fileName) {
        if (fileName == null) return "config";
        int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String name = separator >= 0 ? fileName.substring(separator + 1) : fileName;
        StringBuilder clean = new StringBuilder(name.length());
        for (int i = 0; i < name.length() && clean.length() < 128; i++) {
            char c = name.charAt(i);
            if (c >= ' ' && c != '<' && c != '>' && c != '"' && c != '|' && c != '?'
                    && c != '*' && c != ':' && c != '/' && c != '\\') {
                clean.append(c);
            }
        }
        return clean.isEmpty() ? "config" : clean.toString();
    }
}
