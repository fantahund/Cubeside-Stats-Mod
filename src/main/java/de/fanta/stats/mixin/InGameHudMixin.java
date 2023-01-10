package de.fanta.stats.mixin;

import de.fanta.stats.client.GUI;
import de.fanta.stats.client.StatsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
    private static GUI statsGUI;

    @Inject(method="render", at=@At(value="FIELD", target="Lnet/minecraft/client/option/GameOptions;debugEnabled:Z", opcode = Opcodes.GETFIELD, args = {"log=false"}))
    private void beforeRenderDebugScreen2(MatrixStack stack, float f, CallbackInfo ci) {
        if (statsGUI != null && !GUI.updater.isAlive()) {
            statsGUI = null;
        }

        if (statsGUI == null) {
            statsGUI = new GUI();
        }
        statsGUI.onRenderGameOverlayPost(stack);
    }
}
