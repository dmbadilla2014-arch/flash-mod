package com.flashmod.mixin;

import com.flashmod.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Con tanta velocidad el FOV se deforma; lo limitamos mientras llevas el traje. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void flashmod$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !ModItems.hasSuit(mc.player)) return;
        double max = mc.options.getFov().getValue() + 25.0;
        if (cir.getReturnValue() > max) cir.setReturnValue(max);
    }
}
