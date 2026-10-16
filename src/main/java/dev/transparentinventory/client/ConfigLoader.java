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
            else ModClient.LOGGER.warn("Could not parse {}. Using defaults; original file preserved.", path);
        }
        config.sanitize();
        InventoryConfig.setInstance(config);
    }

    public static synchronized boolean save() {
        InventoryConfig.get().sanitize();
        return writeDefault(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME), InventoryConfig.get());
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
