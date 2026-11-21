package com.example.clientclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

public final class Hud4 {
    private Hud4() {}

    private static void pillRight(DrawContext ctx, TextRenderer tr, int w, int y, String s, int idx) {
        int wd = tr.getWidth(s) + 10;
        int x = w - wd - 4;
        Draw.rrect(ctx, x, y, x + wd, y + 12, 0x80000000);
        ctx.fill(x + wd - 2, y + 1, x + wd, y + 11, Colors.accent(idx * 60));
        ctx.drawTextWithShadow(tr, s, x + 4, y + 2, 0xFFFFFFFF);
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int w, int h) {
        PlayerEntity p = mc.player;
        if (p == null) return;
        long ms = System.currentTimeMillis();

        if (ModuleManager.on("TopBar"))
            for (int x = 0; x < w; x += 4) ctx.fill(x, 0, Math.min(x + 4, w), 2, Colors.accent(x - (int) (ms / 8)));

        if (ModuleManager.on("SideBars"))
            for (int y = 0; y < h; y += 4) {
                int c = Colors.accent(y - (int) (ms / 8));
                ctx.fill(0, y, 2, Math.min(y + 4, h), c);
                ctx.fill(w - 2, y, w, Math.min(y + 4, h), c);
            }

        if (ModuleManager.on("HotbarGlow")) {
            int x0 = w / 2 - 91;
            ctx.fill(x0, h - 5, x0 + 182, h - 2, Colors.withAlpha(Colors.accent(0) & 0xFFFFFF, 60));
            for (int i = 0; i < 182; i += 2) ctx.fill(x0 + i, h - 2, x0 + i + 2, h, Colors.accent(i * 2 - (int) (ms / 8)));
        }

        if (ModuleManager.on("SlotHighlight")) {
            int x = w / 2 - 91 + Compat.getSlot(mc) * 20, y = h - 22;
            int c = Colors.accent((int) (ms / 8));
            ctx.fill(x - 2, y - 2, x + 22, y - 1, c);
            ctx.fill(x - 2, y + 21, x + 22, y + 22, c);
            ctx.fill(x - 2, y - 1, x - 1, y + 21, c);
            ctx.fill(x + 21, y - 1, x + 22, y + 21, c);
        }

        int cx = w / 2, cy = h / 2;
        if (ModuleManager.on("CrosshairDot")) ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, Colors.accent(0));
        if (ModuleManager.on("CrosshairBox")) {
            int r = 8, c = Colors.accent(60);
            ctx.fill(cx - r, cy - r, cx - r + 4, cy - r + 1, c);
            ctx.fill(cx - r, cy - r, cx - r + 1, cy - r + 4, c);
            ctx.fill(cx + r - 4, cy - r, cx + r, cy - r + 1, c);
            ctx.fill(cx + r - 1, cy - r, cx + r, cy - r + 4, c);
            ctx.fill(cx - r, cy + r - 1, cx - r + 4, cy + r, c);
            ctx.fill(cx - r, cy + r - 4, cx - r + 1, cy + r, c);
            ctx.fill(cx + r - 4, cy + r - 1, cx + r, cy + r, c);
            ctx.fill(cx + r - 1, cy + r - 4, cx + r, cy + r, c);
        }

        if (ModuleManager.on("KeyHint")) pillRight(ctx, tr, w, h - 14, "Menu: " + Keys.name(Config.menuKey), 1);
        if (ModuleManager.on("ActiveCount")) {
            int n = 0;
            for (Module m : ModuleManager.MODULES) if (m.isEnabled()) n++;
            pillRight(ctx, tr, w, h - 28, n + " active", 2);
        }

        if (ModuleManager.on("WelcomeBanner") && p.age < 120) {
            int a = p.age < 90 ? 255 : (120 - p.age) * 8;
            String s = "Welcome, " + p.getName().getString();
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate((float) (w / 2), (float) (h / 4));
            ctx.getMatrices().scale(2f, 2f);
            ctx.drawCenteredTextWithShadow(tr, s, 0, 0, Colors.withAlpha(Colors.accent((int) (ms / 8)) & 0xFFFFFF, a));
            ctx.getMatrices().popMatrix();
        }

        if (ModuleManager.on("MobRadar")) {
            int size = 84, x = w - 96, y = h - 146, mx = x + size / 2, my = y + size / 2;
            ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, Colors.withAlpha(0xFF8030, 120));
            ctx.fill(x, y, x + size, y + size, 0xB0000008);
            ctx.fill(mx, y + 1, mx + 1, y + size - 1, 0x30FFFFFF);
            ctx.fill(x + 1, my, x + size - 1, my + 1, 0x30FFFFFF);
            ctx.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFFFFFFF);
            double yaw = Math.toRadians(p.getYaw());
            double cos = Math.cos(yaw), sin = Math.sin(yaw);
            double max = size / 2.0 - 4, scale = max / 32.0;
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof Monster)) continue;
                double ddx = e.getX() - p.getX(), ddz = e.getZ() - p.getZ();
                double px = (-ddx * cos - ddz * sin) * scale;
                double py = -(-ddx * sin + ddz * cos) * scale;
                double len = Math.hypot(px, py);
                if (len > max) { px *= max / len; py *= max / len; }
                int dx = (int) (mx + px), dy = (int) (my + py);
                ctx.fill(dx - 1, dy - 1, dx + 2, dy + 2, 0xFFFF9030);
            }
        }
    }
}
