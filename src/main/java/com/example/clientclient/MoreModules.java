package com.example.clientclient;

import com.example.clientclient.Module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public final class MoreModules {
    private static final int NONE = GLFW.GLFW_KEY_UNKNOWN;

    private MoreModules() {}

    /** A module assembled from small lambdas, so dozens of modules stay short. */
    static final class Tick extends Module {
        private final Consumer<MinecraftClient> tick, on, off;

        Tick(String name, String desc, Category cat, Consumer<MinecraftClient> tick,
             Consumer<MinecraftClient> on, Consumer<MinecraftClient> off) {
            super(name, desc, NONE, cat);
            this.tick = tick;
            this.on = on;
            this.off = off;
        }

        @Override public void onTick(MinecraftClient mc) { if (tick != null) tick.accept(mc); }
        @Override protected void onEnable(MinecraftClient mc) { if (on != null) on.accept(mc); }
        @Override protected void onDisable(MinecraftClient mc) { if (off != null) off.accept(mc); }
    }

    /** Particle patterns drawn around the player. */
    static final class Fx {
        private Fx() {}

        private static double r() { return Math.random() - 0.5; }

        private static void put(MinecraftClient mc, ParticleEffect base, double x, double y, double z, double vx, double vy, double vz) {
            ParticleEffect e = base != null ? base
                    : new DustParticleEffect(Colors.accent((int) (mc.player.age * 7 + x * 30)) & 0xFFFFFF, 1.3f);
            Compat.particle(mc, e, x, y, z, vx, vy, vz);
        }

        static void emit(MinecraftClient mc, String pattern, ParticleEffect base) {
            var p = mc.player;
            double x = p.getX(), y = p.getY(), z = p.getZ();
            int age = p.age;
            double yaw = Math.toRadians(p.getYaw());
            double sin = Math.sin(yaw), cos = Math.cos(yaw);
            switch (pattern) {
                case "trail": {
                    Vec3d v = p.getVelocity();
                    if (Math.hypot(v.x, v.z) < 0.04) return;
                    for (int i = 0; i < 2; i++) put(mc, base, x + r() * 0.4, y + 0.1 + Math.random() * 0.3, z + r() * 0.4, 0, 0, 0);
                    break;
                }
                case "aura": {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.random() * Math.PI * 2;
                        put(mc, base, x + Math.cos(a) * 0.7, y + Math.random() * 1.8, z + Math.sin(a) * 0.7, 0, 0.02, 0);
                    }
                    break;
                }
                case "halo": {
                    for (int k = 0; k < 6; k++) {
                        double a = age * 0.2 + k * Math.PI / 3;
                        put(mc, base, x + Math.cos(a) * 0.4, y + p.getHeight() + 0.3, z + Math.sin(a) * 0.4, 0, 0, 0);
                    }
                    break;
                }
                case "wings": {
                    double rx = -cos, rz = -sin, bx = sin, bz = -cos;
                    double flap = Math.sin(age * 0.15) * 0.2;
                    for (int i = 0; i < 3; i++) {
                        double t = Math.random();
                        double side = 0.2 + t * 0.9;
                        double h = 1.0 + Math.sin(t * Math.PI) * 0.5 + flap * t;
                        double behind = 0.25 + t * 0.3;
                        for (int s = -1; s <= 1; s += 2)
                            put(mc, base, x + bx * behind + rx * side * s, y + h, z + bz * behind + rz * side * s, 0, 0, 0);
                    }
                    break;
                }
                case "spiral": {
                    double a = age * 0.35;
                    double h = (age % 24) / 12.0;
                    for (int k = 0; k < 2; k++) {
                        double aa = a + k * Math.PI;
                        put(mc, base, x + Math.cos(aa) * 0.65, y + h, z + Math.sin(aa) * 0.65, 0, 0, 0);
                    }
                    break;
                }
                case "rain": {
                    for (int i = 0; i < 3; i++) put(mc, base, x + r() * 1.8, y + 2.8, z + r() * 1.8, 0, -0.15, 0);
                    break;
                }
                case "fountain": {
                    for (int i = 0; i < 3; i++) put(mc, base, x, y + 0.1, z, r() * 0.12, 0.3 + Math.random() * 0.2, r() * 0.12);
                    break;
                }
                case "ring": {
                    double rad = 0.3 + (age % 20) * 0.08;
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4 + age * 0.05;
                        put(mc, base, x + Math.cos(a) * rad, y + 0.1, z + Math.sin(a) * rad, 0, 0, 0);
                    }
                    break;
                }
                case "orbit": {
                    double b0 = age * 0.15;
                    for (int k = 0; k < 3; k++) {
                        double a = b0 + k * Math.PI * 2 / 3;
                        put(mc, base, x + Math.cos(a) * 1.1, y + 1.0 + Math.sin(b0 * 0.7 + k) * 0.5, z + Math.sin(a) * 1.1, 0, 0, 0);
                    }
                    break;
                }
                case "sphere": {
                    for (int k = 0; k < 3; k++) {
                        double th = Math.random() * Math.PI * 2, ph = Math.acos(2 * Math.random() - 1);
                        put(mc, base, x + 1.2 * Math.sin(ph) * Math.cos(th), y + 1.0 + 1.2 * Math.cos(ph), z + 1.2 * Math.sin(ph) * Math.sin(th), 0, 0, 0);
                    }
                    break;
                }
                case "pillar": {
                    for (int k = 0; k < 6; k++) put(mc, base, x + r() * 0.2, y + k * 0.5 + Math.random() * 0.4, z + r() * 0.2, 0, 0, 0);
                    break;
                }
                case "tornado": {
                    for (int k = 0; k < 5; k++) {
                        double hh = 0.2 + k * 0.4;
                        double a = age * 0.4 + hh * 1.2;
                        double rad = 0.3 + hh * 0.25;
                        put(mc, base, x + Math.cos(a) * rad, y + hh, z + Math.sin(a) * rad, 0, 0, 0);
                    }
                    break;
                }
                case "flower": {
                    double a0 = age * 0.1;
                    for (int j = 0; j < 12; j++) {
                        double tt = j * Math.PI / 6;
                        double rr = 0.8 * Math.abs(Math.cos(2.5 * tt + a0));
                        put(mc, base, x + Math.cos(tt) * rr, y + 0.1, z + Math.sin(tt) * rr, 0, 0, 0);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    static void reg(Module m) { ModuleManager.addNoKey(m); }

    static void t(String n, String d, Category c, Consumer<MinecraftClient> tick) {
        reg(new Tick(n, d, c, tick, null, null));
    }

    static void full(String n, String d, Category c, Consumer<MinecraftClient> tick,
                             Consumer<MinecraftClient> on, Consumer<MinecraftClient> off) {
        reg(new Tick(n, d, c, tick, on, off));
    }

    static void flag(String n, String d, Category c) { reg(new Tick(n, d, c, null, null, null)); }

    static void fx(String n, String d, ParticleEffect base, String pattern) {
        reg(new Tick(n, d, Category.PARTICLES, mc -> Fx.emit(mc, pattern, base), null, null));
    }

    static void rainbow(String n, String d, String pattern) {
        reg(new Tick(n, d, Category.PARTICLES, mc -> Fx.emit(mc, pattern, null), null, null));
    }

    static void potion(String n, String d, RegistryEntry<StatusEffect> eff, int amp) {
        reg(new Tick(n, d, Category.PLAYER,
                mc -> mc.player.addStatusEffect(new StatusEffectInstance(eff, 40, amp, false, false)),
                null, mc -> mc.player.removeStatusEffect(eff)));
    }

    static void fov(String n, String d, int value) {
        int[] old = {70};
        full(n, d, Category.RENDER, null,
                mc -> { old[0] = mc.options.getFov().getValue(); mc.options.getFov().setValue(value); },
                mc -> mc.options.getFov().setValue(old[0]));
    }

    static void init() {
        // ---------------- HUD toggles ----------------
        flag("Watermark", "ClientClient logo and stats (top-left)", Category.HUD);
        flag("ArrayList", "List of enabled modules (top-right)", Category.HUD);

        // ---------------- PARTICLES (35) ----------------
        fx("FlameTrail", "Fire behind your feet", ParticleTypes.FLAME, "trail");
        fx("HeartAura", "Hearts floating around you", ParticleTypes.HEART, "aura");
        fx("NoteHalo", "Music notes circle your head", ParticleTypes.NOTE, "halo");
        fx("CloudWalk", "Walk on a trail of clouds", ParticleTypes.CLOUD, "trail");
        fx("SnowAura", "A swirl of snowflakes", ParticleTypes.SNOWFLAKE, "aura");
        fx("SoulTrail", "Blue soul fire trail", ParticleTypes.SOUL_FIRE_FLAME, "trail");
        fx("MagicSpiral", "Enchanting table runes spiral up", ParticleTypes.ENCHANT, "spiral");
        fx("StarCrown", "A crown of end rod sparks", ParticleTypes.END_ROD, "halo");
        fx("EmeraldAura", "Green villager sparkles", ParticleTypes.HAPPY_VILLAGER, "aura");
        fx("PortalTrail", "Purple portal dust behind you", ParticleTypes.PORTAL, "trail");
        fx("DragonWings", "Wings of dragon breath", ParticleTypes.DRAGON_BREATH, "wings");
        fx("WitchSpiral", "A purple witch spiral", ParticleTypes.WITCH, "spiral");
        fx("CritAura", "Critical hit sparks around you", ParticleTypes.CRIT, "aura");
        fx("SparkTrail", "Electric sparks behind you", ParticleTypes.ELECTRIC_SPARK, "trail");
        fx("SmokeTrail", "A trail of smoke", ParticleTypes.SMOKE, "trail");
        fx("LavaFountain", "Lava shoots up from your feet", ParticleTypes.LAVA, "fountain");
        fx("SplashAura", "Water splashes around you", ParticleTypes.SPLASH, "aura");
        fx("FireworkRain", "Firework sparks fall on you", ParticleTypes.FIREWORK, "rain");
        fx("TotemBurst", "Totem sparkles burst upward", ParticleTypes.TOTEM_OF_UNDYING, "fountain");
        fx("AngryAura", "Angry villager clouds", ParticleTypes.ANGRY_VILLAGER, "aura");
        fx("SakuraRain", "Cherry petals fall on you", ParticleTypes.CHERRY_LEAVES, "rain");
        fx("GlowSpiral", "A glowing spiral", ParticleTypes.GLOW, "spiral");
        fx("SoulWings", "Wings of drifting souls", ParticleTypes.SOUL, "wings");
        fx("FlameWings", "Wings of fire", ParticleTypes.FLAME, "wings");
        fx("SnowWings", "Wings of snowflakes", ParticleTypes.SNOWFLAKE, "wings");
        fx("HeartRain", "Hearts rain down", ParticleTypes.HEART, "rain");
        fx("NoteSpiral", "A spiral of music notes", ParticleTypes.NOTE, "spiral");
        fx("CloudHalo", "A cloud ring over your head", ParticleTypes.CLOUD, "halo");
        fx("EnderAura", "Portal particles swirl around you", ParticleTypes.PORTAL, "aura");
        rainbow("RainbowSpiral", "Theme-colored spiral", "spiral");
        rainbow("RainbowWings", "Theme-colored wings", "wings");
        rainbow("RainbowAura", "Theme-colored aura", "aura");
        rainbow("RainbowRain", "Theme-colored rain", "rain");
        rainbow("RainbowFountain", "Theme-colored fountain", "fountain");
        rainbow("RainbowCrown", "Theme-colored crown", "halo");
        rainbow("RainbowWalk", "Theme-colored footsteps", "trail");

        // ---------------- MOVEMENT (10) ----------------
        t("LowGravity", "Fall slowly", Category.MOVEMENT, mc -> {
            Vec3d v = mc.player.getVelocity();
            if (!mc.player.isOnGround() && v.y < 0) mc.player.setVelocity(v.x, v.y * 0.7, v.z);
        });
        t("LongJump", "Jumps carry you far forward", Category.MOVEMENT, mc -> {
            if (mc.options.jumpKey.isPressed() && mc.player.isOnGround() && Compat.moving(mc)) {
                double yaw = Math.toRadians(mc.player.getYaw());
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x - Math.sin(yaw) * 0.6, 0.5, v.z + Math.cos(yaw) * 0.6);
            }
        });
        double[] lastFall = {0};
        t("Bounce", "Bounce when you land", Category.MOVEMENT, mc -> {
            Vec3d v = mc.player.getVelocity();
            if (!mc.player.isOnGround()) lastFall[0] = v.y;
            else {
                if (lastFall[0] < -0.45) mc.player.setVelocity(v.x, Math.min(-lastFall[0] * 0.8, 1.2), v.z);
                lastFall[0] = 0;
            }
        });
        t("Speed2", "Much faster ground movement", Category.MOVEMENT, mc -> {
            if (mc.player.isOnGround() && Compat.moving(mc)) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * 1.3, v.y, v.z * 1.3);
            }
        });
        t("Anchor", "Stop instantly when you let go of the keys", Category.MOVEMENT, mc -> {
            if (mc.player.isOnGround() && !Compat.moving(mc)) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(0, v.y, 0);
            }
        });
        t("Hover", "Hold sneak in the air to hover", Category.MOVEMENT, mc -> {
            if (!mc.player.isOnGround() && mc.options.sneakKey.isPressed()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * 0.9, 0, v.z * 0.9);
            }
        });
        t("Rocket", "Hold jump to blast upward", Category.MOVEMENT, mc -> {
            if (mc.options.jumpKey.isPressed()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, Math.min(v.y + 0.15, 1.0), v.z);
                for (int i = 0; i < 3; i++)
                    Compat.particle(mc, ParticleTypes.FLAME, mc.player.getX() + (Math.random() - 0.5) * 0.3,
                            mc.player.getY(), mc.player.getZ() + (Math.random() - 0.5) * 0.3, 0, -0.2, 0);
            }
        });
        int[] strafeTimer = {0};
        full("CircleStrafe", "Walk in circles", Category.MOVEMENT, mc -> {
            strafeTimer[0]++;
            boolean left = (strafeTimer[0] / 20) % 2 == 0;
            mc.options.forwardKey.setPressed(true);
            mc.options.leftKey.setPressed(left);
            mc.options.rightKey.setPressed(!left);
        }, null, mc -> {
            mc.options.forwardKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        });
        t("Pogo", "Constantly hop high while moving", Category.MOVEMENT, mc -> {
            if (mc.player.isOnGround() && Compat.moving(mc)) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.7, v.z);
            }
        });
        t("AirBrake", "Stop drifting in mid-air", Category.MOVEMENT, mc -> {
            if (!mc.player.isOnGround() && !Compat.moving(mc)) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * 0.8, v.y, v.z * 0.8);
            }
        });

        // ---------------- AUTO (5) ----------------
        full("AutoMine", "Holds the attack button", Category.AUTO,
                mc -> mc.options.attackKey.setPressed(true), null, mc -> mc.options.attackKey.setPressed(false));
        full("AutoUse", "Holds the use button", Category.AUTO,
                mc -> mc.options.useKey.setPressed(true), null, mc -> mc.options.useKey.setPressed(false));
        full("AutoJump", "Holds the jump button", Category.AUTO,
                mc -> mc.options.jumpKey.setPressed(true), null, mc -> mc.options.jumpKey.setPressed(false));
        full("AutoSwim", "Swims up automatically in water", Category.AUTO,
                mc -> mc.options.jumpKey.setPressed(mc.player.isTouchingWater()), null, mc -> mc.options.jumpKey.setPressed(false));
        int[] swingTimer = {0};
        t("AutoSwing", "Swings your hand over and over", Category.AUTO, mc -> {
            if (++swingTimer[0] >= 8) { swingTimer[0] = 0; mc.player.swingHand(Hand.MAIN_HAND); }
        });

        // ---------------- FUN (7) ----------------
        t("Nod", "Your head nods up and down", Category.FUN,
                mc -> mc.player.setPitch((float) (25 * Math.sin(mc.player.age * 0.15))));
        t("Wobble", "Your view wobbles", Category.FUN,
                mc -> mc.player.setYaw(mc.player.getYaw() + (float) (Math.sin(mc.player.age * 0.5) * 2)));
        t("Drunk", "Sway while you walk", Category.FUN, mc -> {
            if (Compat.moving(mc)) mc.player.setYaw(mc.player.getYaw() + (float) (Math.sin(mc.player.age * 0.12) * 3.5));
        });
        t("SlowSpin", "A lazy spin", Category.FUN, mc -> mc.player.setYaw(mc.player.getYaw() + 3f));
        t("FastSpin", "A very fast spin", Category.FUN, mc -> mc.player.setYaw(mc.player.getYaw() + 40f));
        t("Shake", "Camera shake", Category.FUN, mc -> {
            mc.player.setYaw(mc.player.getYaw() + (float) ((Math.random() - 0.5) * 3));
            mc.player.setPitch(mc.player.getPitch() + (float) ((Math.random() - 0.5) * 3));
        });
        t("Dizzy", "Everything sways", Category.FUN, mc -> {
            mc.player.setYaw(mc.player.getYaw() + (float) (Math.sin(mc.player.age * 0.2) * 6));
            mc.player.setPitch((float) (15 * Math.cos(mc.player.age * 0.17)));
        });

        // ---------------- POTIONS (client-side) (7) ----------------
        potion("PotionHaste", "Client-side Haste III", StatusEffects.HASTE, 2);
        potion("PotionJump", "Client-side Jump Boost III", StatusEffects.JUMP_BOOST, 2);
        potion("PotionSpeed", "Client-side Speed III", StatusEffects.SPEED, 2);
        potion("PotionDolphin", "Client-side Dolphin's Grace", StatusEffects.DOLPHINS_GRACE, 0);
        potion("PotionFeather", "Client-side Slow Falling", StatusEffects.SLOW_FALLING, 0);
        potion("PotionGlow", "Client-side Glowing", StatusEffects.GLOWING, 0);
        potion("PotionInvis", "Client-side Invisibility", StatusEffects.INVISIBILITY, 0);

        // ---------------- FOV (4) ----------------
        fov("Fov50", "Field of view 50", 50);
        fov("Fov90", "Field of view 90", 90);
        fov("Fov100", "Field of view 100", 100);
        int[] pulseOld = {70};
        full("FovPulse", "Field of view breathes in and out", Category.RENDER,
                mc -> mc.options.getFov().setValue(80 + (int) (12 * Math.sin(mc.player.age * 0.12))),
                mc -> pulseOld[0] = mc.options.getFov().getValue(),
                mc -> mc.options.getFov().setValue(pulseOld[0]));

        // ---------------- OVERLAYS (drawn in Hud3) (20) ----------------
        for (String n : new String[]{"Rainfall", "Embers", "Confetti", "Sakura", "Bubbles", "StarField", "Ash", "Leaves", "NeonDots", "Sparks"})
            flag(n, "Animated screen effect: " + n, Category.OVERLAY);
        flag("Vignette", "Dark edges", Category.OVERLAY);
        flag("ColorTint", "A faint theme-colored tint", Category.OVERLAY);
        flag("Letterbox", "Cinematic black bars", Category.OVERLAY);
        flag("Scanlines", "Retro monitor lines", Category.OVERLAY);
        flag("PulseBorder", "A pulsing glowing border", Category.OVERLAY);
        flag("LowHealthPulse", "Red pulse when your health is low", Category.OVERLAY);
        flag("Lightning", "Random lightning flashes", Category.OVERLAY);
        flag("Disco", "A color-cycling tint", Category.OVERLAY);
        flag("SunsetTint", "An orange-to-purple sunset tint", Category.OVERLAY);
        flag("GridOverlay", "A faint grid", Category.OVERLAY);

        // ---------------- INFO widgets (drawn in Hud3) (20) ----------------
        flag("Coordinates", "Your XYZ position", Category.INFO);
        flag("Direction", "Which way you face", Category.INFO);
        flag("HealthBar", "Health bar", Category.INFO);
        flag("HungerBar", "Hunger bar", Category.INFO);
        flag("LookingAt", "Name of what you look at", Category.INFO);
        flag("HeldItem", "Name of your held item", Category.INFO);
        flag("Durability", "Durability of your held tool", Category.INFO);
        flag("Memory", "Game memory use", Category.INFO);
        flag("Session", "How long you have played", Category.INFO);
        flag("Distance", "Blocks walked this session", Category.INFO);
        flag("Jumps", "Jump counter", Category.INFO);
        flag("EntityCount", "Entities near you", Category.INFO);
        flag("MobCount", "Hostile mobs near you", Category.INFO);
        flag("Nearby", "Players around you with distance", Category.INFO);
        flag("YawPitch", "Your view angles", Category.INFO);
        flag("SpeedBar", "Speed bar in blocks per second", Category.INFO);
        flag("ArmorPercent", "Average armor durability", Category.INFO);
        flag("DateHUD", "Today's date", Category.INFO);
        flag("SprintStatus", "Sprinting, sneaking or walking", Category.INFO);
        flag("FpsBig", "Large FPS counter", Category.INFO);
    }
}
