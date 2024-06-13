package de.fanta.stats.mixin;

import de.fanta.stats.client.GUI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
    @Unique
    private static GUI statsGUI;

    @Inject(method="renderMainHud", at=@At(value = "RETURN"))
    private void renderOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (statsGUI == null) {
            statsGUI = new GUI();
        }
        statsGUI.onRenderGameOverlayPost(context);
    }
}
