package dev.transparentinventory.client.screen;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.anchor.AnchorType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Exact class > nearest registered superclass > generic container > opt-in screen space.
 * Exclusions always win. Class decisions are cached; live configuration is checked separately. */
public final class SpatialScreenRegistry {
    public static final SpatialScreenRegistry INSTANCE = new SpatialScreenRegistry();
    private final Map<Class<?>, SpatialScreenProfile> profiles = new HashMap<>();
    private final Map<Class<?>, Optional<SpatialScreenProfile>> cache = new HashMap<>();
    private final SpatialScreenProfile fallback = new SpatialScreenProfile("screen_space", AnchorType.SCREEN_SPACE, 1, 0, 0);

    private SpatialScreenRegistry() {
        register(AbstractContainerScreen.class, new SpatialScreenProfile("handled", AnchorType.BLOCK, 1, 0, 0));
        register(InventoryScreen.class, new SpatialScreenProfile("inventory", AnchorType.PLAYER, 1, -0.08, 0));
        register(MerchantScreen.class, new SpatialScreenProfile("merchant", AnchorType.ENTITY, 1, 0, 0));
        register(HorseInventoryScreen.class, new SpatialScreenProfile("horse", AnchorType.ENTITY, 1, 0, 0));
    }

    public void register(Class<? extends Screen> type, SpatialScreenProfile profile) {
        profiles.put(type, profile);
        cache.clear();
    }

    public SpatialScreenProfile resolve(Screen screen) {
        InventoryConfig config = InventoryConfig.get();
        if (screen == null || screen instanceof SpatialSettingsScreen || !config.enabled) return null;
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            if (config.excludedScreens.contains(type.getName())) return null;
        }
        SpatialScreenProfile profile = cache.computeIfAbsent(screen.getClass(), type -> {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                SpatialScreenProfile found = profiles.get(current);
                if (found != null) return Optional.of(found);
            }
            return Optional.empty();
        }).orElse(null);
        if (profile != null) return config.defaultSpatialHandledScreens ? profile : null;
        return config.additionalScreens.contains(screen.getClass().getName()) ? fallback : null;
    }
}
