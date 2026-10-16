package dev.transparentinventory.gametest.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardTestAccess {
    @Invoker("keyPress") void spatialtest$key(long window, int action, KeyEvent event);
}
