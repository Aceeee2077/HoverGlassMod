package dev.transparentinventory.client.render;

import dev.transparentinventory.client.InventoryConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

/** Lightweight translucent chrome; keeps vanilla labels, layout, input and narration. */
public final class LiquidGlassRenderer {
    private LiquidGlassRenderer() { }

    public static boolean enabled() {
        return InventoryConfig.get().enabled && InventoryConfig.get().liquidGlassUi;
    }

    public static void widget(GuiGraphicsExtractor g, AbstractWidget widget, int mouseX) {
        double light = Math.clamp((mouseX - widget.getX()) / (double) Math.max(1, widget.getWidth()), 0, 1);
        surface(g, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                widget.getAlpha(), widget.isHoveredOrFocused(), widget.active, light);
    }

    public static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               double alpha, boolean highlighted, boolean active, double light) {
        if (w < 4 || h < 4 || alpha <= 0) return;
        // Use the existing extraction scope: glass already carries its own alpha.
        boolean previous = SpatialGuiRenderer.deferred();
        SpatialGuiRenderer.deferred(true);
        try {
            double strength = alpha * (active ? 1 : 0.38);
            int radius = Math.min(7, Math.min(w, h) / 2);
            rounded(g, x, y + 2, w, h, radius, color(strength * 0.20, 0x020813));
            rounded(g, x, y, w, h, radius, color(strength * (highlighted ? 0.60 : 0.25), 0xE0F5FF));
            rounded(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1),
                    color(strength * 0.42, highlighted ? 0x314B66 : 0x1A2A40));
            // Broad upper reflection and a cool lower lip give the surface depth.
            int inside = radius + 1;
            g.fillGradient(x + inside, y + 1, x + w - inside, y + h / 2,
                    color(strength * (highlighted ? 0.32 : 0.20), 0xE4F6FF), color(0, 0xE4F6FF));
            g.fill(x + radius, y + 1, x + w - radius, y + 2, color(strength * 0.68, 0xFFFFFF));
            g.fill(x + radius, y + h - 2, x + w - radius, y + h - 1, color(strength * 0.35, 0x8BCFFF));
            if (highlighted && active) {
                int center = x + radius + (int) ((w - radius * 2) * light);
                int half = Math.max(2, Math.min(16, w / 5));
                for (int dx = -half; dx <= half; dx++) {
                    int px = center + dx;
                    if (px < x + radius || px >= x + w - radius) continue;
                    double glow = strength * 0.50 * (1 - Math.abs(dx) / (double) (half + 1));
                    g.fill(px, y + 1, px + 1, y + 3, color(glow, 0xFFFFFF));
                }
            }
        } finally {
            SpatialGuiRenderer.deferred(previous);
        }
    }

    private static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    private static int color(double alpha, int rgb) {
        return ((int) Math.round(Math.clamp(alpha, 0, 1) * 255) << 24) | rgb;
    }
}
