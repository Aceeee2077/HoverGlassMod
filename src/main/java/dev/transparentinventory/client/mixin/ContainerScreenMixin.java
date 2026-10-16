package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.transparentinventory.client.render.SpatialGuiRenderer;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @WrapMethod(method = {"extractSlotHighlightBack", "extractSlotHighlightFront"})
    private void spatial$readableHighlight(GuiGraphicsExtractor graphics, Operation<Void> original) {
        boolean wasDeferred = SpatialGuiRenderer.deferred();
        SpatialGuiRenderer.deferred(true);
        try { original.call(graphics); }
        finally { SpatialGuiRenderer.deferred(wasDeferred); }
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void spatial$close(CallbackInfo ci) {
        if (SpatialScreenManager.INSTANCE.requestClose((AbstractContainerScreen<?>) (Object) this)) ci.cancel();
    }
}
