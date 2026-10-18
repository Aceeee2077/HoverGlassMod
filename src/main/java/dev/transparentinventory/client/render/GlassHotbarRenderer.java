package dev.transparentinventory.client.render;

import dev.transparentinventory.client.InventoryConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.transparentinventory.client.render.GlassInventoryRenderer.color;
import static dev.transparentinventory.client.render.GlassInventoryRenderer.frame;

/** Replaces HUD chrome only, using the same glass material as the inventory. */
public final class GlassHotbarRenderer {
    private GlassHotbarRenderer() { }

    public static boolean enabled() {
        return InventoryConfig.get().enabled && InventoryConfig.get().glassHotbar;
    }

    public static void bar(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        panel(g, x, y, width, height);
        for (int slot = 0; slot < 9; slot++) slot(g, x + 2 + slot * 20, y + 2);
    }

    public static void offhand(GuiGraphicsExtractor g, int x, int y, boolean left) {
        // Vanilla's right-hand sprite has an extra connector before the item well.
        int panelX = x + (left ? 1 : 8);
        panel(g, panelX, y + 1, 22, 22);
        slot(g, panelX + 2, y + 3);
    }

    public static void selection(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        int sx = x + 2, sy = y + 2;
        g.fill(sx + 1, sy + 1, sx + width - 5, sy + height - 4,
                color(InventoryConfig.get().hotbarOpacity * 0.45, 0xB6EFFF));
        frame(g, sx, sy, width - 4, height - 3, color(0.95, 0xF2FCFF));
        g.fill(sx + 3, sy + 1, sx + width - 7, sy + 2, color(0.9, 0xB9F4FF));
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        var config = InventoryConfig.get();
        g.fillGradient(x + 1, y + 2, x + w - 1, y + h - 1,
                color(config.hotbarOpacity, 0xBFDDEB), color(config.hotbarOpacity, 0x47728B));
        frame(g, x, y, w, h, color(config.borderOpacity, 0xEFFAFF));
        g.fill(x + 4, y + 1, x + w - 4, y + 2, color(config.borderOpacity * 0.6, 0xFFFFFF));
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        var config = InventoryConfig.get();
        g.fill(x + 1, y + 1, x + 17, y + 17,
                color(config.slotOpacity * config.hotbarOpacity, 0xD8EDFF));
        frame(g, x, y, 18, 18, color(config.slotBorderOpacity, 0xE8F6FF));
    }
}
