package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.transparentinventory.client.render.SpatialGuiRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @Shadow public abstract Matrix3x2fStack pose();

    @WrapOperation(method = "enableScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;transformAxisAligned(Lorg/joml/Matrix3x2fc;)Lnet/minecraft/client/gui/navigation/ScreenRectangle;"))
    private ScreenRectangle spatial$rotatedScissor(ScreenRectangle rectangle, Matrix3x2fc matrix, Operation<ScreenRectangle> original) {
        return SpatialGuiRenderer.extracting() ? rectangle.transformMaxBounds(matrix) : original.call(rectangle, matrix);
    }

    @WrapOperation(method = "containsPointInScissor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor$ScissorStack;containsPoint(II)Z"))
    private boolean spatial$scissorPoint(@Coerce Object stack, int x, int y, Operation<Boolean> original) {
        if (!SpatialGuiRenderer.extracting()) return original.call(stack, x, y);
        var point = pose().transformPosition(x, y, new org.joml.Vector2f());
        return original.call(stack, (int) point.x, (int) point.y);
    }
    @WrapMethod(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V")
    private void spatial$readableItems(Font font, ItemStack stack, int x, int y, String count, Operation<Void> original) {
        boolean wasDeferred = SpatialGuiRenderer.deferred();
        SpatialGuiRenderer.deferred(true);
        try { original.call(font, stack, x, y, count); }
        finally { SpatialGuiRenderer.deferred(wasDeferred); }
    }
}
