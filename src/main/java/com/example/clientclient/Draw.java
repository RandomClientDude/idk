package com.example.clientclient;

import net.minecraft.client.gui.DrawContext;

public final class Draw {
    private Draw() {}

    /** Filled rectangle with 1px chamfered corners (no double blending on translucent colors). */
    public static void rrect(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        ctx.fill(x1, y1 + 1, x2, y2 - 1, color);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
    }
}
