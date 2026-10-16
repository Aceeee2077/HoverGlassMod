package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import dev.transparentinventory.client.input.SpatialInputMapper;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow private double xpos;
    @Shadow private double ypos;
    @Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"), cancellable = true)
    private void spatial$x(Window window, CallbackInfoReturnable<Double> cir) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.mapsInput()) cir.setReturnValue(SpatialInputMapper.x(manager.transform(), cir.getReturnValue(), MouseHandler.getScaledYPos(window, ypos)));
    }
    @Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"), cancellable = true)
    private void spatial$y(Window window, CallbackInfoReturnable<Double> cir) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.mapsInput()) cir.setReturnValue(SpatialInputMapper.y(manager.transform(), MouseHandler.getScaledXPos(window, xpos), cir.getReturnValue()));
    }

    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean spatial$click(Screen screen, MouseButtonEvent event, boolean doubled, Operation<Boolean> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return true;
        return manager.settings() != null ? manager.settings().mouseClicked(event, doubled) : original.call(screen, event, doubled);
    }
    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean spatial$release(Screen screen, MouseButtonEvent event, Operation<Boolean> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return true;
        return manager.settings() != null ? manager.settings().mouseReleased(event) : original.call(screen, event);
    }
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean spatial$scroll(Screen screen, double x, double y, double dx, double dy, Operation<Boolean> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return true;
        return manager.settings() != null ? manager.settings().mouseScrolled(x, y, dx, dy) : original.call(screen, x, y, dx, dy);
    }
    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void spatial$move(Screen screen, double x, double y, Operation<Void> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return;
        if (manager.settings() != null) manager.settings().mouseMoved(x, y);
        else original.call(screen, x, y);
    }
    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean spatial$drag(Screen screen, MouseButtonEvent event, double dx, double dy, Operation<Boolean> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return true;
        if (manager.settings() != null) return manager.settings().mouseDragged(event, dx, dy);
        var transform = manager.transform();
        return original.call(screen, event, SpatialInputMapper.dx(transform, dx, dy), SpatialInputMapper.dy(transform, dx, dy));
    }
}
