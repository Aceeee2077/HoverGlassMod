package dev.transparentinventory.client.compat;

import dev.transparentinventory.client.ModClient;
import net.minecraft.client.gui.screens.Screen;
import java.util.HashSet;
import java.util.Set;

/** Session-local circuit breaker for failures in our projection code. Vanilla render
 * errors are never swallowed or retried against partially submitted GUI state. */
public final class ScreenCompatibilityManager {
    private static final Set<Class<?>> failed = new HashSet<>();
    private ScreenCompatibilityManager() { }
    public static boolean failed(Screen screen) { return failed.contains(screen.getClass()); }
    public static void disable(Screen screen, RuntimeException error) {
        if (failed.add(screen.getClass())) ModClient.LOGGER.warn("Spatial projection disabled for {}", screen.getClass().getName(), error);
    }
}
