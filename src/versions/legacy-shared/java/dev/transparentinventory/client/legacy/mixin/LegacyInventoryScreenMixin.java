package dev.transparentinventory.client.legacy.mixin;

import dev.transparentinventory.client.InventoryConfig;
import dev.transparentinventory.client.legacy.LegacyEntityPreview;
import dev.transparentinventory.client.legacy.LegacyScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Legacy (2D) entrance for the player inventory: the panel starts off to the left and
 * slams back into its resting spot, the vanilla texture gives way to a translucent panel,
 * and the player model arrives from the right.
 *
 * <p>Panel and slots both ride on {@code leftPos}, which is re-derived every frame from
 * the resting position, so hit-testing follows the animation and no version-specific
 * render chain has to be intercepted. The vanilla texture and its embedded player preview
 * are suppressed by short-circuiting {@code renderBg}, which exists with the same shape
 * on every supported release.
 */
@Mixin(InventoryScreen.class)
public abstract class LegacyInventoryScreenMixin {
    @Unique private int legacy$restingLeft;
    @Unique private boolean legacy$resting;

    @Inject(method = "init", at = @At("TAIL"))
    private void legacy$onInit(CallbackInfo ci) {
        LegacyContainerScreenAccessor access = (LegacyContainerScreenAccessor) (Object) this;
        this.legacy$restingLeft = access.legacy$leftPos();
        this.legacy$resting = true;
        LegacyScreenManager.INSTANCE.reset();
    }

    /**
     * Re-derives {@code leftPos} from the resting position for the current frame.
     *
     * <p>Called from both {@code render} and {@code renderBg} because the two arrive in
     * opposite orders depending on the release: 1.20.1 draws the background from inside
     * {@code render}, while 1.21.x draws it from {@code renderBackground} beforehand.
     * The call is idempotent, so whichever comes first wins and the other is a no-op.
     */
    @Unique
    private void legacy$updateSlide() {
        LegacyContainerScreenAccessor access = (LegacyContainerScreenAccessor) (Object) this;
        LegacyScreenManager manager = LegacyScreenManager.INSTANCE;
        if (!manager.active() || !this.legacy$resting) {
            // Fresh screen or resize: leftPos has just been re-centred, so remember that spot.
            this.legacy$restingLeft = access.legacy$leftPos();
            this.legacy$resting = true;
        }
        manager.open(this.legacy$restingLeft + access.legacy$imageWidth() + 24);
        access.legacy$setLeftPos(this.legacy$restingLeft - (int) Math.round(manager.travel()));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void legacy$slideIn(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        legacy$updateSlide();
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void legacy$glassPanel(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        ci.cancel();
        legacy$updateSlide();
        LegacyContainerScreenAccessor access = (LegacyContainerScreenAccessor) (Object) this;
        InventoryScreen self = (InventoryScreen) (Object) this;
        InventoryConfig config = InventoryConfig.get();
        LegacyScreenManager manager = LegacyScreenManager.INSTANCE;

        int x0 = access.legacy$leftPos();
        int y0 = access.legacy$topPos();
        int x1 = x0 + access.legacy$imageWidth();
        int y1 = y0 + access.legacy$imageHeight();
        float visibility = (float) manager.progress();

        int panel = alpha(config.panelOpacity, visibility);
        int border = alpha(config.borderOpacity, visibility);
        int slotFill = alpha(config.slotOpacity, visibility);
        int slotEdge = alpha(config.slotBorderOpacity, visibility);

        // Two bands fake a soft vertical gradient without depending on fillGradient,
        // whose signature moved around between these releases.
        int split = y0 + access.legacy$imageHeight() / 3;
        graphics.fill(x0, y0, x1, split, panel | 0x141924);
        graphics.fill(x0, split, x1, y1, panel | 0x0A0D14);
        graphics.fill(x0, y0, x1, y0 + 1, border | 0xFFFFFF);
        graphics.fill(x0, y1 - 1, x1, y1, border | 0xFFFFFF);
        graphics.fill(x0, y0, x0 + 1, y1, border | 0xFFFFFF);
        graphics.fill(x1 - 1, y0, x1, y1, border | 0xFFFFFF);

        for (Slot slot : self.getMenu().slots) {
            int sx = x0 + slot.x;
            int sy = y0 + slot.y;
            graphics.fill(sx, sy, sx + 16, sy + 16, slotFill | 0xFFFFFF);
            graphics.fill(sx, sy, sx + 16, sy + 1, slotEdge | 0xFFFFFF);
            graphics.fill(sx, sy + 15, sx + 16, sy + 16, slotEdge | 0xFFFFFF);
            graphics.fill(sx, sy, sx + 1, sy + 16, slotEdge | 0xFFFFFF);
            graphics.fill(sx + 15, sy, sx + 16, sy + 16, slotEdge | 0xFFFFFF);
        }

        LivingEntity player = Minecraft.getInstance().player;
        if (config.playerPreview && player != null) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            double p = manager.progress();
            int playerX = (int) Math.round(screenWidth * config.playerCenterXRatio
                    + (1 - p) * screenWidth * config.playerSlideRatio);
            int playerY = (int) Math.round(screenHeight * config.playerCenterYRatio);
            int height = Math.max(20, (int) Math.round(screenHeight * config.playerHeightRatio));
            LegacyEntityPreview.draw(graphics, playerX, playerY, height, mouseX, mouseY, player);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void legacy$landingFlash(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        LegacyScreenManager manager = LegacyScreenManager.INSTANCE;
        double impact = manager.impact();
        if (impact <= 0.02) return;
        LegacyContainerScreenAccessor access = (LegacyContainerScreenAccessor) (Object) this;
        int x = access.legacy$leftPos() + access.legacy$imageWidth();
        int top = access.legacy$topPos();
        int bottom = top + access.legacy$imageHeight();
        int glow = (int) (impact * 60);
        int core = (int) (impact * 140);
        graphics.fill(x - 14, top, x + 14, bottom, (glow << 24) | 0xFFFFFF);
        graphics.fill(x - 2, top, x + 2, bottom, (core << 24) | 0xFFFFFF);
    }

    private static int alpha(float opacity, float visibility) {
        return ((int) (opacity * 255 * visibility) & 0xFF) << 24;
    }
}
