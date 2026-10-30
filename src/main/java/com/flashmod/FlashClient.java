package com.flashmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class FlashClient implements ClientModInitializer {
    private static KeyBinding slowKey, vibrateKey;
    private static int sprintGrace = 0, climbTicks = 0;

    @Override
    public void onInitialize() {
        slowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flashmod.slowmo", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.flashmod"));
        vibrateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flashmod.vibrate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.flashmod"));

        ClientPlayNetworking.registerGlobalReceiver(StatePayload.ID, (payload, ctx) -> ClientState.set(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (slowKey.wasPressed()) ClientPlayNetworking.send(new ActionPayload(0));
            while (vibrateKey.wasPressed()) ClientPlayNetworking.send(new ActionPayload(1));
            movement(mc);
        });
        HudRenderCallback.EVENT.register(FlashClient::hud);
    }

    /** Correr por paredes y por agua (el cliente manda sobre su propio movimiento). */
    private static void movement(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || !ModItems.hasSuit(p) || !ClientState.received || ClientState.tired || ClientState.stam <= 0) return;
        int lvl = p.experienceLevel;
        Vec3d v = p.getVelocity();

        if (p.isSprinting()) sprintGrace = 8; else if (sprintGrace > 0) sprintGrace--;
        if (p.isOnGround()) climbTicks = 0;

        // Pared: al correr contra un muro, subes por el
        if (lvl >= 8 && sprintGrace > 0 && p.horizontalCollision && mc.options.forwardKey.isPressed()
                && climbTicks < 40 + lvl * 2) {
            p.setVelocity(v.x, 0.42, v.z);
            sprintGrace = 8;
            climbTicks++;
        }
        // Agua: a nivel 5 corres sobre la superficie
        if (lvl >= 5 && sprintGrace > 0) {
            double h = p.getFluidHeight(FluidTags.WATER);
            if (h > 0 && h < 1.0) {
                p.setVelocity(v.x, MathHelper.clamp((0.2 - h) * 0.8, -0.15, 0.25), v.z);
                p.fallDistance = 0;
            }
        }
    }

    private static void hud(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || !ClientState.received || !ModItems.hasSuit(mc.player)) return;
        int lvl = Math.min(mc.player.experienceLevel, FlashLogic.MAX_LEVEL);
        int x = 10, y = 10, wBar = 100;

        if (ClientState.slow)
            ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), 0x2200CCFF);

        ctx.fill(x - 1, y - 1, x + wBar + 1, y + 7, 0xAA000000);
        int filled = (int) (wBar * (float) ClientState.stam / ClientState.max);
        ctx.fill(x, y, x + filled, y + 6, ClientState.tired ? 0xFFFF4444 : 0xFFFFD21A);
        String info = String.format("§c⚡ Nivel %d  §fStamina %d/%d  §bVel x%.1f%s", lvl, ClientState.stam, ClientState.max,
                1.0 + FlashLogic.sprintBonus(lvl), ClientState.slow ? "  §3[TIEMPO LENTO]" : "");
        ctx.drawTextWithShadow(mc.textRenderer, info, x, y + 10, 0xFFFFFF);
    }
}
