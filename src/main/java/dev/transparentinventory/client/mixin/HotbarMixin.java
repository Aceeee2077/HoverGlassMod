package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.transparentinventory.client.render.GlassHotbarRenderer;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Target only the four hotbar sprites, preserving items, attack indicator and HUD visibility. */
@Mixin(Hud.class)
public abstract class HotbarMixin {
    @Shadow @Final private static Identifier HOTBAR_SPRITE;
    @Shadow @Final private static Identifier HOTBAR_SELECTION_SPRITE;
    @Shadow @Final private static Identifier HOTBAR_OFFHAND_LEFT_SPRITE;
    @Shadow @Final private static Identifier HOTBAR_OFFHAND_RIGHT_SPRITE;

    @WrapOperation(method = "extractItemHotbar", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void glass$chrome(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite,
                               int x, int y, int width, int height, Operation<Void> original) {
        if (!GlassHotbarRenderer.enabled()) {
            original.call(g, pipeline, sprite, x, y, width, height);
        } else if (sprite.equals(HOTBAR_SPRITE)) {
            GlassHotbarRenderer.bar(g, x, y, width, height);
        } else if (sprite.equals(HOTBAR_SELECTION_SPRITE)) {
            GlassHotbarRenderer.selection(g, x, y, width, height);
        } else if (sprite.equals(HOTBAR_OFFHAND_LEFT_SPRITE) || sprite.equals(HOTBAR_OFFHAND_RIGHT_SPRITE)) {
            GlassHotbarRenderer.offhand(g, x, y, sprite.equals(HOTBAR_OFFHAND_LEFT_SPRITE));
        } else {
            original.call(g, pipeline, sprite, x, y, width, height);
        }
    }
}
