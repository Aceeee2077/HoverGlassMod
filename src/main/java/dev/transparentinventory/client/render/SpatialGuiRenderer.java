package dev.transparentinventory.client.render;

import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/** Extraction-scoped state only. The GPU consumes immutable states after this scope ends. */
public final class SpatialGuiRenderer {
    private static boolean extracting;
    private static boolean deferred;
    private static float opacity = 1;
    private static Matrix3x2fc picturePose;
    private SpatialGuiRenderer() { }
    public static boolean extracting() { return extracting; }
    public static float opacity() { return extracting && !deferred ? opacity : 1; }
    public static void deferred(boolean value) { deferred = value; }
    public static boolean deferred() { return deferred; }
    public static Matrix3x2fc picturePose() { return extracting && !deferred ? picturePose : null; }
    public static void begin(GuiGraphicsExtractor graphics, SpatialTransform transform) {
        opacity = SpatialScreenManager.INSTANCE.backgroundOpacity();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) transform.centerX(), (float) transform.centerY());
        graphics.pose().mul(new Matrix3x2f((float) transform.a(), (float) transform.b(),
                (float) transform.c(), (float) transform.d(), 0, 0));
        graphics.pose().translate((float) -transform.originX(), (float) -transform.originY());
        picturePose = new Matrix3x2f(graphics.pose());
        extracting = true;
    }
    public static void end(GuiGraphicsExtractor graphics) {
        extracting = false;
        deferred = false;
        opacity = 1;
        picturePose = null;
        graphics.pose().popMatrix();
    }
}
