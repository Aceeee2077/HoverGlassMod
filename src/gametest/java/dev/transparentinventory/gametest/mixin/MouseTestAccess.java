package dev.transparentinventory.gametest.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseTestAccess {
    @Invoker("onButton") void spatialtest$button(long window, MouseButtonInfo button, int action);
}
