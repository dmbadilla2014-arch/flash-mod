package com.flashmod;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Toda la logica del velocista (servidor). El nivel de velocista = nivel de XP de Minecraft. */
public final class FlashLogic {
    private static final class St {
        float stam = 100;
        boolean slow, tired;
        double xpAcc, lastMult = 1;
        int tick;
    }

    private static final Map<UUID, St> STATE = new HashMap<>();
    private static UUID slowOwner = null;
    private static final Identifier ATTR = Identifier.of(FlashMod.ID, "speed_force");
    private static final DustParticleEffect YELLOW = new DustParticleEffect(new Vector3f(1.0f, 0.85f, 0.1f), 1.3f);

    public static final int MAX_LEVEL = 100;
    public static int maxStam(int l) { return 100 + l * 6; }
    public static double sprintBonus(int l) { return Math.min(1.0 + l * 0.15, 12.0); }   // multiplicador extra al correr
    public static float slowRate(int l) { return Math.max(2f, 12f - l * 0.25f); }          // ticks/seg del mundo con C
    public static int phaseDist(int l) { return Math.min(4 + l / 3, 24); }

    private FlashLogic() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(FlashLogic::tick);

        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            if (payload.action() == 0) toggleSlow(p); else vibrate(p);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            endSlow(server, id);
            STATE.remove(id);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            slowOwner = null;
            STATE.clear();
            server.getTickManager().setTickRate(20f);
        });

        // El traje te protege de las caidas.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity p && ModItems.hasSuit(p) && source.isIn(DamageTypeTags.IS_FALL)));
    }

    private static void tick(MinecraftServer server) {
        float rate = server.getTickManager().getTickRate();
        float dt = 20f / Math.max(rate, 1f);   // cada tick dura "dt" ticks normales: asi los costes van en tiempo real

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            St s = STATE.computeIfAbsent(p.getUuid(), k -> new St());
            s.tick++;
            int lvl = Math.min(p.experienceLevel, MAX_LEVEL);

            if (!ModItems.hasSuit(p)) {
                if (s.slow) endSlow(server, p.getUuid());
                applyMult(p, s, 1.0);
                continue;
            }

            float max = maxStam(lvl);
            if (s.stam > max) s.stam = max;
            boolean sprint = p.isSprinting();
            boolean run = sprint && !s.tired && s.stam > 0;
            double mult = 1.0;

            if (run) {
                s.stam -= 0.5f * dt;
                mult *= 1.0 + sprintBonus(lvl);
                trail(p);
                s.xpAcc += dt;
                if (s.xpAcc >= 20) {            // ~1 vez por segundo real
                    s.xpAcc -= 20;
                    if (p.experienceLevel < MAX_LEVEL) p.addExperience(4 + lvl / 5);
                }
            } else if (!s.slow) {
                s.stam = Math.min(max, s.stam + 0.35f * dt);
            }

            if (s.slow) {
                s.stam -= 0.25f * dt;
                mult *= 20.0 / rate;             // compensa para que TU no te veas lento
            }

            if (s.stam <= 0) {
                s.stam = 0;
                if (!s.tired) p.sendMessage(Text.literal("§cTe quedaste sin stamina"), true);
                s.tired = true;
                if (s.slow) endSlow(server, p.getUuid());
            }
            if (s.tired && s.stam >= 25) s.tired = false;

            applyMult(p, s, mult);

            if (s.tick % 2 == 0)
                ServerPlayNetworking.send(p, new StatePayload(Math.round(s.stam), (int) max, s.slow, s.tired));
        }
    }

    private static void applyMult(ServerPlayerEntity p, St s, double mult) {
        EntityAttributeInstance inst = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (inst == null) return;
        boolean want = mult > 1.0001;
        if (Math.abs(mult - s.lastMult) < 0.01 && inst.hasModifier(ATTR) == want) return;
        inst.removeModifier(ATTR);
        if (want)
            inst.addTemporaryModifier(new EntityAttributeModifier(ATTR, mult - 1.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        s.lastMult = mult;
    }

    private static void trail(ServerPlayerEntity p) {
        ServerWorld w = (ServerWorld) p.getWorld();
        Vec3d look = p.getRotationVector();
        double hx = look.x, hz = look.z, n = Math.hypot(hx, hz);
        if (n > 1e-4) { hx /= n; hz /= n; }
        for (int k = 1; k <= 3; k++)
            w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() - hx * k * 0.9, p.getY() + 0.9, p.getZ() - hz * k * 0.9,
                    4, 0.25, 0.65, 0.25, 0.08);
        w.spawnParticles(YELLOW, p.getX() - hx * 0.6, p.getY() + 0.9, p.getZ() - hz * 0.6, 8, 0.3, 0.7, 0.3, 0.0);
    }

    // ---------- C: tiempo lento ----------
    private static void toggleSlow(ServerPlayerEntity p) {
        if (!ModItems.hasSuit(p)) { p.sendMessage(Text.literal("§eNecesitas el traje completo de Flash"), true); return; }
        MinecraftServer server = p.getServer();
        St s = STATE.computeIfAbsent(p.getUuid(), k -> new St());
        if (s.slow) { endSlow(server, p.getUuid()); soundAt(p, false); return; }
        if (slowOwner != null) { p.sendMessage(Text.literal("§eOtro velocista ya tiene el tiempo detenido"), true); return; }
        if (s.tired || s.stam < 15) { p.sendMessage(Text.literal("§cSin stamina"), true); return; }
        int lvl = Math.min(p.experienceLevel, MAX_LEVEL);
        s.slow = true;
        slowOwner = p.getUuid();
        server.getTickManager().setTickRate(slowRate(lvl));
        soundAt(p, true);
        p.sendMessage(Text.literal("§b⚡ Speed Force: el tiempo se ralentiza"), true);
    }

    private static void endSlow(MinecraftServer server, UUID id) {
        St s = STATE.get(id);
        if (s != null) s.slow = false;
        if (id.equals(slowOwner)) {
            slowOwner = null;
            if (server != null) server.getTickManager().setTickRate(20f);
        }
    }

    private static void soundAt(ServerPlayerEntity p, boolean on) {
        p.getWorld().playSound(null, p.getBlockPos(),
                on ? SoundEvents.BLOCK_BEACON_ACTIVATE : SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.PLAYERS, 1f, 1f);
    }

    // ---------- V: vibrar a traves de paredes ----------
    private static void vibrate(ServerPlayerEntity p) {
        if (!ModItems.hasSuit(p)) return;
        St s = STATE.computeIfAbsent(p.getUuid(), k -> new St());
        int lvl = Math.min(p.experienceLevel, MAX_LEVEL);
        if (lvl < 10) { p.sendMessage(Text.literal("§eNecesitas nivel 10 para vibrar a través de paredes"), true); return; }
        if (s.tired || s.stam < 30) { p.sendMessage(Text.literal("§cSin stamina"), true); return; }

        World w = p.getWorld();
        Vec3d look = p.getRotationVector();
        double hx = look.x, hz = look.z, n = Math.hypot(hx, hz);
        if (n < 1e-4) return;
        hx /= n; hz /= n;
        for (int d = phaseDist(lvl); d >= 2; d--) {
            BlockPos feet = BlockPos.ofFloored(p.getX() + hx * d, p.getY(), p.getZ() + hz * d);
            if (!w.isChunkLoaded(feet)) continue;
            if (free(w, feet) && free(w, feet.up())) {
                ServerWorld sw = (ServerWorld) w;
                for (int i = 0; i <= 12; i++)
                    sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + hx * d * i / 12.0, p.getY() + 1,
                            p.getZ() + hz * d * i / 12.0, 3, 0.2, 0.5, 0.2, 0.05);
                p.requestTeleport(feet.getX() + 0.5, p.getY(), feet.getZ() + 0.5);
                w.playSound(null, feet, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1.6f);
                s.stam -= 30;
                return;
            }
        }
        p.sendMessage(Text.literal("§7No hay espacio libre al otro lado"), true);
    }

    private static boolean free(World w, BlockPos pos) {
        return w.getBlockState(pos).getCollisionShape(w, pos).isEmpty() && w.getFluidState(pos).isEmpty();
    }
}
