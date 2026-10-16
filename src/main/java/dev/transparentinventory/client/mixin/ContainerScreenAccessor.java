package dev.transparentinventory.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int spatial$left();
    @Accessor("topPos") int spatial$top();
    @Accessor("imageWidth") int spatial$width();
    @Accessor("imageHeight") int spatial$height();
    @Accessor("hoveredSlot") Slot spatial$hoveredSlot();
}
