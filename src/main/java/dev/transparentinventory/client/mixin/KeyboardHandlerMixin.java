package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.transparentinventory.client.screen.SpatialScreenManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void spatial$settings(long window, int action, KeyEvent key, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().handle() || key.key() != InputConstants.KEY_F8) return;
        if (action == InputConstants.PRESS) SpatialScreenManager.INSTANCE.toggleSettings();
        ci.cancel();
    }
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean spatial$key(Screen screen, KeyEvent key, Operation<Boolean> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return true;
        return manager.settings() != null ? manager.settings().keyPressed(key) : original.call(screen, key);
    }
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean spatial$release(Screen screen, KeyEvent key, Operation<Boolean> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return true;
        return manager.settings() != null ? manager.settings().keyReleased(key) : original.call(screen, key);
    }
    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
    private boolean spatial$character(Screen screen, CharacterEvent key, Operation<Boolean> original) {
        var manager = SpatialScreenManager.INSTANCE;
        if (manager.blocksInput()) return true;
        return manager.settings() != null ? manager.settings().charTyped(key) : original.call(screen, key);
    }
}
