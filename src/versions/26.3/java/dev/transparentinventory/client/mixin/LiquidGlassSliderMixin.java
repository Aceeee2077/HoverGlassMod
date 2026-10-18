package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.transparentinventory.client.render.LiquidGlassRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSliderButton.class)
public abstract class LiquidGlassSliderMixin {
    @WrapOperation(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void glass$sprite(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite,
                              int x, int y, int width, int height, int tint, Operation<Void> original) {
        if (!LiquidGlassRenderer.enabled()) {
            original.call(g, pipeline, sprite, x, y, width, height, tint);
            return;
        }
        var widget = (AbstractWidget) (Object) this;
        LiquidGlassRenderer.surface(g, x, y, width, height, widget.getAlpha(),
                widget.isHoveredOrFocused() || width == 8, widget.active, 0.5);
    }
}
