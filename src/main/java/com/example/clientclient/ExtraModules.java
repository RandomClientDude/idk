package com.example.clientclient;

import com.example.clientclient.Module.Category;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public final class ExtraModules {
    public static long lastHit, comboTime;
    public static int combo, hits;
    public static final ArrayDeque<Long> CLICKS = new ArrayDeque<>();
    public static final int[] FPS = new int[60];
    public static int fpsIdx;
    public static double bps, dist;
    private static boolean havePos;
    private static double lastX, lastZ;
    private static boolean prevAttack;
    private static final int NONE = GLFW.GLFW_KEY_UNKNOWN;

    private ExtraModules() {}

    public static void onAttack(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        lastHit = now;
        hits++;
        if (now - comboTime < 2000) combo++; else combo = 1;
        comboTime = now;
        if (mc.world != null && ModuleManager.on("HitParticles")) {
            for (int i = 0; i < 12; i++) {
                ParticleEffect pe = (i % 2 == 0) ? ParticleTypes.CRIT : new DustParticleEffect(Colors.accent(i * 30) & 0xFFFFFF, 1.4f);
                Compat.particle(mc, pe,
                        target.getX() + (Math.random() - 0.5) * 0.7,
                        target.getY() + target.getHeight() * 0.5 + (Math.random() - 0.5) * 0.7,
                        target.getZ() + (Math.random() - 0.5) * 0.7,
                        (Math.random() - 0.5) * 0.3, Math.random() * 0.2, (Math.random() - 0.5) * 0.3);
            }
        }
    }

    public static void globalTick(MinecraftClient mc) {
        long ms = System.currentTimeMillis();
        boolean now = mc.options.attackKey.isPressed();
        if (now && !prevAttack) CLICKS.add(ms);
        prevAttack = now;
        while (!CLICKS.isEmpty() && ms - CLICKS.peekFirst() > 1000) CLICKS.pollFirst();
        bps = havePos ? Math.hypot(mc.player.getX() - lastX, mc.player.getZ() - lastZ) * 20 : 0;
        if (bps > 200) bps = 0;
        dist += bps / 20.0;
        havePos = true;
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
        FPS[fpsIdx] = mc.getCurrentFps();
        fpsIdx = (fpsIdx + 1) % FPS.length;
    }

    private static LivingEntity nearest(MinecraftClient mc, double range, boolean playersToo) {
        LivingEntity best = null;
        double bestD = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive()) continue;
            if (!(e instanceof Monster) && !(playersToo && e instanceof PlayerEntity)) continue;
            double d = mc.player.squaredDistanceTo(e);
            if (d < bestD) { bestD = d; best = le; }
        }
        return best;
    }

    static void init() {
        // ---------------- COMBAT ----------------
        ModuleManager.add(new Module("KillAura", "Attacks the nearest player or mob in range", NONE, Category.COMBAT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player.getAttackCooldownProgress(0f) < 1f) return;
                LivingEntity t = nearest(mc, 4.2, true);
                if (t != null) {
                    mc.interactionManager.attackEntity(mc.player, t);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        });

        ModuleManager.add(new Module("AutoClicker", "Hold attack to hit at full cooldown", NONE, Category.COMBAT) {
            @Override public void onTick(MinecraftClient mc) {
                if (!mc.options.attackKey.isPressed() || mc.player.getAttackCooldownProgress(0f) < 1f) return;
                if (mc.crosshairTarget instanceof EntityHitResult hit) {
                    mc.interactionManager.attackEntity(mc.player, hit.getEntity());
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        });

        ModuleManager.add(new Module("AimAssist", "Gently steers your aim toward targets", NONE, Category.COMBAT) {
            @Override public void onTick(MinecraftClient mc) {
                LivingEntity t = nearest(mc, 6.0, true);
                if (t == null) return;
                Vec3d d = t.getEyePos().subtract(mc.player.getEyePos());
                float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
                float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
                float dy = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
                if (Math.abs(dy) > 70f) return;
                mc.player.setYaw(mc.player.getYaw() + dy * 0.15f);
                mc.player.setPitch(mc.player.getPitch() + (pitch - mc.player.getPitch()) * 0.15f);
            }
        });

        ModuleManager.add(new Module("HitParticles", "Colored sparks when you hit something", NONE, Category.COMBAT) {});

        // ---------------- MOVEMENT / PLAYER ----------------
        ModuleManager.add(new Module("AirJump", "Jump again in mid-air", NONE, Category.MOVEMENT) {
            private int cd;
            @Override public void onTick(MinecraftClient mc) {
                if (cd > 0) cd--;
                if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && cd == 0 && !mc.player.getAbilities().flying) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x, 0.42, v.z);
                    cd = 10;
                }
            }
        });

        ModuleManager.add(new Module("AutoTool", "Switches to the fastest tool while mining", NONE, Category.PLAYER) {
            @Override public void onTick(MinecraftClient mc) {
                if (!mc.options.attackKey.isPressed() || !(mc.crosshairTarget instanceof BlockHitResult bhr)) return;
                BlockState st = mc.world.getBlockState(bhr.getBlockPos());
                int best = -1;
                float bestSpeed = 1f;
                for (int i = 0; i < 9; i++) {
                    float sp = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(st);
                    if (sp > bestSpeed) { bestSpeed = sp; best = i; }
                }
                if (best >= 0) Compat.setSlot(mc, best);
            }
        });

        // ---------------- HUD (drawn in Hud2.java) ----------------
        for (String[] s : new String[][]{
                {"CPS", "Clicks per second"}, {"Ping", "Your latency"}, {"Clock", "Real-world time"},
                {"FpsGraph", "Live FPS graph"}, {"InventoryHUD", "Your inventory on screen"},
                {"Totems", "Totem counter"}, {"Hitmarker", "X marker when you land a hit"},
                {"ComboCounter", "Consecutive hit counter"}, {"Fireflies", "Glowing fireflies over the screen"},
                {"MatrixRain", "Falling code overlay"}}) {
            ModuleManager.add(new Module(s[0], s[1], NONE, Category.HUD) {});
        }
    }
}
