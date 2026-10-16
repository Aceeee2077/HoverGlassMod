package dev.transparentinventory.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
    @Accessor("mouseX") int spatial$mouseX();
    @Accessor("mouseY") int spatial$mouseY();
    @Mutable @Accessor("mouseX") void spatial$mouseX(int value);
    @Mutable @Accessor("mouseY") void spatial$mouseY(int value);
}
