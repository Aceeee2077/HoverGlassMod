package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.render.SpatialPictureState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.*;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GuiEntityRenderState.class, GuiBookModelRenderState.class, GuiSkinRenderState.class,
        GuiBannerResultRenderState.class, GuiProfilerChartRenderState.class})
public abstract class PictureInPictureMixin implements SpatialPictureState {
    @Unique private Matrix3x2fc spatial$matrix;
    @Override public void spatial$pose(Matrix3x2fc pose) { spatial$matrix = pose; }

    public Matrix3x2fc pose() {
        return spatial$matrix == null ? PictureInPictureRenderState.IDENTITY_POSE : spatial$matrix;
    }

    @Inject(method = "bounds", at = @At("RETURN"), cancellable = true)
    private void spatial$bounds(CallbackInfoReturnable<ScreenRectangle> cir) {
        if (spatial$matrix == null) return;
        var state = (PictureInPictureRenderState) (Object) this;
        var rect = new ScreenRectangle(state.x0(), state.y0(), state.x1() - state.x0(), state.y1() - state.y0()).transformMaxBounds(spatial$matrix);
        cir.setReturnValue(state.scissorArea() == null ? rect : rect.intersection(state.scissorArea()));
    }
}
