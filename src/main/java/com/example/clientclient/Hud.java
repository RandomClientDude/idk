package com.example.clientclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class Hud {
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private static final int SNOW = 80;
    private static final float[] SX = new float[SNOW], SY = new float[SNOW], SV = new float[SNOW];
    private static final Random RNG = new Random();
    private static boolean snowInit;

    private Hud() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();

        Hud3.overlays(ctx, mc, w, h);
        if (ModuleManager.on("BorderGlow")) drawBorderGlow(ctx, w, h);
        if (ModuleManager.on("Snowfall")) drawSnow(ctx, w, h);
        if (mc.currentScreen instanceof ClickGui) return;

        if (ModuleManager.on("Watermark")) drawWatermark(ctx, tr, mc);
        if (ModuleManager.on("ArrayList")) drawArrayList(ctx, tr, w);
        if (ModuleManager.on("Radar")) drawRadar(ctx, mc, h);
        if (ModuleManager.on("Keystrokes")) drawKeystrokes(ctx, tr, mc, h);
        if (ModuleManager.on("Crosshair+")) drawCrosshair(ctx, w, h);
        if (ModuleManager.on("ArmorHUD")) drawArmor(ctx, mc, w, h);
        if (ModuleManager.on("TargetHUD")) drawTarget(ctx, tr, mc, w, h);
        if (ModuleManager.on("Compass")) drawCompass(ctx, tr, mc, w);
        if (ModuleManager.on("PotionHUD")) drawPotions(ctx, tr, mc, h);
        if (ModuleManager.on("Speedometer")) drawSpeed(ctx, tr, mc);
        Hud2.render(ctx, mc, tr, w, h);
        Hud3.render(ctx, mc, tr, w, h);
        Hud4.render(ctx, mc, tr, w, h);
        Notifications.render(ctx, tr, w, h);
    }

    private static void drawWatermark(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        String title = "ClientClient";
        int w = tr.getWidth(title) + 10;
        ctx.fill(4, 4, 4 + w, 18, 0x90000000);
        ctx.fillGradient(4, 4, 4 + w, 6, Colors.accent(0), Colors.accent(90));
        int x = 9;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            ctx.drawTextWithShadow(tr, ch, x, 8, Colors.accent(i * 25));
            x += tr.getWidth(ch);
        }
        String info = mc.getCurrentFps() + " FPS  |  " + (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ();
        ctx.fill(4, 20, 4 + tr.getWidth(info) + 8, 32, 0x80000000);
        ctx.drawTextWithShadow(tr, info, 8, 22, 0xFFCCCCCC);
    }

    private static void drawSpeed(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        double bps = ExtraModules.bps;
        String s = String.format("%.1f b/s", bps);
        ctx.fill(4, 34, 4 + tr.getWidth(s) + 8, 46, 0x80000000);
        ctx.fill(4, 34, 6, 46, Colors.accent(200));
        ctx.drawTextWithShadow(tr, s, 10, 36, 0xFFFFFFFF);
    }

    private static void drawArrayList(DrawContext ctx, TextRenderer tr, int screenW) {
        List<Module> sorted = ModuleManager.MODULES.stream()
                .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed()).toList();
        int y = 4, index = 0;
        for (Module m : sorted) {
            if (m.anim < 0.02f) continue;
            if (index >= 16) break;
            int w = tr.getWidth(m.name) + 8;
            int slide = (int) ((1f - m.anim) * (w + 6));
            int x = screenW - w - 4 + slide;
            int color = Colors.accent(index * 40);
            ctx.fill(x - 1, y, screenW - 3 + slide, y + 12, 0x90000000);
            ctx.fill(screenW - 3 + slide, y, screenW - 1 + slide, y + 12, color);
            ctx.drawTextWithShadow(tr, m.name, x + 3, y + 2, color);
            y += (int) (12 * m.anim);
            index++;
        }
    }

    private static void drawRadar(DrawContext ctx, MinecraftClient mc, int h) {
        PlayerEntity p = mc.player;
        int size = 84, x = 8, y = h - size - 8, cx = x + size / 2, cy = y + size / 2;
        ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, Colors.withAlpha(Colors.accent(0) & 0xFFFFFF, 120));
        ctx.fill(x, y, x + size, y + size, 0xB0000008);
        ctx.fill(cx, y + 1, cx + 1, y + size - 1, 0x30FFFFFF);
        ctx.fill(x + 1, cy, x + size - 1, cy + 1, 0x30FFFFFF);
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);

        double yaw = Math.toRadians(p.getYaw());
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        double max = size / 2.0 - 4, scale = max / 32.0;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity) || e == p) continue;
            double ddx = e.getX() - p.getX(), ddz = e.getZ() - p.getZ();
            double px = -ddx * cos - ddz * sin;
            double py = -(-ddx * sin + ddz * cos);
            px *= scale; py *= scale;
            double len = Math.hypot(px, py);
            if (len > max) { px *= max / len; py *= max / len; }
            int dx = (int) (cx + px), dy = (int) (cy + py);
            ctx.fill(dx - 1, dy - 1, dx + 2, dy + 2, 0xFFFF4444);
        }
    }

    private static void drawKeystrokes(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int h) {
        int x = 8, y = h / 2 - 28;
        key(ctx, tr, "W", mc.options.forwardKey, x + 26, y, 0);
        key(ctx, tr, "A", mc.options.leftKey, x, y + 26, 1);
        key(ctx, tr, "S", mc.options.backKey, x + 26, y + 26, 2);
        key(ctx, tr, "D", mc.options.rightKey, x + 52, y + 26, 3);
    }

    private static void key(DrawContext ctx, TextRenderer tr, String label, KeyBinding kb, int x, int y, int i) {
        boolean down = kb.isPressed();
        int accent = Colors.accent(i * 60);
        ctx.fill(x, y, x + 24, y + 24, down ? Colors.withAlpha(accent & 0xFFFFFF, 190) : 0x90000000);
        ctx.fill(x, y + 22, x + 24, y + 24, accent);
        ctx.drawCenteredTextWithShadow(tr, label, x + 12, y + 8, down ? 0xFF000000 : 0xFFFFFFFF);
    }

    private static void drawCrosshair(DrawContext ctx, int w, int h) {
        int cx = w / 2, cy = h / 2, gap = 4, len = 5;
        ctx.fill(cx - gap - len, cy, cx - gap, cy + 1, Colors.accent(0));
        ctx.fill(cx + gap + 1, cy, cx + gap + 1 + len, cy + 1, Colors.accent(90));
        ctx.fill(cx, cy - gap - len, cx + 1, cy - gap, Colors.accent(180));
        ctx.fill(cx, cy + gap + 1, cx + 1, cy + gap + 1 + len, Colors.accent(270));
    }

    private static void drawArmor(DrawContext ctx, MinecraftClient mc, int w, int h) {
        int x = w - 26, y = h / 2 - 44;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int s = 3; s >= 0; s--) {
            ItemStack st = mc.player.getEquippedStack(slots[3 - s]);
            ctx.fill(x - 2, y - 2, x + 18, y + 18, 0x80000000);
            ctx.fill(x - 2, y - 2, x - 1, y + 18, Colors.accent(s * 70));
            if (!st.isEmpty()) {
                ctx.drawItem(st, x, y);
                ctx.drawStackOverlay(mc.textRenderer, st, x, y);
            }
            y += 22;
        }
    }

    private static void drawTarget(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int w, int h) {
        if (!(mc.targetedEntity instanceof LivingEntity le)) return;
        int x = w / 2 + 18, y = h / 2 + 18, bw = 112;
        ctx.fill(x, y, x + bw, y + 30, 0xB0050510);
        ctx.fill(x, y, x + bw, y + 2, Colors.accent(0));
        ctx.drawTextWithShadow(tr, le.getName().getString(), x + 5, y + 6, 0xFFFFFFFF);
        String hp = String.format("%.1f", le.getHealth());
        ctx.drawTextWithShadow(tr, hp, x + bw - 5 - tr.getWidth(hp), y + 6, 0xFFFFAAAA);
        float ratio = Math.max(0f, Math.min(1f, le.getHealth() / le.getMaxHealth()));
        ctx.fill(x + 5, y + 19, x + bw - 5, y + 25, 0xFF2A2A2A);
        ctx.fill(x + 5, y + 19, x + 5 + (int) ((bw - 10) * ratio), y + 25, Colors.lerp(0xFFFF3355, 0xFF33FF77, ratio));
    }

    private static void drawCompass(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int w) {
        float yaw = ((mc.player.getYaw() % 360f) + 360f) % 360f;
        int cx = w / 2;
        ctx.fill(cx - 72, 4, cx + 72, 22, 0x90000000);
        for (int a = ((int) (yaw - 60)) / 15 * 15; a <= yaw + 60; a += 15) {
            int norm = ((a % 360) + 360) % 360;
            int x = cx + (int) ((a - yaw) * 1.15f);
            if (x < cx - 70 || x > cx + 70) continue;
            if (norm % 45 == 0) ctx.drawCenteredTextWithShadow(tr, DIRS[norm / 45], x, 8, 0xFFFFFFFF);
            else ctx.fill(x, 14, x + 1, 19, 0x90FFFFFF);
        }
        ctx.fill(cx, 4, cx + 1, 22, Colors.accent(0));
        ctx.fill(cx - 72, 22, cx + 72, 23, Colors.accent(120));
    }

    private static void drawPotions(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int h) {
        int y = h - 104, i = 0;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            int secs = e.getDuration() / 20;
            String s = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1)
                    + "  " + (secs / 60) + ":" + String.format("%02d", secs % 60);
            ctx.fill(8, y, 8 + tr.getWidth(s) + 8, y + 12, 0x90000000);
            ctx.fill(8, y, 10, y + 12, Colors.accent(i * 50));
            ctx.drawTextWithShadow(tr, s, 14, y + 2, 0xFFFFFFFF);
            y -= 14; i++;
        }
    }

    private static void drawSnow(DrawContext ctx, int w, int h) {
        if (!snowInit) {
            for (int i = 0; i < SNOW; i++) { SX[i] = RNG.nextFloat() * w; SY[i] = RNG.nextFloat() * h; SV[i] = 0.4f + RNG.nextFloat() * 1.1f; }
            snowInit = true;
        }
        for (int i = 0; i < SNOW; i++) {
            SY[i] += SV[i];
            SX[i] += (float) Math.sin((SY[i] + i * 10) / 25.0) * 0.4f;
            if (SY[i] > h) { SY[i] = -2; SX[i] = RNG.nextFloat() * w; }
            ctx.fill((int) SX[i], (int) SY[i], (int) SX[i] + 2, (int) SY[i] + 2, 0xC0FFFFFF);
        }
    }

    private static void drawBorderGlow(DrawContext ctx, int w, int h) {
        int base = Colors.accent(0) & 0xFFFFFF;
        for (int i = 0; i < 14; i++) {
            int c = Colors.withAlpha(base, (14 - i) * 7);
            ctx.fill(0, i, w, i + 1, c);
            ctx.fill(0, h - 1 - i, w, h - i, c);
            ctx.fill(i, 0, i + 1, h, c);
            ctx.fill(w - 1 - i, 0, w - i, h, c);
        }
    }
}
