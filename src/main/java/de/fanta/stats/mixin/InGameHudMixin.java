package de.fanta.stats.mixin;

import de.fanta.stats.client.GUI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
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
    private void renderOverlay(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (statsGUI != null && !GUI.updater.isAlive()) {
            statsGUI = null;
        }

        if (statsGUI == null) {
            statsGUI = new GUI();
        }
        statsGUI.onRenderGameOverlayPost(context);
    }
}
