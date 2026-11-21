package com.example.clientclient;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Random;

/** Decorates Minecraft's title screen and adds a ClientClient button. */
public final class TitleFx {
    private static final int N = 120;
    private static final float[] X = new float[N], Y = new float[N], V = new float[N], P = new float[N];
    private static final Random RNG = new Random();
    private static boolean init;

    private TitleFx() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof TitleScreen)) return;
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("ClientClient"), b -> client.setScreen(new ClickGui()))
                    .dimensions(w - 108, 8, 100, 20).build());
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> render(ctx, client, s.width, s.height));
        });
    }

    private static void render(DrawContext ctx, MinecraftClient mc, int w, int h) {
        long ms = System.currentTimeMillis();
        if (!init) {
            for (int i = 0; i < N; i++) {
                X[i] = RNG.nextFloat() * w; Y[i] = RNG.nextFloat() * h;
                V[i] = 0.3f + RNG.nextFloat() * 1.1f; P[i] = RNG.nextFloat() * 6.28f;
            }
            init = true;
        }

        // glowing screen edges
        for (int i = 0; i < 28; i++) {
            int c = Colors.withAlpha(Colors.accent(i * 9) & 0xFFFFFF, (28 - i) * 4);
            ctx.fill(0, i, w, i + 1, c);
            ctx.fill(0, h - 1 - i, w, h - i, c);
            ctx.fill(i, 0, i + 1, h, c);
            ctx.fill(w - 1 - i, 0, w - i, h, c);
        }

        // rising embers and twinkling stars
        for (int i = 0; i < N; i++) {
            P[i] += 0.04f;
            boolean star = i % 3 == 0;
            if (!star) {
                Y[i] -= V[i];
                X[i] += (float) Math.sin(P[i] + Y[i] * 0.02f) * 0.4f;
                if (Y[i] < -4) { Y[i] = h + 4; X[i] = RNG.nextFloat() * w; }
            }
            int a = star ? (int) (110 + 110 * Math.sin(P[i] * 3)) : 170;
            int size = (i % 5 == 0) ? 2 : 1;
            ctx.fill((int) X[i], (int) Y[i], (int) X[i] + size, (int) Y[i] + size, Colors.withAlpha(Colors.accent(i * 11) & 0xFFFFFF, a));
        }

        // animated bars
        for (int x = 0; x < w; x += 4) {
            int c = Colors.accent(x - (int) (ms / 6));
            ctx.fill(x, 0, Math.min(x + 4, w), 2, c);
            ctx.fill(x, h - 2, Math.min(x + 4, w), h, c);
        }

        // badge
        String title = "ClientClient";
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(12f, 12f);
        ctx.getMatrices().scale(2f, 2f);
        int tx = 0;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            ctx.drawTextWithShadow(mc.textRenderer, ch, tx, 0, Colors.accent(i * 35 + (int) (ms / 10)));
            tx += mc.textRenderer.getWidth(ch);
        }
        ctx.getMatrices().popMatrix();
        ctx.drawTextWithShadow(mc.textRenderer, ModuleManager.MODULES.size() + " modules", 12, 32, 0xFFC8C8D8);
        ctx.drawTextWithShadow(mc.textRenderer, "Menu key: " + Keys.name(Config.menuKey) + "   (click the button to rebind)", 12, 44, 0xFF9898AC);

        String foot = "ClientClient for Minecraft 1.21.11";
        ctx.drawCenteredTextWithShadow(mc.textRenderer, foot, w / 2, h - 16, Colors.accent((int) (ms / 8)));
    }
}
