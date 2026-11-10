package dev.transparentinventory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Clipboard-friendly config export and import.
 *
 * <p>Exports the same JSON that {@link ConfigLoader} writes to disk, and only
 * accepts text that parses into a config object. Everything imported is passed
 * through {@link InventoryConfig#sanitize()} before use, so a pasted snippet
 * can never inject out-of-range values or malformed screen rules.
 */
public final class ConfigTransfer {

    /** Upper bound for pasted text; a real config is far smaller. */
    public static final int MAX_LENGTH = 262_144;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigTransfer() {
    }

    /** Serializes the current values as pretty JSON; the config is sanitized first. */
    public static String export(InventoryConfig config) {
        InventoryConfig source = config == null ? new InventoryConfig() : config;
        source.sanitize();
        return GSON.toJson(source);
    }

    /**
     * Parses pasted JSON into a sanitized config.
     *
     * @return the parsed config, or {@code null} when the text is blank, too long,
     *         not a JSON object, or contains values Gson cannot map
     */
    public static InventoryConfig parse(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) return null;
        InventoryConfig config;
        try {
            config = GSON.fromJson(trimmed, InventoryConfig.class);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (config == null) return null;
        config.sanitize();
        return config;
    }
}
