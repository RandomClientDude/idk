package com.example.clientclient;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class Notifications {
    private record Note(String name, boolean on, long time) {}
    private static final List<Note> LIST = new ArrayList<>();
    private static final long LIFE = 2400, SLIDE = 260;

    private Notifications() {}

    public static void push(String name, boolean on) {
        LIST.add(new Note(name, on, System.currentTimeMillis()));
        if (LIST.size() > 5) LIST.remove(0);
    }

    public static void render(DrawContext ctx, TextRenderer tr, int w, int h) {
        long now = System.currentTimeMillis();
        LIST.removeIf(n -> now - n.time() > LIFE);
        int y = h - 52;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            Note n = LIST.get(i);
            long age = now - n.time();
            float in = Math.min(1f, age / (float) SLIDE);
            float out = age > LIFE - SLIDE ? (LIFE - age) / (float) SLIDE : 1f;
            float t = Math.min(in, out);
            String state = n.on() ? "enabled" : "disabled";
            int width = tr.getWidth(n.name() + "  " + state) + 18;
            int x = w - width - 6 + (int) ((1f - t) * (width + 10));
            int col = n.on() ? 0xFF55FF77 : 0xFFFF5566;
            Draw.rrect(ctx, x, y, x + width, y + 18, 0xC0050510);
            ctx.fill(x, y, x + 3, y + 18, col);
            ctx.fill(x, y + 16, x + (int) (width * (1f - age / (float) LIFE)), y + 18, Colors.withAlpha(col, 160));
            ctx.drawTextWithShadow(tr, n.name(), x + 8, y + 5, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, state, x + 8 + tr.getWidth(n.name() + "  "), y + 5, col);
            y -= 22;
        }
    }
}
