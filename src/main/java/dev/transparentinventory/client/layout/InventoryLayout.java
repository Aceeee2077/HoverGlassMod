package dev.transparentinventory.client.layout;

import dev.transparentinventory.client.InventoryConfig;

/**
 * Resolution-independent layout maths for the inventory screen.
 *
 * <p>Nothing here is hard-coded to a specific resolution: every position is derived
 * from the actual screen size, the GUI-scaled panel size and the recipe-book state,
 * so 16:9, 16:10, 21:9, 4K and GUI Scale 2/3/4/Auto all work.
 *
 * <p>Plain Java only (no Minecraft types) so it can be unit tested.
 */
public final class InventoryLayout {

    /** Final panel top-left X. */
    public final int leftPos;
    /** Resting panel top-left Y (the animation slides the real {@code topPos} towards this). */
    public final int targetTopPos;
    /** Final panel centre X, used as the look-at target. */
    public final double panelCenterX;
    /** Resting panel centre Y, used as the look-at target. */
    public final double panelCenterY;

    /** Player preview capture rectangle (upper-left / lower-right), in GUI pixels. */
    public final int playerX1;
    public final int playerY1;
    public final int playerX2;
    public final int playerY2;
    /** Model scale passed to the entity render state (1.0 == 1 block == 1 GUI pixel). */
    public final float playerScale;
    public final double playerCenterX;
    public final double playerCenterY;

    public final boolean recipeBookVisible;

    private InventoryLayout(int leftPos, int targetTopPos, double panelCenterX, double panelCenterY,
                            int playerX1, int playerY1, int playerX2, int playerY2,
                            float playerScale, double playerCenterX, double playerCenterY,
                            boolean recipeBookVisible) {
        this.leftPos = leftPos;
        this.targetTopPos = targetTopPos;
        this.panelCenterX = panelCenterX;
        this.panelCenterY = panelCenterY;
        this.playerX1 = playerX1;
        this.playerY1 = playerY1;
        this.playerX2 = playerX2;
        this.playerY2 = playerY2;
        this.playerScale = playerScale;
        this.playerCenterX = playerCenterX;
        this.playerCenterY = playerCenterY;
        this.recipeBookVisible = recipeBookVisible;
    }

    /**
     * Computes the layout for the current frame.
     *
     * <p>The player preview is a perfect square-capture box whose only job is to size
     * the render texture and clip the model; the model itself is centred horizontally
     * on the box and vertically positioned so that the feet sit at
     * {@code centre + 0.9625 * scale} and the head at {@code centre - 0.8375 * scale}.
     * The box is therefore at least {@code 1.925 * scale} tall; we use {@code 2.4}
     * so the model is not clipped at the edge of the box when the head pitches up
     * to its maximum angle.
     */
    public static InventoryLayout compute(int screenW, int screenH, int imageW, int imageH,
                                          boolean bookVisible, InventoryConfig cfg) {
        InventoryConfig config = (cfg == null) ? new InventoryConfig() : cfg;
        int safeScreenW = Math.max(1, screenW);
        int safeScreenH = Math.max(1, screenH);
        int panelW = Math.max(1, imageW);
        int panelH = Math.max(1, imageH);

        // --- Panel horizontal placement -------------------------------------
        double panelLeft = safeScreenW * config.panelCenterRatio - panelW / 2.0;
        if (bookVisible) {
            // Vanilla places the panel centred plus an offset when the recipe book
            // is open; keep our panel from overlapping the book by taking the max.
            double bookAware = (safeScreenW - panelW) / 2.0 + config.recipeBookOffset;
            if (bookAware > panelLeft) {
                panelLeft = bookAware;
            }
        }
        int clampedLeft = (int) Math.round(clamp(panelLeft, 0.0, Math.max(0, safeScreenW - panelW)));

        // --- Panel vertical resting position ---------------------------------
        int targetTop = Math.max(0, (safeScreenH - panelH) / 2);

        double panelCenterX = clampedLeft + panelW / 2.0;
        double panelCenterY = targetTop + panelH / 2.0;

        // --- Player preview ---------------------------------------------------
        // Model height on screen is 1.8 * scale GUI pixels, so pick the scale from
        // the requested fraction of the screen height.
        float playerScale = (float) (safeScreenH * config.playerHeightRatio / 1.8);
        if (playerScale < 1.0F) {
            playerScale = 1.0F;
        }
        int boxW = Math.max(1, (int) Math.ceil(playerScale * 2.0));
        int boxH = Math.max(1, (int) Math.ceil(playerScale * 2.4));

        int centerX = (int) Math.round(safeScreenW * config.playerCenterXRatio);
        int centerY = (int) Math.round(safeScreenH * config.playerCenterYRatio);

        int x1 = centerX - boxW / 2;
        int y1 = centerY - boxH / 2;
        int x2 = x1 + boxW;
        int y2 = y1 + boxH;

        // Keep the box on screen. If it is smaller than the screen we translate it;
        // only when the box itself is larger than the screen do we clip it.
        if (boxW <= safeScreenW) {
            if (x1 < 0) {
                x2 -= x1;
                x1 = 0;
            }
            if (x2 > safeScreenW) {
                int shift = x2 - safeScreenW;
                x1 -= shift;
                x2 -= shift;
            }
        } else {
            x1 = 0;
            x2 = safeScreenW;
        }
        if (boxH <= safeScreenH) {
            if (y1 < 0) {
                y2 -= y1;
                y1 = 0;
            }
            if (y2 > safeScreenH) {
                int shift = y2 - safeScreenH;
                y1 -= shift;
                y2 -= shift;
            }
        } else {
            y1 = 0;
            y2 = safeScreenH;
        }

        return new InventoryLayout(
                clampedLeft, targetTop, panelCenterX, panelCenterY,
                x1, y1, x2, y2,
                playerScale, (x1 + x2) / 2.0, (y1 + y2) / 2.0,
                bookVisible);
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
