package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.render.LiquidGlassRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class LiquidGlassButtonMixin {
    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void glass$sprite(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!LiquidGlassRenderer.enabled()) return;
        var widget = (AbstractWidget) (Object) this;
        LiquidGlassRenderer.widget(graphics, widget, ((GuiGraphicsExtractorAccessor) graphics).spatial$mouseX());
        ci.cancel();
    }
}
