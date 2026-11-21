package com.example.clientclient;

import com.example.clientclient.Module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Ten new modules for each of the ten categories. */
public final class Modules3 {
    private Modules3() {}

    private static LivingEntity nearest(MinecraftClient mc, double range, boolean players, boolean mobs) {
        LivingEntity best = null;
        double bestD = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive()) continue;
            boolean ok = (players && e instanceof PlayerEntity) || (mobs && e instanceof Monster);
            if (!ok) continue;
            double d = mc.player.squaredDistanceTo(e);
            if (d < bestD) { bestD = d; best = le; }
        }
        return best;
    }

    private static void hit(MinecraftClient mc, Entity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private static void aim(MinecraftClient mc, double range, float f, boolean players, boolean mobs) {
        LivingEntity t = nearest(mc, range, players, mobs);
        if (t == null) return;
        Vec3d d = t.getEyePos().subtract(mc.player.getEyePos());
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        float dy = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
        mc.player.setYaw(mc.player.getYaw() + dy * f);
        mc.player.setPitch(mc.player.getPitch() + (pitch - mc.player.getPitch()) * f);
    }

    private static void glow(MinecraftClient mc, Class<?> type, boolean on) {
        for (Entity e : mc.world.getEntities())
            if (type.isInstance(e) && e != mc.player) e.setGlowing(on);
    }

    private static void release(MinecraftClient mc) {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }

    static void init() {
        // =================== COMBAT (10) ===================
        MoreModules.t("AutoCrit", "Jump when hitting for critical strikes", Category.COMBAT, mc -> {
            if (mc.options.attackKey.isPressed() && mc.targetedEntity instanceof LivingEntity && mc.player.isOnGround()) mc.player.jump();
        });
        MoreModules.full("AutoShield", "Raise your shield when a hostile is close", Category.COMBAT, mc -> {
            boolean shield = mc.player.getOffHandStack().isOf(Items.SHIELD) || mc.player.getMainHandStack().isOf(Items.SHIELD);
            mc.options.useKey.setPressed(shield && nearest(mc, 4.0, false, true) != null);
        }, null, mc -> mc.options.useKey.setPressed(false));
        MoreModules.t("SnapAim", "Snaps your aim onto nearby targets", Category.COMBAT, mc -> aim(mc, 5.0, 0.6f, true, true));
        MoreModules.t("SoftAim", "A very gentle aim pull", Category.COMBAT, mc -> aim(mc, 6.0, 0.05f, true, true));
        MoreModules.full("AutoRetreat", "Back away when low on health", Category.COMBAT, mc -> {
            boolean flee = mc.player.getHealth() <= 6f && nearest(mc, 7.0, false, true) != null;
            mc.options.backKey.setPressed(flee);
            if (flee) mc.player.setSprinting(true);
        }, null, mc -> mc.options.backKey.setPressed(false));
        MoreModules.t("WideAura", "Attacks anything within 6 blocks", Category.COMBAT, mc -> {
            if (mc.player.getAttackCooldownProgress(0f) < 1f) return;
            LivingEntity e = nearest(mc, 6.0, true, true);
            if (e != null) hit(mc, e);
        });
        MoreModules.t("TargetFX", "Sparks on whatever you aim at", Category.COMBAT, mc -> {
            Entity e = mc.targetedEntity;
            if (e == null) return;
            for (int i = 0; i < 3; i++)
                Compat.particle(mc, ParticleTypes.CRIT, e.getX() + (Math.random() - 0.5) * 0.6,
                        e.getY() + e.getHeight() / 2 + (Math.random() - 0.5) * 0.6, e.getZ() + (Math.random() - 0.5) * 0.6, 0, 0.05, 0);
        });
        int[] healPrev = {-1};
        boolean[] healing = {false};
        MoreModules.full("AutoHeal", "Eats a golden apple when hurt", Category.COMBAT, mc -> {
            int slot = -1;
            for (int i = 0; i < 9; i++)
                if (mc.player.getInventory().getStack(i).isOf(Items.GOLDEN_APPLE)) { slot = i; break; }
            if (mc.player.getHealth() <= 10f && slot >= 0) {
                if (!healing[0]) healPrev[0] = Compat.getSlot(mc);
                Compat.setSlot(mc, slot);
                mc.options.useKey.setPressed(true);
                healing[0] = true;
            } else if (healing[0]) {
                mc.options.useKey.setPressed(false);
                if (healPrev[0] >= 0) Compat.setSlot(mc, healPrev[0]);
                healPrev[0] = -1;
                healing[0] = false;
            }
        }, null, mc -> { if (healing[0]) { mc.options.useKey.setPressed(false); healing[0] = false; } });
        int[] msT = {0};
        MoreModules.full("MobStrafe", "Circle around hostile mobs", Category.COMBAT, mc -> {
            if (nearest(mc, 5.0, false, true) != null) {
                msT[0]++;
                boolean left = (msT[0] / 15) % 2 == 0;
                mc.options.leftKey.setPressed(left);
                mc.options.rightKey.setPressed(!left);
            } else {
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
            }
        }, null, mc -> { mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false); });
        MoreModules.t("Executioner", "Only hits weak targets", Category.COMBAT, mc -> {
            if (mc.player.getAttackCooldownProgress(0f) < 1f) return;
            LivingEntity e = nearest(mc, 4.2, true, true);
            if (e != null && e.getHealth() <= 6f) hit(mc, e);
        });

        // =================== MOVEMENT (10) ===================
        int[] dashT = {0};
        MoreModules.t("Dash", "A burst of speed every two seconds", Category.MOVEMENT, mc -> {
            if (++dashT[0] >= 40 && Compat.forward(mc)) {
                dashT[0] = 0;
                double yaw = Math.toRadians(mc.player.getYaw());
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x - Math.sin(yaw) * 0.9, v.y, v.z + Math.cos(yaw) * 0.9);
            }
        });
        MoreModules.t("Slide", "Slippery ground like ice", Category.MOVEMENT, mc -> {
            Vec3d v = mc.player.getVelocity();
            double sp = Math.hypot(v.x, v.z);
            if (mc.player.isOnGround() && sp > 0.03 && sp < 0.6) mc.player.setVelocity(v.x * 1.55, v.y, v.z * 1.55);
        });
        int[] flapT = {0};
        MoreModules.t("Flap", "Tap jump in the air to flap upward", Category.MOVEMENT, mc -> {
            if (flapT[0] > 0) flapT[0]--;
            if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && flapT[0] == 0) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.32, v.z);
                flapT[0] = 8;
            }
        });
        MoreModules.t("MoonJump", "Jump very high and float down", Category.MOVEMENT, mc -> {
            Vec3d v = mc.player.getVelocity();
            if (mc.options.jumpKey.isPressed() && mc.player.isOnGround()) mc.player.setVelocity(v.x, 0.9, v.z);
            else if (!mc.player.isOnGround() && v.y < 0) mc.player.setVelocity(v.x, v.y * 0.85, v.z);
        });
        int[] kangT = {0};
        MoreModules.t("Kangaroo", "Hop high automatically while moving", Category.MOVEMENT, mc -> {
            if (mc.player.isOnGround() && Compat.moving(mc) && ++kangT[0] >= 6) {
                kangT[0] = 0;
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.6, v.z);
            }
        });
        MoreModules.t("Torpedo", "Swim fast", Category.MOVEMENT, mc -> {
            if (mc.player.isTouchingWater() && Compat.moving(mc)) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * 1.12, v.y, v.z * 1.12);
            }
        });
        MoreModules.t("WallJump", "Jump off walls", Category.MOVEMENT, mc -> {
            if (mc.player.horizontalCollision && mc.options.jumpKey.isPressed() && !mc.player.isOnGround()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.45, v.z);
            }
        });
        MoreModules.t("AirControl", "Steer freely in mid-air", Category.MOVEMENT, mc -> {
            if (mc.player.isOnGround()) return;
            int fwd = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
            int side = (mc.options.rightKey.isPressed() ? 1 : 0) - (mc.options.leftKey.isPressed() ? 1 : 0);
            if (fwd == 0 && side == 0) return;
            double yaw = Math.toRadians(mc.player.getYaw());
            double dx = -Math.sin(yaw) * fwd - Math.cos(yaw) * side;
            double dz = Math.cos(yaw) * fwd - Math.sin(yaw) * side;
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x + dx * 0.03, v.y, v.z + dz * 0.03);
        });
        MoreModules.t("Parachute", "Brakes a fast fall", Category.MOVEMENT, mc -> {
            Vec3d v = mc.player.getVelocity();
            if (!mc.player.isOnGround() && v.y < -0.5) mc.player.setVelocity(v.x, -0.15, v.z);
        });
        int[] surge = {0};
        MoreModules.t("Surge", "Speed builds the longer you run", Category.MOVEMENT, mc -> {
            if (mc.player.isOnGround() && Compat.moving(mc)) {
                surge[0] = Math.min(surge[0] + 1, 60);
                double f = 1.05 + surge[0] * 0.004;
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * f, v.y, v.z * f);
            } else if (!Compat.moving(mc)) surge[0] = 0;
        });

        // =================== RENDER (10) ===================
        MoreModules.full("AnimalESP", "Glowing outlines on animals", Category.RENDER,
                mc -> glow(mc, AnimalEntity.class, true), null, mc -> glow(mc, AnimalEntity.class, false));
        MoreModules.full("XPESP", "Glowing experience orbs", Category.RENDER,
                mc -> glow(mc, ExperienceOrbEntity.class, true), null, mc -> glow(mc, ExperienceOrbEntity.class, false));
        MoreModules.full("ProjectileESP", "Glowing arrows and projectiles", Category.RENDER,
                mc -> glow(mc, ProjectileEntity.class, true), null, mc -> glow(mc, ProjectileEntity.class, false));
        MoreModules.full("VillagerESP", "Glowing villagers", Category.RENDER,
                mc -> glow(mc, VillagerEntity.class, true), null, mc -> glow(mc, VillagerEntity.class, false));
        MoreModules.full("AllESP", "Glowing outlines on every living thing", Category.RENDER,
                mc -> glow(mc, LivingEntity.class, true), null, mc -> glow(mc, LivingEntity.class, false));
        int[] fsOld = {70};
        MoreModules.full("FovSprint", "Field of view widens while sprinting", Category.RENDER,
                mc -> mc.options.getFov().setValue(Math.min(110, mc.player.isSprinting() ? fsOld[0] + 15 : fsOld[0])),
                mc -> fsOld[0] = mc.options.getFov().getValue(),
                mc -> mc.options.getFov().setValue(fsOld[0]));
        MoreModules.t("Tether", "A beam of sparks to the nearest target", Category.RENDER, mc -> {
            LivingEntity e = nearest(mc, 16.0, true, true);
            if (e == null) return;
            double x0 = mc.player.getX(), y0 = mc.player.getY() + 1.0, z0 = mc.player.getZ();
            double x1 = e.getX(), y1 = e.getY() + e.getHeight() / 2, z1 = e.getZ();
            for (int i = 1; i <= 10; i++) {
                double f = i / 11.0;
                Compat.particle(mc, ParticleTypes.END_ROD, x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, z0 + (z1 - z0) * f, 0, 0, 0);
            }
        });
        MoreModules.t("Sparkle", "Sparkles drift through the air near you", Category.RENDER, mc -> {
            for (int i = 0; i < 4; i++)
                Compat.particle(mc, ParticleTypes.END_ROD, mc.player.getX() + (Math.random() - 0.5) * 16,
                        mc.player.getY() + Math.random() * 5, mc.player.getZ() + (Math.random() - 0.5) * 16, 0, 0.01, 0);
        });
        MoreModules.t("WorldSnow", "Snow falls around you", Category.RENDER, mc -> {
            for (int i = 0; i < 8; i++)
                Compat.particle(mc, ParticleTypes.SNOWFLAKE, mc.player.getX() + (Math.random() - 0.5) * 20,
                        mc.player.getY() + 4 + Math.random() * 5, mc.player.getZ() + (Math.random() - 0.5) * 20, 0, -0.05, 0);
        });
        MoreModules.t("WorldEmbers", "Embers rise around you", Category.RENDER, mc -> {
            for (int i = 0; i < 3; i++)
                Compat.particle(mc, ParticleTypes.FLAME, mc.player.getX() + (Math.random() - 0.5) * 16,
                        mc.player.getY() + Math.random() * 2, mc.player.getZ() + (Math.random() - 0.5) * 16, 0, 0.06, 0);
        });

        // =================== HUD (10) ===================
        MoreModules.flag("HotbarGlow", "A glowing strip under your hotbar", Category.HUD);
        MoreModules.flag("SlotHighlight", "Animated outline on the selected slot", Category.HUD);
        MoreModules.flag("CrosshairDot", "A small colored dot in the center", Category.HUD);
        MoreModules.flag("CrosshairBox", "Corner brackets around the crosshair", Category.HUD);
        MoreModules.flag("ActiveCount", "How many modules are on", Category.HUD);
        MoreModules.flag("WelcomeBanner", "A welcome message when you join", Category.HUD);
        MoreModules.flag("MobRadar", "A radar for hostile mobs", Category.HUD);
        MoreModules.flag("TopBar", "A moving color bar along the top", Category.HUD);
        MoreModules.flag("SideBars", "Moving color bars on both sides", Category.HUD);
        MoreModules.flag("KeyHint", "Shows your menu key", Category.HUD);

        // =================== PLAYER (10) ===================
        int[] scT = {0};
        MoreModules.t("SlotCycle", "Cycles through your hotbar slots", Category.PLAYER, mc -> {
            if (++scT[0] >= 30) { scT[0] = 0; Compat.setSlot(mc, (Compat.getSlot(mc) + 1) % 9); }
        });
        int[] lockSlot = {-1};
        MoreModules.full("SlotLock", "Keeps your hotbar slot from changing", Category.PLAYER,
                mc -> { if (lockSlot[0] >= 0) Compat.setSlot(mc, lockSlot[0]); },
                mc -> lockSlot[0] = Compat.getSlot(mc), mc -> lockSlot[0] = -1);
        MoreModules.t("Stalker", "Slowly turns to face the nearest player", Category.PLAYER, mc -> aim(mc, 24.0, 0.12f, true, false));
        MoreModules.full("EdgeSneak", "Sneaks automatically at ledges", Category.PLAYER, mc -> {
            boolean edge = false;
            if (mc.player.isOnGround()) {
                Vec3d v = mc.player.getVelocity();
                BlockPos below = new BlockPos((int) Math.floor(mc.player.getX() + v.x * 4), (int) Math.floor(mc.player.getY() - 0.5),
                        (int) Math.floor(mc.player.getZ() + v.z * 4));
                edge = mc.world.getBlockState(below).isAir();
            }
            mc.options.sneakKey.setPressed(edge);
        }, null, mc -> mc.options.sneakKey.setPressed(false));
        MoreModules.t("Freeze", "Freezes you in place", Category.PLAYER, mc -> mc.player.setVelocity(0, 0, 0));
        MoreModules.t("NoGravity", "Float instead of falling", Category.PLAYER, mc -> {
            if (!mc.player.isOnGround() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * 0.9, 0, v.z * 0.9);
            }
        });
        MoreModules.full("Autopilot", "Walks forward and jumps over obstacles", Category.PLAYER, mc -> {
            mc.options.forwardKey.setPressed(true);
            if (mc.player.horizontalCollision && mc.player.isOnGround()) mc.player.jump();
        }, null, mc -> mc.options.forwardKey.setPressed(false));
        MoreModules.full("AutoBack", "Holds the back key", Category.PLAYER,
                mc -> mc.options.backKey.setPressed(true), null, mc -> mc.options.backKey.setPressed(false));
        MoreModules.t("FoodSaver", "Stops sprinting when you are very hungry", Category.PLAYER, mc -> {
            if (mc.player.getHungerManager().getFoodLevel() <= 6) mc.player.setSprinting(false);
        });
        MoreModules.full("DiveDown", "Dive down automatically in water", Category.PLAYER,
                mc -> mc.options.sneakKey.setPressed(mc.player.isTouchingWater()), null, mc -> mc.options.sneakKey.setPressed(false));

        // =================== PARTICLES (10) ===================
        MoreModules.fx("FlameRing", "Rings of fire spread from your feet", ParticleTypes.FLAME, "ring");
        MoreModules.fx("SoulOrbit", "Souls orbit around you", ParticleTypes.SOUL, "orbit");
        MoreModules.fx("EnderSphere", "A sphere of portal particles", ParticleTypes.PORTAL, "sphere");
        MoreModules.fx("BeamPillar", "A pillar of light above you", ParticleTypes.END_ROD, "pillar");
        MoreModules.fx("SnowTornado", "A tornado of snowflakes", ParticleTypes.SNOWFLAKE, "tornado");
        MoreModules.fx("HeartFlower", "A flower of hearts at your feet", ParticleTypes.HEART, "flower");
        MoreModules.rainbow("RainbowRing", "Theme-colored expanding rings", "ring");
        MoreModules.rainbow("RainbowOrbit", "Theme-colored orbs circle you", "orbit");
        MoreModules.rainbow("RainbowTornado", "A theme-colored tornado", "tornado");
        MoreModules.rainbow("RainbowPillar", "A theme-colored pillar", "pillar");

        // =================== OVERLAYS (10) ===================
        for (String n : new String[]{"Meteors", "Glitter", "Dust", "Lanterns", "Hail", "Pollen", "Blizzard"})
            MoreModules.flag(n, "Animated screen effect: " + n, Category.OVERLAY);
        MoreModules.flag("Frame", "A moving colored frame around the screen", Category.OVERLAY);
        MoreModules.flag("Sepia", "An old-photo brown tint", Category.OVERLAY);
        MoreModules.flag("NightTint", "A deep blue night tint", Category.OVERLAY);

        // =================== INFO (10) ===================
        MoreModules.flag("Velocity", "Your velocity on each axis", Category.INFO);
        MoreModules.flag("Saturation", "Hidden food saturation", Category.INFO);
        MoreModules.flag("ArmorPoints", "Armor point total", Category.INFO);
        MoreModules.flag("AirBar", "Air bar while underwater", Category.INFO);
        MoreModules.flag("FreeSlots", "Empty inventory slots", Category.INFO);
        MoreModules.flag("HeldCount", "How many of your held item you carry", Category.INFO);
        MoreModules.flag("TargetDistance", "Distance to what you aim at", Category.INFO);
        MoreModules.flag("HitCounter", "Hits landed this session", Category.INFO);
        MoreModules.flag("PlayerCount", "Players near you", Category.INFO);
        MoreModules.flag("BlockBelow", "The block under your feet", Category.INFO);

        // =================== AUTO (10) ===================
        int[] apT = {0};
        MoreModules.full("AutoPlace", "Taps use every few ticks", Category.AUTO,
                mc -> { apT[0]++; mc.options.useKey.setPressed(apT[0] % 4 < 2); }, null, mc -> mc.options.useKey.setPressed(false));
        int[] clT = {0};
        MoreModules.full("AutoClickL", "Rapid left clicks", Category.AUTO,
                mc -> { clT[0]++; mc.options.attackKey.setPressed(clT[0] % 2 == 0); }, null, mc -> mc.options.attackKey.setPressed(false));
        int[] crT = {0};
        MoreModules.full("AutoClickR", "Rapid right clicks", Category.AUTO,
                mc -> { crT[0]++; mc.options.useKey.setPressed(crT[0] % 2 == 0); }, null, mc -> mc.options.useKey.setPressed(false));
        MoreModules.full("AutoDodge", "Sidestep when a hostile gets close", Category.AUTO, mc -> {
            boolean near = nearest(mc, 3.0, false, true) != null;
            boolean left = (mc.player.age / 10) % 2 == 0;
            mc.options.leftKey.setPressed(near && left);
            mc.options.rightKey.setPressed(near && !left);
        }, null, mc -> { mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false); });
        MoreModules.full("AutoRun", "Holds the sprint key", Category.AUTO,
                mc -> mc.options.sprintKey.setPressed(true), null, mc -> mc.options.sprintKey.setPressed(false));
        MoreModules.full("AutoCrouchLow", "Crouch when your health is low", Category.AUTO,
                mc -> mc.options.sneakKey.setPressed(mc.player.getHealth() <= 8f), null, mc -> mc.options.sneakKey.setPressed(false));
        int[] spT = {0};
        MoreModules.t("AutoSlotPrev", "Steps backward through your hotbar", Category.AUTO, mc -> {
            if (++spT[0] >= 40) { spT[0] = 0; Compat.setSlot(mc, (Compat.getSlot(mc) + 8) % 9); }
        });
        MoreModules.t("AutoLookDown", "Keeps your view pointed at the ground", Category.AUTO, mc -> mc.player.setPitch(80f));
        int[] atT = {0};
        MoreModules.t("AutoTurn", "Turns 90 degrees every four seconds", Category.AUTO, mc -> {
            if (++atT[0] >= 80) { atT[0] = 0; mc.player.setYaw(mc.player.getYaw() + 90f); }
        });
        int[] swT = {0};
        MoreModules.full("AutoSwapHands", "Swaps your hands every two seconds", Category.AUTO,
                mc -> { swT[0]++; mc.options.swapHandsKey.setPressed(swT[0] % 40 == 0); }, null, mc -> mc.options.swapHandsKey.setPressed(false));

        // =================== FUN (10) ===================
        int[] taT = {0};
        MoreModules.t("TurnAround", "Spins around 180 degrees now and then", Category.FUN, mc -> {
            if (++taT[0] >= 100) { taT[0] = 0; mc.player.setYaw(mc.player.getYaw() + 180f); }
        });
        int[] swayT = {0};
        MoreModules.full("Sway", "Sways left and right", Category.FUN, mc -> {
            swayT[0]++;
            boolean left = (swayT[0] / 10) % 2 == 0;
            mc.options.leftKey.setPressed(left);
            mc.options.rightKey.setPressed(!left);
        }, null, mc -> { mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false); });
        MoreModules.t("Headbang", "Your head bangs up and down", Category.FUN,
                mc -> mc.player.setPitch((float) (30 * Math.sin(mc.player.age * 0.6))));
        int[] zmT = {0};
        MoreModules.t("Zoomies", "Random sudden turns", Category.FUN, mc -> {
            if (++zmT[0] >= 5) { zmT[0] = 0; mc.player.setYaw(mc.player.getYaw() + (float) ((Math.random() - 0.5) * 60)); }
        });
        MoreModules.t("Helicopter", "Spin and rise", Category.FUN, mc -> {
            mc.player.setYaw(mc.player.getYaw() + 30f);
            Vec3d v = mc.player.getVelocity();
            if (v.y < 0.3) mc.player.setVelocity(v.x, v.y + 0.08, v.z);
        });
        int[] fjOld = {70};
        MoreModules.full("FovJitter", "Field of view twitches", Category.FUN,
                mc -> mc.options.getFov().setValue(fjOld[0] + (int) (Math.random() * 9)),
                mc -> fjOld[0] = mc.options.getFov().getValue(),
                mc -> mc.options.getFov().setValue(fjOld[0]));
        MoreModules.t("LookAround", "Slowly sweeps your view left and right", Category.FUN,
                mc -> mc.player.setYaw(mc.player.getYaw() + (float) (2 * Math.cos(mc.player.age * 0.05))));
        int[] rtT = {0};
        MoreModules.t("Roulette", "A random direction every two seconds", Category.FUN, mc -> {
            if (++rtT[0] >= 40) { rtT[0] = 0; mc.player.setYaw((float) (Math.random() * 360)); }
        });
        MoreModules.full("SneakDance", "Rapid sneaking", Category.FUN,
                mc -> mc.options.sneakKey.setPressed((mc.player.age / 3) % 2 == 0), null, mc -> mc.options.sneakKey.setPressed(false));
        int[] hcT = {60};
        MoreModules.t("Hiccup", "A surprise jump every few seconds", Category.FUN, mc -> {
            if (--hcT[0] <= 0) { hcT[0] = 60 + (int) (Math.random() * 60); mc.player.jump(); }
        });
    }
}
