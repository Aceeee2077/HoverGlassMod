package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.render.LiquidGlassRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class LiquidGlassPauseMixin extends net.minecraft.client.gui.screens.Screen {
    protected LiquidGlassPauseMixin() { super(net.minecraft.network.chat.Component.empty()); }
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void glass$background(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick, CallbackInfo ci) {
        if (!LiquidGlassRenderer.enabled() || !((PauseScreen) (Object) this).showsPauseMenu()) return;
        extractBlurredBackground(g);
        g.fillGradient(0, 0, g.guiWidth(), g.guiHeight(), 0x70101E31, 0x90304455);
        ci.cancel();
    }
}
