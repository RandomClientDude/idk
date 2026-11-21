package com.example.clientclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class Hud3 {
    private static final Random RNG = new Random();
    private static final Map<String, Field> FIELDS = new HashMap<>();
    private static final long START = System.currentTimeMillis();
    private static final String[] FACING = {"South", "West", "North", "East"};
    private static boolean prevGround = true;
    private static int jumps;
    private static long nextFlash;
    private static float flash;
    private static int iy, idx;

    private Hud3() {}

    /** A field of drifting particles drawn over the screen. */
    private static final class Field {
        final int n, size, streak, color;
        final float minV, maxV, dir, drift;
        final boolean rainbow;
        final float[] x, y, v, p;
        boolean init;

        Field(int n, float minV, float maxV, float dir, float drift, int size, int streak, int color, boolean rainbow) {
            this.n = n; this.minV = minV; this.maxV = maxV; this.dir = dir; this.drift = drift;
            this.size = size; this.streak = streak; this.color = color; this.rainbow = rainbow;
            x = new float[n]; y = new float[n]; v = new float[n]; p = new float[n];
        }

        void draw(DrawContext ctx, int w, int h) {
            if (!init) {
                for (int i = 0; i < n; i++) {
                    x[i] = RNG.nextFloat() * w; y[i] = RNG.nextFloat() * h;
                    v[i] = minV + RNG.nextFloat() * (maxV - minV); p[i] = RNG.nextFloat() * 6.28f;
                }
                init = true;
            }
            for (int i = 0; i < n; i++) {
                p[i] += 0.04f;
                if (maxV > 0) {
                    y[i] += dir * v[i];
                    x[i] += (float) Math.sin(p[i] + y[i] * 0.02f) * drift;
                }
                if (y[i] > h + 8) { y[i] = -8; x[i] = RNG.nextFloat() * w; }
                if (y[i] < -8) { y[i] = h + 8; x[i] = RNG.nextFloat() * w; }
                if (x[i] < -4) x[i] = w + 4;
                if (x[i] > w + 4) x[i] = -4;
                int a = maxV > 0 ? 190 : (int) (120 + 110 * Math.sin(p[i] * 3));
                int c = rainbow ? (Colors.accent(i * 13) & 0xFFFFFF) : (color & 0xFFFFFF);
                int px = (int) x[i], py = (int) y[i];
                ctx.fill(px, py, px + size, py + size + streak, Colors.withAlpha(c, a));
            }
        }
    }

    static {
        FIELDS.put("Rainfall", new Field(90, 6, 10, 1, 0, 1, 8, 0x8FB8FF, false));
        FIELDS.put("Embers", new Field(50, 0.5f, 1.6f, -1, 0.6f, 2, 0, 0xFF8A30, false));
        FIELDS.put("Confetti", new Field(70, 0.8f, 2f, 1, 0.9f, 3, 0, 0xFFFFFF, true));
        FIELDS.put("Sakura", new Field(45, 0.4f, 1.1f, 1, 1.1f, 3, 0, 0xFFB7D5, false));
        FIELDS.put("Bubbles", new Field(40, 0.4f, 1.2f, -1, 0.5f, 3, 0, 0xB8E6FF, false));
        FIELDS.put("StarField", new Field(70, 0f, 0f, 1, 0, 1, 0, 0xFFFFFF, false));
        FIELDS.put("Ash", new Field(60, 0.3f, 0.9f, 1, 0.7f, 2, 0, 0x9A9A9A, false));
        FIELDS.put("Leaves", new Field(40, 0.6f, 1.4f, 1, 1.0f, 3, 0, 0x6FBF3F, false));
        FIELDS.put("NeonDots", new Field(40, 0.3f, 1.0f, -1, 0.8f, 3, 0, 0xFFFFFF, true));
        FIELDS.put("Sparks", new Field(45, 2f, 5f, -1, 0.3f, 1, 4, 0xFFE066, false));
        FIELDS.put("Meteors", new Field(14, 8f, 14f, 1, 0.5f, 1, 14, 0xCFE8FF, false));
        FIELDS.put("Glitter", new Field(80, 0f, 0f, 1, 0f, 2, 0, 0xFFFFFF, true));
        FIELDS.put("Dust", new Field(70, 0.1f, 0.35f, -1, 0.4f, 1, 0, 0xD8C8A8, false));
        FIELDS.put("Lanterns", new Field(18, 0.3f, 0.8f, -1, 0.5f, 5, 0, 0xFFA040, false));
        FIELDS.put("Hail", new Field(60, 9f, 13f, 1, 0.2f, 2, 3, 0xEAF6FF, false));
        FIELDS.put("Pollen", new Field(50, 0.2f, 0.6f, 1, 1.2f, 2, 0, 0xFFE55C, false));
        FIELDS.put("Blizzard", new Field(160, 3f, 6f, 1, 2.6f, 2, 0, 0xFFFFFF, false));
    }

    private static void edges(DrawContext ctx, int w, int h, int rgb, int depth, int step) {
        for (int i = 0; i < depth; i++) {
            int c = Colors.withAlpha(rgb, (depth - i) * step);
            ctx.fill(0, i, w, i + 1, c);
            ctx.fill(0, h - 1 - i, w, h - i, c);
            ctx.fill(i, 0, i + 1, h, c);
            ctx.fill(w - 1 - i, 0, w - i, h, c);
        }
    }

    /** Full-screen effects, drawn behind the rest of the HUD. */
    public static void overlays(DrawContext ctx, MinecraftClient mc, int w, int h) {
        long ms = System.currentTimeMillis();
        for (Map.Entry<String, Field> e : FIELDS.entrySet())
            if (ModuleManager.on(e.getKey())) e.getValue().draw(ctx, w, h);

        if (ModuleManager.on("Vignette")) edges(ctx, w, h, 0x000000, 22, 6);
        if (ModuleManager.on("ColorTint")) ctx.fill(0, 0, w, h, Colors.withAlpha(Colors.accent(0) & 0xFFFFFF, 28));
        if (ModuleManager.on("SunsetTint")) ctx.fillGradient(0, 0, w, h, 0x28FF6A00, 0x387A00B8);
        if (ModuleManager.on("Disco")) ctx.fill(0, 0, w, h, Colors.withAlpha(Colors.rainbow((int) (ms / 4)) & 0xFFFFFF, 35));
        if (ModuleManager.on("GridOverlay")) {
            for (int x = 0; x < w; x += 32) ctx.fill(x, 0, x + 1, h, 0x18FFFFFF);
            for (int y = 0; y < h; y += 32) ctx.fill(0, y, w, y + 1, 0x18FFFFFF);
        }
        if (ModuleManager.on("Scanlines")) for (int y = 0; y < h; y += 3) ctx.fill(0, y, w, y + 1, 0x38000000);
        if (ModuleManager.on("PulseBorder")) edges(ctx, w, h, Colors.accent(0) & 0xFFFFFF, 12, (int) (6 + 5 * Math.sin(ms / 300.0)));
        if (ModuleManager.on("LowHealthPulse") && mc.player != null && mc.player.getHealth() <= 8)
            edges(ctx, w, h, 0xFF2030, 20, (int) (5 + 4 * Math.sin(ms / 150.0)));
        if (ModuleManager.on("Lightning")) {
            if (ms > nextFlash) { flash = 1f; nextFlash = ms + 3000 + RNG.nextInt(9000); }
            if (flash > 0f) { ctx.fill(0, 0, w, h, Colors.withAlpha(0xFFFFFF, (int) (flash * 150))); flash -= 0.06f; }
        }
        if (ModuleManager.on("Sepia")) ctx.fill(0, 0, w, h, 0x30704214);
        if (ModuleManager.on("NightTint")) ctx.fill(0, 0, w, h, 0x40101050);
        if (ModuleManager.on("Frame")) {
            int c = Colors.accent((int) (ms / 8));
            ctx.fill(0, 0, w, 2, c);
            ctx.fill(0, h - 2, w, h, c);
            ctx.fill(0, 0, 2, h, c);
            ctx.fill(w - 2, 0, w, h, c);
        }
        if (ModuleManager.on("Letterbox")) {
            ctx.fill(0, 0, w, h / 9, 0xFF000000);
            ctx.fill(0, h - h / 9, w, h, 0xFF000000);
        }
    }

    private static void row(DrawContext ctx, TextRenderer tr, String s) {
        int wd = tr.getWidth(s) + 10;
        Draw.rrect(ctx, 96, iy, 96 + wd, iy + 12, 0x80000000);
        ctx.fill(96, iy + 1, 98, iy + 11, Colors.accent(idx * 55));
        ctx.drawTextWithShadow(tr, s, 102, iy + 2, 0xFFFFFFFF);
        iy += 14;
        idx++;
    }

    private static void bar(DrawContext ctx, TextRenderer tr, String label, float ratio, int color) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int bw = 100;
        Draw.rrect(ctx, 96, iy, 96 + bw + 8, iy + 16, 0x80000000);
        ctx.fill(96, iy + 1, 98, iy + 15, Colors.accent(idx * 55));
        ctx.drawTextWithShadow(tr, label, 102, iy + 1, 0xFFFFFFFF);
        ctx.fill(102, iy + 11, 102 + bw - 4, iy + 13, 0xFF2A2A2A);
        ctx.fill(102, iy + 11, 102 + (int) ((bw - 4) * r), iy + 13, color);
        iy += 18;
        idx++;
    }

    /** Info widgets stacked in a column. */
    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int w, int h) {
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        boolean ground = p.isOnGround();
        if (prevGround && !ground && p.getVelocity().y > 0) jumps++;
        prevGround = ground;

        iy = 48;
        idx = 0;
        if (ModuleManager.on("Coordinates")) row(ctx, tr, "XYZ  " + (int) p.getX() + " " + (int) p.getY() + " " + (int) p.getZ());
        if (ModuleManager.on("Direction")) row(ctx, tr, "Facing  " + FACING[Math.floorMod(Math.round(p.getYaw() / 90f), 4)]);
        if (ModuleManager.on("YawPitch")) row(ctx, tr, String.format("Yaw %.0f  Pitch %.0f", ((p.getYaw() % 360f) + 360f) % 360f, p.getPitch()));
        if (ModuleManager.on("HealthBar")) bar(ctx, tr, String.format("HP %.1f", p.getHealth()), p.getHealth() / p.getMaxHealth(), 0xFFFF4466);
        if (ModuleManager.on("HungerBar")) {
            int food = p.getHungerManager().getFoodLevel();
            bar(ctx, tr, "Food " + food, food / 20f, 0xFFFFA040);
        }
        if (ModuleManager.on("SpeedBar")) bar(ctx, tr, String.format("%.1f b/s", ExtraModules.bps), (float) (ExtraModules.bps / 20.0), 0xFFB070FF);
        if (ModuleManager.on("LookingAt")) {
            String t = "-";
            if (mc.targetedEntity != null) t = mc.targetedEntity.getName().getString();
            else if (mc.crosshairTarget instanceof BlockHitResult b)
                t = mc.world.getBlockState(b.getBlockPos()).getBlock().getName().getString();
            row(ctx, tr, "Looking at  " + t);
        }
        ItemStack held = p.getMainHandStack();
        if (ModuleManager.on("HeldItem"))
            row(ctx, tr, held.isEmpty() ? "Hand  empty" : "Hand  " + held.getName().getString() + (held.getCount() > 1 ? " x" + held.getCount() : ""));
        if (ModuleManager.on("Durability")) {
            if (held.isDamageable()) {
                int max = held.getMaxDamage(), left = max - held.getDamage();
                bar(ctx, tr, "Durability " + left, left / (float) max, 0xFF66DD88);
            } else row(ctx, tr, "Durability  -");
        }
        if (ModuleManager.on("ArmorPercent")) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            float sum = 0;
            int count = 0;
            for (EquipmentSlot s : slots) {
                ItemStack st = p.getEquippedStack(s);
                if (st.isDamageable()) { sum += (st.getMaxDamage() - st.getDamage()) / (float) st.getMaxDamage(); count++; }
            }
            row(ctx, tr, count == 0 ? "Armor  -" : "Armor  " + (int) (sum / count * 100) + "%");
        }
        if (ModuleManager.on("Memory")) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L, max = rt.maxMemory() / 1048576L;
            bar(ctx, tr, "Mem " + used + "/" + max + "MB", used / (float) max, 0xFF66AAFF);
        }
        if (ModuleManager.on("Session")) {
            long s = (System.currentTimeMillis() - START) / 1000;
            row(ctx, tr, String.format("Session  %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
        }
        if (ModuleManager.on("Distance")) row(ctx, tr, String.format("Distance  %.0f m", ExtraModules.dist));
        if (ModuleManager.on("Jumps")) row(ctx, tr, "Jumps  " + jumps);
        if (ModuleManager.on("EntityCount")) {
            int n = 0;
            for (Entity e : mc.world.getEntities()) n++;
            row(ctx, tr, "Entities  " + n);
        }
        if (ModuleManager.on("MobCount")) {
            int n = 0;
            for (Entity e : mc.world.getEntities()) if (e instanceof Monster) n++;
            row(ctx, tr, "Hostiles  " + n);
        }
        if (ModuleManager.on("DateHUD")) row(ctx, tr, LocalDate.now().toString());
        if (ModuleManager.on("SprintStatus")) row(ctx, tr, p.isSprinting() ? "Sprinting" : p.isSneaking() ? "Sneaking" : "Walking");
        if (ModuleManager.on("Velocity")) {
            var vel = p.getVelocity();
            row(ctx, tr, String.format("Vel  %.2f %.2f %.2f", vel.x, vel.y, vel.z));
        }
        if (ModuleManager.on("Saturation")) row(ctx, tr, String.format("Saturation  %.1f", p.getHungerManager().getSaturationLevel()));
        if (ModuleManager.on("ArmorPoints")) row(ctx, tr, "Armor points  " + p.getArmor());
        if (ModuleManager.on("AirBar")) bar(ctx, tr, "Air " + p.getAir(), p.getAir() / (float) p.getMaxAir(), 0xFF66CCFF);
        if (ModuleManager.on("FreeSlots")) {
            int free = 0;
            for (int i = 0; i < 36; i++) if (p.getInventory().getStack(i).isEmpty()) free++;
            row(ctx, tr, "Free slots  " + free);
        }
        if (ModuleManager.on("HeldCount")) {
            int c = 0;
            if (!held.isEmpty())
                for (int i = 0; i < 36; i++) {
                    ItemStack s2 = p.getInventory().getStack(i);
                    if (s2.isOf(held.getItem())) c += s2.getCount();
                }
            row(ctx, tr, "Held total  " + c);
        }
        if (ModuleManager.on("TargetDistance")) {
            double d = mc.crosshairTarget == null ? 0 : Math.sqrt(mc.crosshairTarget.getPos().squaredDistanceTo(p.getEyePos()));
            row(ctx, tr, String.format("Target  %.1f m", d));
        }
        if (ModuleManager.on("HitCounter")) row(ctx, tr, "Hits  " + ExtraModules.hits);
        if (ModuleManager.on("PlayerCount")) {
            int n2 = 0;
            for (Entity e : mc.world.getEntities()) if (e instanceof PlayerEntity) n2++;
            row(ctx, tr, "Players  " + n2);
        }
        if (ModuleManager.on("BlockBelow")) {
            BlockPos bp = new BlockPos((int) Math.floor(p.getX()), (int) Math.floor(p.getY() - 0.5), (int) Math.floor(p.getZ()));
            row(ctx, tr, "Below  " + mc.world.getBlockState(bp).getBlock().getName().getString());
        }
        if (ModuleManager.on("FpsBig")) {
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(96f, (float) iy);
            ctx.getMatrices().scale(2f, 2f);
            ctx.drawTextWithShadow(tr, mc.getCurrentFps() + " FPS", 0, 0, Colors.accent(idx * 55));
            ctx.getMatrices().popMatrix();
            iy += 20;
        }

        if (ModuleManager.on("Nearby")) {
            int nx = w / 2 - 60, ny = 64, shown = 0;
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PlayerEntity) || e == p || shown >= 5) continue;
                String s = e.getName().getString() + "  " + (int) Math.sqrt(p.squaredDistanceTo(e)) + "m";
                Draw.rrect(ctx, nx, ny, nx + tr.getWidth(s) + 10, ny + 12, 0x90000000);
                ctx.fill(nx, ny + 1, nx + 2, ny + 11, Colors.accent(shown * 70));
                ctx.drawTextWithShadow(tr, s, nx + 6, ny + 2, 0xFFFFFFFF);
                ny += 14;
                shown++;
            }
        }
    }
}
