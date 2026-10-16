package dev.transparentinventory.client.mixin;

import dev.transparentinventory.client.render.Opacity;
import dev.transparentinventory.client.render.SpatialGuiRenderer;
import dev.transparentinventory.client.render.SpatialPictureState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tint immutable extraction states; never touch global GPU/shader alpha. Item,
 * text, entity and deferred tooltip states retain their original colors. */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
    @Inject(method = "addPicturesInPictureState", at = @At("HEAD"))
    private void spatial$picture(PictureInPictureRenderState state, CallbackInfo ci) {
        var pose = SpatialGuiRenderer.picturePose();
        if (pose == null) return;
        if (state instanceof SpatialPictureState spatial) spatial.spatial$pose(pose);
        else {
            var screen = dev.transparentinventory.client.compat.MinecraftAccess.screen(net.minecraft.client.Minecraft.getInstance());
            if (screen != null) dev.transparentinventory.client.compat.ScreenCompatibilityManager.disable(screen,
                    new IllegalArgumentException("Unsupported picture-in-picture state: " + state.getClass().getName()));
        }
    }

    @ModifyVariable(method = "addGuiElement", at = @At("HEAD"), argsOnly = true)
    private GuiElementRenderState spatial$opacity(GuiElementRenderState state) {
        float alpha = SpatialGuiRenderer.opacity();
        if (alpha >= 1) return state;
        if (state instanceof BlitRenderState blit) {
            return new BlitRenderState(blit.pipeline(), blit.textureSetup(), blit.pose(), blit.x0(), blit.y0(), blit.x1(), blit.y1(),
                    blit.u0(), blit.u1(), blit.v0(), blit.v1(), Opacity.multiply(blit.color(), alpha), blit.scissorArea(), blit.bounds());
        }
        if (state instanceof ColoredRectangleRenderState rect) {
            return new ColoredRectangleRenderState(rect.pipeline(), rect.textureSetup(), rect.pose(), rect.x0(), rect.y0(), rect.x1(), rect.y1(),
                    Opacity.multiply(rect.col1(), alpha), Opacity.multiply(rect.col2(), alpha), rect.scissorArea(), rect.bounds());
        }
        return state;
    }
}
