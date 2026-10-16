package dev.transparentinventory.client.legacy.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Field access for the legacy entrance. {@code leftPos}/{@code imageWidth} are declared
 * {@code protected final} on the vanilla class in some releases and re-declared in
 * others, so an accessor is used instead of {@code @Shadow} to stay signature-agnostic.
 */
@Mixin(AbstractContainerScreen.class)
public interface LegacyContainerScreenAccessor {
    @Accessor("leftPos") int legacy$leftPos();
    @Accessor("leftPos") void legacy$setLeftPos(int value);
    @Accessor("topPos") int legacy$topPos();
    @Accessor("imageWidth") int legacy$imageWidth();
    @Accessor("imageHeight") int legacy$imageHeight();
}
