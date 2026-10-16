package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.input.SpatialInputMapper;
import dev.transparentinventory.client.render.Opacity;
import dev.transparentinventory.client.render.SpatialGuiRenderer;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @WrapMethod(method = "extractRenderStateWithTooltipAndSubtitles")
    private void spatial$extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        Screen self = (Screen) (Object) this;
        SpatialScreenManager manager = SpatialScreenManager.INSTANCE;
        var transform = manager.prepare(self);
        if (manager.finishClose(self)) return;
        Minecraft client = Minecraft.getInstance();
        var window = client.getWindow();
        // Read unmodified raw coordinates, because the GUI caller may already have
        // called the mapped MouseHandler getters before this frame's transform existed.
        double rawX = MouseHandler.getScaledXPos(window, client.mouseHandler.xpos());
        double rawY = MouseHandler.getScaledYPos(window, client.mouseHandler.ypos());
        var extractor = (GuiGraphicsExtractorAccessor) graphics;
        int previousX = extractor.spatial$mouseX(), previousY = extractor.spatial$mouseY();
        if (manager.active(self)) {
            int localX = (int) SpatialInputMapper.x(transform, rawX, rawY), localY = (int) SpatialInputMapper.y(transform, rawX, rawY);
            extractor.spatial$mouseX(localX);
            extractor.spatial$mouseY(localY);
            SpatialGuiRenderer.begin(graphics, transform);
            try {
                original.call(graphics, localX, localY, partialTick);
            } finally {
                SpatialGuiRenderer.end(graphics);
                extractor.spatial$mouseX(previousX);
                extractor.spatial$mouseY(previousY);
            }
        } else {
            if (self == dev.transparentinventory.client.compat.MinecraftAccess.screen(client)) { mouseX = (int) rawX; mouseY = (int) rawY; }
            extractor.spatial$mouseX(mouseX);
            extractor.spatial$mouseY(mouseY);
            try { original.call(graphics, mouseX, mouseY, partialTick); }
            finally {
                extractor.spatial$mouseX(previousX);
                extractor.spatial$mouseY(previousY);
            }
        }

        // Landing flash: a bright core plus a soft band along the panel's leading edge,
        // drawn in window coordinates so it sits on top of the sliding panel.
        double flash = manager.impactFlash();
        if (flash > 0.01 && manager.active(self)) {
            int coreAlpha = (int) (flash * 140);
            int glowAlpha = (int) (flash * 60);
            if (coreAlpha > 0) {
                int x = (int) manager.flashEdgeX();
                int y = (int) manager.flashEdgeY();
                int halfH = Math.max(1, (int) manager.flashHalfHeight());
                graphics.pose().pushMatrix();
                graphics.pose().identity();
                SpatialGuiRenderer.deferred(true);
                try {
                    graphics.fill(x - 12, y - halfH, x + 12, y + halfH, (glowAlpha << 24) | 0xFFFFFF);
                    graphics.fill(x - 2, y - halfH, x + 2, y + halfH, (coreAlpha << 24) | 0xFFFFFF);
                } finally {
                    SpatialGuiRenderer.deferred(false);
                    graphics.pose().popMatrix();
                }
            }
        }

        var settings = manager.settings();
        if (settings != null && self == dev.transparentinventory.client.compat.MinecraftAccess.screen(client)) {
            if (settings.width != self.width || settings.height != self.height) settings.resize(self.width, self.height);
            settings.extractRenderStateWithTooltipAndSubtitles(graphics, (int) rawX, (int) rawY, partialTick);
        }
    }

    @WrapOperation(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"))
    private void spatial$tooltip(GuiGraphicsExtractor graphics, int x, int y, float tick, Operation<Void> original) {
        SpatialGuiRenderer.deferred(true);
        try { original.call(graphics, x, y, tick); }
        finally { SpatialGuiRenderer.deferred(false); }
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void spatial$dim(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!SpatialGuiRenderer.extracting()) return;
        // Dim the world in window coordinates, independent of panel scale/position.
        graphics.pose().pushMatrix();
        graphics.pose().identity();
        SpatialGuiRenderer.deferred(true);
        try { graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), Opacity.multiply(0xFF000000, InventoryConfig.get().worldDimOpacity)); }
        finally { SpatialGuiRenderer.deferred(false); }
        graphics.pose().popMatrix();
        ci.cancel();
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void spatial$blur(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (SpatialGuiRenderer.extracting()) ci.cancel();
    }
}
