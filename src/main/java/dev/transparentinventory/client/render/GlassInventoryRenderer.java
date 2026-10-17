package dev.transparentinventory.client.render;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.mixin.ContainerScreenAccessor;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;

/** Glass chrome only; vanilla still draws and handles all slots, items and widgets. */
public final class GlassInventoryRenderer {
    private GlassInventoryRenderer() { }

    public static boolean active(AbstractContainerScreen<?> screen) {
        return (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)
                && InventoryConfig.get().glassInventory && SpatialGuiRenderer.extracting()
                && SpatialScreenManager.INSTANCE.active(screen);
    }

    public static void draw(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen) {
        var bounds = (ContainerScreenAccessor) screen;
        var config = InventoryConfig.get();
        var manager = SpatialScreenManager.INSTANCE;
        int x = bounds.spatial$left(), y = bounds.spatial$top();
        int w = bounds.spatial$width(), h = bounds.spatial$height();
        double visibility = manager.visibility();
        double opacity = config.opacityFor(screen.getClass().getName());
        double impact = manager.impactFlash();
        // These states carry their own alpha, avoiding a second panel-opacity multiply.
        boolean previous = SpatialGuiRenderer.deferred();
        SpatialGuiRenderer.deferred(true);
        try {
            int topColor = color(opacity * visibility, 0xBFDDEB);
            int bottomColor = color(opacity * visibility, 0x47728B);
            g.fillGradient(x + 1, y + 2, x + w - 1, y + h - 2, topColor, bottomColor);
            g.fill(x + 4, y, x + w - 4, y + 2, topColor);
            g.fill(x + 4, y + h - 2, x + w - 4, y + h, bottomColor);
            // Wide faint halo, crisp outer rim and a second reflection inside the glass.
            frame(g, x - 2, y - 2, w + 4, h + 4, color(visibility * (0.08 + impact * 0.22), 0xD3F6FF));
            frame(g, x, y, w, h, color(config.borderOpacity * visibility, 0xEFFAFF));
            frame(g, x + 2, y + 2, w - 4, h - 4, color(config.borderOpacity * visibility * 0.25, 0xFFFFFF));
            g.fill(x + 5, y + 1, x + w - 5, y + 2, color(visibility * 0.75, 0xFFFFFF));
            g.fill(x + 5, y + h - 2, x + w - 5, y + h - 1, color(visibility * 0.48, 0xFFE4B8));

            if (screen instanceof InventoryScreen) {
                silhouette(g, x + 26, y + 7, 49, 72, visibility);
            } else if (((CreativeModeInventoryScreen) screen).isInventoryOpen()) {
                silhouette(g, x + 73, y + 6, 32, 43, visibility);
            } else {
                frame(g, x + 174, y + 17, 14, 114, color(visibility * 0.32, 0xD3F6FF));
            }
            for (Slot slot : screen.getMenu().slots) {
                if (!slot.isActive() || slot.x < 0 || slot.y < 0 || slot.x + 16 > w || slot.y + 16 > h) continue;
                int sx = x + slot.x - 1, sy = y + slot.y - 1;
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17,
                        color(config.slotOpacity * opacity * visibility, 0xD8EDFF));
                frame(g, sx, sy, 18, 18, color(config.slotBorderOpacity * visibility, 0xE8F6FF));
                g.fill(sx + 2, sy + 1, sx + 16, sy + 2, color(visibility * 0.22, 0xFFFFFF));
            }
            if (screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen()) {
                Slot trash = screen.getMenu().slots.getLast();
                int tx = x + trash.x, ty = y + trash.y;
                int bin = color(visibility * 0.72, 0xE8F6FF);
                frame(g, tx + 4, ty + 5, 8, 9, bin);
                g.fill(tx + 3, ty + 3, tx + 13, ty + 4, bin);
                g.fill(tx + 6, ty + 1, tx + 10, ty + 2, bin);
                g.fill(tx + 7, ty + 7, tx + 8, ty + 12, bin);
            }
            if (screen instanceof InventoryScreen) {
                int arrow = color(visibility * 0.85, 0xF0F8FF);
                g.fill(x + 137, y + 35, x + 145, y + 38, arrow);
                for (int i = 0; i < 5; i++) g.fill(x + 143 + i, y + 32 + i, x + 144 + i, y + 41 - i, arrow);
            }

            // A narrow travelling reflection makes the left-to-right materialization visible.
            if (visibility > 0.01 && visibility < 0.99 && !manager.closing()) {
                int sweep = x + (int) (w * visibility);
                for (int i = -6; i <= 6; i++) {
                    g.fill(sweep + i, y + 4, sweep + i + 1, y + h - 4,
                            color((1 - Math.abs(i) / 7.0) * 0.24 * Math.sin(Math.PI * visibility), 0xE3F9FF));
                }
            }
            if (impact > 0.01) {
                frame(g, x - 1, y - 1, w + 2, h + 2, color(impact * 0.8, 0xFFFFFF));
                for (int i = 0; i < 6; i++) {
                    int px = x + 5 + i * (w - 10) / 5;
                    int length = (int) (impact * (5 + (i % 3) * 3));
                    if (length <= 2) continue;
                    g.fill(px, y - length, px + 1, y - 2, color(impact * 0.55, 0xCBF5FF));
                    g.fill(px, y + h + 2, px + 1, y + h + length, color(impact * 0.55, 0xFFE8C7));
                }
            }
        } finally {
            SpatialGuiRenderer.deferred(previous);
        }
        if (screen instanceof InventoryScreen && !outsidePlayer()) {
            float mouseX = x + 50, mouseY = y + 36;
            if (config.playerLookAtInventory) {
                var client = net.minecraft.client.Minecraft.getInstance();
                var window = client.getWindow();
                double rawX = net.minecraft.client.MouseHandler.getScaledXPos(window, client.mouseHandler.xpos());
                double rawY = net.minecraft.client.MouseHandler.getScaledYPos(window, client.mouseHandler.ypos());
                var transform = manager.transform();
                mouseX = (float) transform.localX(rawX, rawY);
                mouseY = (float) transform.localY(rawX, rawY);
            }
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + 26, y + 8, x + 75, y + 78,
                    30, 0.0625F, mouseX, mouseY, net.minecraft.client.Minecraft.getInstance().player);
        }
    }

    public static boolean outsidePlayer() {
        var config = InventoryConfig.get();
        return SpatialScreenManager.INSTANCE.cinematicInventory() && config.cameraEnabled && config.thirdPersonOnOpen;
    }

    /** Creative tabs and scrollbar thumbs keep vanilla geometry and hit testing. */
    public static void control(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen,
                               int x, int y, int width, int height, boolean selected) {
        var config = InventoryConfig.get();
        double visibility = SpatialScreenManager.INSTANCE.visibility();
        boolean previous = SpatialGuiRenderer.deferred();
        SpatialGuiRenderer.deferred(true);
        try {
            g.fillGradient(x + 1, y + 2, x + width - 1, y + height - 2,
                    color(config.opacityFor(screen.getClass().getName()) * visibility, selected ? 0xBFEDFF : 0x6D91AD),
                    color(config.opacityFor(screen.getClass().getName()) * visibility, 0x34566B));
            frame(g, x, y, width, height, color(visibility * (selected ? 0.9 : config.slotBorderOpacity), 0xE8F8FF));
            if (selected) g.fill(x + 3, y + 2, x + width - 3, y + 3, color(visibility * 0.9, 0xB3F3FF));
        } finally {
            SpatialGuiRenderer.deferred(previous);
        }
    }

    private static void silhouette(GuiGraphicsExtractor g, int x, int y, int width, int height, double visibility) {
        frame(g, x, y, width, height, color(InventoryConfig.get().slotBorderOpacity * visibility, 0xE7F5FF));
        int tint = color(visibility * 0.16, 0xD6EBFF);
        g.fill(x + width * 18 / 49, y + height * 9 / 72, x + width * 31 / 49, y + height * 22 / 72, tint);
        g.fill(x + width * 13 / 49, y + height * 22 / 72, x + width * 36 / 49, y + height * 43 / 72, tint);
        g.fill(x + width * 18 / 49, y + height * 43 / 72, x + width * 24 / 49, y + height * 65 / 72, tint);
        g.fill(x + width * 25 / 49, y + height * 43 / 72, x + width * 31 / 49, y + height * 65 / 72, tint);
    }

    private static int color(double alpha, int rgb) {
        return ((int) Math.round(Math.clamp(alpha, 0, 1) * 255) << 24) | rgb;
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + 1, y + h - 2, color);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, color);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }
}
