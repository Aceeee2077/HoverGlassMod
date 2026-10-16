package dev.transparentinventory.client.legacy;

import dev.transparentinventory.client.ModClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

/**
 * Draws the inventory player preview wherever the entrance animation wants it.
 *
 * <p>The vanilla helper was reshaped several times between 1.20.1 and 1.21.11 - the
 * early releases take a point plus a scale, the later ones a bounding box plus a pair of
 * look angles - and calling it with the wrong signature would crash the render thread.
 * It is therefore resolved once by reflection and the argument list is built from the
 * arity we actually found. Releases we cannot adapt simply keep the panel-only entrance.
 */
public final class LegacyEntityPreview {
    private static Method resolved;

    private LegacyEntityPreview() { }

    public static void draw(GuiGraphics graphics, int centerX, int centerY, int height,
                            float lookX, float lookY, LivingEntity entity) {
        Method method = method();
        if (method == null) return;
        Object[] arguments = arguments(method, graphics, centerX, centerY, height, lookX, lookY, entity);
        if (arguments == null) return;
        try {
            method.invoke(null, arguments);
        } catch (ReflectiveOperationException | RuntimeException error) {
            resolved = null;
            ModClient.LOGGER.warn("[Hoverglass] Player preview disabled", error);
        }
    }

    private static Method method() {
        if (resolved != null) return resolved;
        for (Method candidate : InventoryScreen.class.getMethods()) {
            Class<?>[] types = candidate.getParameterTypes();
            if (!candidate.getName().equals("renderEntityInInventoryFollowsMouse")
                    || types.length < 6
                    || !types[0].isAssignableFrom(GuiGraphics.class)
                    || !LivingEntity.class.isAssignableFrom(types[types.length - 1])) continue;
            resolved = candidate;
            return resolved;
        }
        return null;
    }

    private static Object[] arguments(Method method, GuiGraphics graphics, int centerX, int centerY,
                                      int height, float lookX, float lookY, LivingEntity entity) {
        Class<?>[] types = method.getParameterTypes();
        for (int i = 1; i < types.length - 1; i++) {
            if (types[i] != int.class && types[i] != float.class) return null;
        }
        // 1.20.1 - 1.20.4: (graphics, x, y, scale, lookX, lookY, entity)
        if (types.length == 7) {
            return new Object[] { graphics, centerX, centerY, height, lookX, lookY, entity };
        }
        // 1.20.5+: (graphics, x1, y1, x2, y2, size, lookX, lookY, [offset], entity)
        if (types.length == 9 || types.length == 10) {
            int half = height / 2;
            Object[] arguments = new Object[types.length];
            arguments[0] = graphics;
            arguments[1] = centerX - half;
            arguments[2] = centerY - height;
            arguments[3] = centerX + half;
            arguments[4] = centerY;
            arguments[5] = height;
            arguments[6] = lookX;
            arguments[7] = lookY;
            for (int i = 8; i < types.length - 1; i++) arguments[i] = 0.0F;
            arguments[types.length - 1] = entity;
            return arguments;
        }
        return null;
    }
}
