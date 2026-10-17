package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.render.GlassInventoryRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void spatial$glass(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick, CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        if (!GlassInventoryRenderer.active(self)) return;
        self.extractTransparentBackground(graphics);
        GlassInventoryRenderer.draw(graphics, self);
        ci.cancel();
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void spatial$glassLabel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        if (!GlassInventoryRenderer.active(self)) return;
        graphics.text(Minecraft.getInstance().font, self.getTitle(), 97, 6, 0xFFF0F8FF, true);
        ci.cancel();
    }
}
