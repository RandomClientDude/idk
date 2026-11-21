package com.example.clientclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public final class Hud2 {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Random RNG = new Random();
    private static final int FLIES = 36, COLS = 40;
    private static final float[] FX = new float[FLIES], FY = new float[FLIES], FP = new float[FLIES];
    private static final float[] MY = new float[COLS], MS = new float[COLS];
    private static boolean init;
    private static final String CH = "0123456789ABCDEF";

    private Hud2() {}

    private static void pill(DrawContext ctx, TextRenderer tr, int y, String s, int idx) {
        int w = tr.getWidth(s) + 10;
        Draw.rrect(ctx, 4, y, 4 + w, y + 12, 0x80000000);
        ctx.fill(4, y + 1, 6, y + 11, Colors.accent(idx * 60));
        ctx.drawTextWithShadow(tr, s, 10, y + 2, 0xFFFFFFFF);
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int w, int h) {
        if (!init) {
            for (int i = 0; i < FLIES; i++) { FX[i] = RNG.nextFloat() * w; FY[i] = RNG.nextFloat() * h; FP[i] = RNG.nextFloat() * 6f; }
            for (int i = 0; i < COLS; i++) { MY[i] = RNG.nextFloat() * h; MS[i] = 1f + RNG.nextFloat() * 2.5f; }
            init = true;
        }
        long now = System.currentTimeMillis();

        if (ModuleManager.on("MatrixRain")) drawMatrix(ctx, tr, w, h);
        if (ModuleManager.on("Fireflies")) drawFireflies(ctx, w, h);

        int y = 48, i = 0;
        if (ModuleManager.on("CPS")) { pill(ctx, tr, y, "CPS: " + ExtraModules.CLICKS.size(), i++); y += 14; }
        if (ModuleManager.on("Ping")) {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            pill(ctx, tr, y, e == null ? "Ping: --" : "Ping: " + e.getLatency() + "ms", i++); y += 14;
        }
        if (ModuleManager.on("Clock")) { pill(ctx, tr, y, LocalTime.now().format(CLOCK), i++); y += 14; }

        if (ModuleManager.on("FpsGraph")) drawFpsGraph(ctx, tr, w);
        if (ModuleManager.on("InventoryHUD")) drawInventory(ctx, mc, w, h);
        if (ModuleManager.on("Totems")) drawTotems(ctx, tr, mc, w, h);

        if (ModuleManager.on("Hitmarker")) {
            long age = now - ExtraModules.lastHit;
            if (age < 260) {
                int col = Colors.withAlpha(0xFF4466, (int) (255 * (1f - age / 260f)));
                int cx = w / 2, cy = h / 2;
                for (int k = 3; k <= 8; k++) {
                    ctx.fill(cx - k, cy - k, cx - k + 1, cy - k + 1, col);
                    ctx.fill(cx + k, cy - k, cx + k + 1, cy - k + 1, col);
                    ctx.fill(cx - k, cy + k, cx - k + 1, cy + k + 1, col);
                    ctx.fill(cx + k, cy + k, cx + k + 1, cy + k + 1, col);
                }
            }
        }

        if (ModuleManager.on("ComboCounter") && ExtraModules.combo > 1 && now - ExtraModules.comboTime < 2000) {
            String s = "Combo x" + ExtraModules.combo;
            int cx = w / 2, cy = h / 2 + 56;
            ctx.drawCenteredTextWithShadow(tr, s, cx, cy, Colors.accent(0));
            int bar = (int) (60 * (1f - (now - ExtraModules.comboTime) / 2000f));
            ctx.fill(cx - bar / 2, cy + 11, cx + bar / 2, cy + 12, Colors.accent(120));
        }
    }

    private static void drawFpsGraph(DrawContext ctx, TextRenderer tr, int w) {
        int x = w / 2 - 60, y = 28;
        Draw.rrect(ctx, x - 2, y, x + 122, y + 32, 0x90000000);
        for (int k = 0; k < 60; k++) {
            int v = ExtraModules.FPS[(ExtraModules.fpsIdx + k) % 60];
            int bh = Math.max(1, Math.min(24, v / 10));
            ctx.fill(x + k * 2, y + 28 - bh, x + k * 2 + 2, y + 28, Colors.accent(k * 6));
        }
        ctx.drawTextWithShadow(tr, ExtraModules.FPS[(ExtraModules.fpsIdx + 59) % 60] + " fps", x + 2, y + 2, 0xFFFFFFFF);
    }

    private static void drawInventory(DrawContext ctx, MinecraftClient mc, int w, int h) {
        int x = w - 172, y = h / 2 + 56;
        Draw.rrect(ctx, x - 2, y - 2, x + 164, y + 56, 0x90000008);
        ctx.fill(x - 2, y - 2, x + 164, y - 1, Colors.accent(0));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) {
                ItemStack st = mc.player.getInventory().getStack(9 + r * 9 + c);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, x + c * 18, y + r * 18);
                ctx.drawStackOverlay(mc.textRenderer, st, x + c * 18, y + r * 18);
            }
    }

    private static void drawTotems(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int w, int h) {
        int n = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? mc.player.getOffHandStack().getCount() : 0;
        for (int k = 0; k < 36; k++) {
            ItemStack s = mc.player.getInventory().getStack(k);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) n += s.getCount();
        }
        int x = w / 2 + 100, y = h - 62;
        Draw.rrect(ctx, x - 2, y - 2, x + 32, y + 18, 0x90000000);
        ctx.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), x, y);
        ctx.drawTextWithShadow(tr, "x" + n, x + 18, y + 4, n > 0 ? 0xFFFFFFFF : 0xFFFF5566);
    }

    private static void drawFireflies(DrawContext ctx, int w, int h) {
        for (int i = 0; i < FLIES; i++) {
            FP[i] += 0.03f;
            FX[i] += (float) Math.sin(FP[i] + i) * 0.35f;
            FY[i] += (float) Math.cos(FP[i] * 0.8f + i) * 0.25f;
            if (FX[i] < 0) FX[i] = w; if (FX[i] > w) FX[i] = 0;
            if (FY[i] < 0) FY[i] = h; if (FY[i] > h) FY[i] = 0;
            int a = (int) (110 + 100 * Math.sin(FP[i] * 3f));
            int x = (int) FX[i], y = (int) FY[i];
            ctx.fill(x - 1, y - 1, x + 3, y + 3, Colors.withAlpha(0xCCFF66, a / 4));
            ctx.fill(x, y, x + 2, y + 2, Colors.withAlpha(0xEFFF99, Math.min(255, a + 50)));
        }
    }

    private static void drawMatrix(DrawContext ctx, TextRenderer tr, int w, int h) {
        int step = Math.max(8, w / COLS);
        int tick = (int) (System.currentTimeMillis() / 120);
        for (int c = 0; c < COLS; c++) {
            MY[c] += MS[c];
            if (MY[c] - 60 > h) MY[c] = 0;
            int x = c * step;
            for (int t = 0; t < 6; t++) {
                int y = (int) (MY[c] - t * 9);
                if (y < 0 || y > h) continue;
                int col = t == 0 ? 0xFFCCFFCC : Colors.withAlpha(0x33FF66, 255 - t * 40);
                ctx.drawText(tr, String.valueOf(CH.charAt((c * 7 + t + tick) % 16)), x, y, col, false);
            }
        }
    }
}
