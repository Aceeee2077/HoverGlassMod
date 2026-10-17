package dev.transparentinventory.gametest.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeTestAccess {
    @Invoker("getTabX") int spatialtest$tabX(CreativeModeTab tab);
    @Invoker("getTabY") int spatialtest$tabY(CreativeModeTab tab);
    @Accessor("scrollOffs") float spatialtest$scroll();
    @Accessor("searchBox") EditBox spatialtest$search();
    @Accessor("destroyItemSlot") net.minecraft.world.inventory.Slot spatialtest$trash();
}
