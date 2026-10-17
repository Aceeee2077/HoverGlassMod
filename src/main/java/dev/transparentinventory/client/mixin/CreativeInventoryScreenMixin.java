package dev.transparentinventory.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.transparentinventory.client.render.GlassInventoryRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Replace only creative chrome; vanilla/Fabric still render tab icons, search,
 * pagination and scrolling, and still own every menu operation. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {
    @Shadow private static CreativeModeTab selectedTab;

    @WrapOperation(method = "extractBackground", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void spatial$glass(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier texture,
                               int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight,
                               Operation<Void> original) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (GlassInventoryRenderer.active(self)) GlassInventoryRenderer.draw(g, self);
        else original.call(g, pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    @WrapOperation(method = "extractTabButton", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void spatial$tab(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite,
                             int x, int y, int width, int height, Operation<Void> original,
                             @Local(argsOnly = true) CreativeModeTab tab) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (GlassInventoryRenderer.active(self)) GlassInventoryRenderer.control(g, self, x, y, width, height, tab == selectedTab);
        else original.call(g, pipeline, sprite, x, y, width, height);
    }

    @WrapOperation(method = "extractBackground", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void spatial$scrollbar(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite,
                                   int x, int y, int width, int height, Operation<Void> original) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (GlassInventoryRenderer.active(self)) GlassInventoryRenderer.control(g, self, x, y, width, height, true);
        else original.call(g, pipeline, sprite, x, y, width, height);
    }

    @WrapOperation(method = "extractLabels", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    private void spatial$label(GuiGraphicsExtractor g, Font font, Component title, int x, int y,
                               int color, boolean shadow, Operation<Void> original) {
        boolean glass = GlassInventoryRenderer.active((CreativeModeInventoryScreen) (Object) this);
        original.call(g, font, title, x, y, glass ? 0xFFF0F8FF : color, glass || shadow);
    }

    @WrapOperation(method = "extractBackground", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"))
    private void spatial$singlePlayer(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size,
                                      float offset, float mouseX, float mouseY, LivingEntity entity, Operation<Void> original) {
        if (!GlassInventoryRenderer.active((CreativeModeInventoryScreen) (Object) this) || !GlassInventoryRenderer.outsidePlayer()) {
            original.call(g, x0, y0, x1, y1, size, offset, mouseX, mouseY, entity);
        }
    }
}
