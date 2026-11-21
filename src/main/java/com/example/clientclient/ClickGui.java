package com.example.clientclient;

import com.example.clientclient.Module.Category;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClickGui extends Screen {
    private static final int SIDE = 112, HEAD = 34, CARD_H = 26, GAP = 6, STARS = 90;
    private static int selected = 0;
    private static String query = "";
    private static final float[] SCROLL = new float[Category.values().length + 1];

    private final float[] sx = new float[STARS], sy = new float[STARS], sv = new float[STARS];
    private final Random rng = new Random();
    private boolean starsInit;
    private boolean rebinding;
    private float open = 0f;
    private final long openedAt = System.currentTimeMillis();

    public ClickGui() { super(Text.literal("ClientClient")); }

    @Override public boolean shouldPause() { return false; }

    private int winW() { return Math.min(width - 24, 560); }
    private int winH() { return Math.min(height - 24, 330); }
    private int winX() { return (width - winW()) / 2; }
    private int winY() { return (height - winH()) / 2 + (int) ((1f - open) * 24); }
    private int cols() { return winW() >= 480 ? 3 : 2; }
    private int cardW() { return (winW() - SIDE - 20 - (cols() - 1) * GAP) / cols(); }
    private int viewX() { return winX() + SIDE + 10; }
    private int viewY() { return winY() + HEAD + 6; }
    private int viewH() { return winH() - HEAD - 12; }
    private int rowH() { return Math.max(12, Math.min(17, (winH() - 62 - HEAD - 8) / Category.values().length)); }
    private int scrollIdx() { return query.isEmpty() ? selected : SCROLL.length - 1; }

    private List<Module> current() {
        if (query.isEmpty()) return ModuleManager.inCategory(Category.values()[selected]);
        List<Module> out = new ArrayList<>();
        String q = query.toLowerCase();
        for (Module m : ModuleManager.MODULES)
            if (m.name.toLowerCase().contains(q) || m.description.toLowerCase().contains(q)) out.add(m);
        return out;
    }

    private String fit(String s, int maxW) {
        if (textRenderer.getWidth(s) <= maxW) return s;
        while (s.length() > 1 && textRenderer.getWidth(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    private float maxScroll(int count) {
        int rows = (count + cols() - 1) / cols();
        return Math.max(0f, rows * (CARD_H + GAP) - viewH());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        open += (1f - open) * 0.16f;
        int alpha = (int) (open * 255);
        long ms = System.currentTimeMillis();
        Category[] cats = Category.values();

        ctx.fillGradient(0, 0, width, height,
                Colors.withAlpha(0x000010, (int) (open * 130)),
                Colors.withAlpha(Colors.accent(0) & 0xFFFFFF, (int) (open * 80)));
        drawStars(ctx);

        int wx = winX(), wy = winY(), ww = winW(), wh = winH();
        Draw.rrect(ctx, wx - 3, wy - 3, wx + ww + 3, wy + wh + 3, Colors.withAlpha(Colors.accent(0) & 0xFFFFFF, (int) (open * 60)));
        Draw.rrect(ctx, wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, Colors.withAlpha(Colors.accent(60) & 0xFFFFFF, (int) (open * 170)));
        Draw.rrect(ctx, wx, wy, wx + ww, wy + wh, Colors.withAlpha(0x0A0A12, (int) (open * 242)));

        // header
        ctx.fillGradient(wx + 1, wy + 1, wx + ww - 1, wy + HEAD, Colors.withAlpha(Colors.accent(0) & 0xFFFFFF, (int) (open * 90)), 0x000A0A12);
        for (int x = wx + 1; x < wx + ww - 1; x += 6)
            ctx.fill(x, wy + HEAD, Math.min(x + 6, wx + ww - 1), wy + HEAD + 1, Colors.accent(x - wx - (int) (ms / 8)));
        String title = "ClientClient";
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) (wx + 10), (float) (wy + 9));
        ctx.getMatrices().scale(2f, 2f);
        int tx = 0;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            ctx.drawTextWithShadow(textRenderer, ch, tx, 0, Colors.accent(i * 35 + (int) (ms / 10)));
            tx += textRenderer.getWidth(ch);
        }
        ctx.getMatrices().popMatrix();

        // search box
        int sbW = Math.min(150, ww - 190), sbX = wx + ww - sbW - 10, sbY = wy + 10;
        Draw.rrect(ctx, sbX - 1, sbY - 1, sbX + sbW + 1, sbY + 15, Colors.withAlpha(Colors.accent(30) & 0xFFFFFF, 150));
        Draw.rrect(ctx, sbX, sbY, sbX + sbW, sbY + 14, 0xFF10101A);
        boolean caret = (ms / 500) % 2 == 0;
        if (query.isEmpty()) ctx.drawTextWithShadow(textRenderer, "Type to search...", sbX + 5, sbY + 3, 0xFF6C6C80);
        else ctx.drawTextWithShadow(textRenderer, fit(query, sbW - 14) + (caret ? "_" : ""), sbX + 5, sbY + 3, 0xFFFFFFFF);

        // sidebar
        ctx.fill(wx + 1, wy + HEAD + 1, wx + SIDE, wy + wh - 1, Colors.withAlpha(0x07070D, alpha));
        int rh = rowH();
        for (int i = 0; i < cats.length; i++) {
            int cy = wy + HEAD + 8 + i * rh;
            boolean sel = i == selected && query.isEmpty();
            boolean hov = mouseX >= wx + 4 && mouseX <= wx + SIDE - 4 && mouseY >= cy && mouseY < cy + rh - 2;
            if (sel) {
                ctx.fillGradient(wx + 4, cy, wx + SIDE - 4, cy + rh - 2,
                        Colors.withAlpha(Colors.accent(i * 40) & 0xFFFFFF, 0x90), Colors.withAlpha(Colors.accent(i * 40 + 120) & 0xFFFFFF, 0x30));
                ctx.fill(wx + 4, cy, wx + 6, cy + rh - 2, Colors.accent(i * 40));
            } else if (hov) ctx.fill(wx + 4, cy, wx + SIDE - 4, cy + rh - 2, 0x30FFFFFF);
            List<Module> mods = ModuleManager.inCategory(cats[i]);
            int on = (int) mods.stream().filter(Module::isEnabled).count();
            int ty = cy + (rh - 2 - 8) / 2;
            ctx.drawTextWithShadow(textRenderer, cats[i].label, wx + 12, ty, sel ? 0xFFFFFFFF : 0xFFA8A8B8);
            String cnt = on + "/" + mods.size();
            ctx.drawTextWithShadow(textRenderer, cnt, wx + SIDE - 8 - textRenderer.getWidth(cnt), ty, sel ? 0xFFE8E8F0 : 0xFF70708A);
        }

        // menu key button
        int kbY = wy + wh - 50, kbX = wx + 8, kbW = SIDE - 16;
        ctx.drawTextWithShadow(textRenderer, "Menu key (click)", kbX, kbY - 11, 0xFFB8B8C8);
        boolean kbHover = mouseX >= kbX && mouseX <= kbX + kbW && mouseY >= kbY && mouseY <= kbY + 14;
        Draw.rrect(ctx, kbX - 1, kbY - 1, kbX + kbW + 1, kbY + 15, rebinding ? 0xFFFFC040 : Colors.withAlpha(Colors.accent(90) & 0xFFFFFF, 160));
        Draw.rrect(ctx, kbX, kbY, kbX + kbW, kbY + 14, kbHover ? 0xFF1E1E2C : 0xFF12121C);
        String kbLabel = rebinding ? ((ms / 400) % 2 == 0 ? "Press a key..." : "Press a key") : Keys.name(Config.menuKey);
        ctx.drawCenteredTextWithShadow(textRenderer, fit(kbLabel, kbW - 6), kbX + kbW / 2, kbY + 3, rebinding ? 0xFFFFC040 : 0xFFFFFFFF);

        // theme chips
        int chipY = wy + wh - 20;
        ctx.drawTextWithShadow(textRenderer, "Theme: " + Colors.THEMES[Colors.theme], wx + 8, chipY - 12 + 0, 0xFFB8B8C8);
        for (int t = 0; t < Colors.THEMES.length; t++) {
            int saved = Colors.theme;
            Colors.theme = t;
            int c = Colors.accent(0);
            Colors.theme = saved;
            int cx = wx + 8 + t * 15;
            ctx.fill(cx, chipY, cx + 12, chipY + 12, c);
            if (t == saved) {
                ctx.fill(cx - 1, chipY - 1, cx + 13, chipY, 0xFFFFFFFF);
                ctx.fill(cx - 1, chipY + 12, cx + 13, chipY + 13, 0xFFFFFFFF);
                ctx.fill(cx - 1, chipY, cx, chipY + 12, 0xFFFFFFFF);
                ctx.fill(cx + 12, chipY, cx + 13, chipY + 12, 0xFFFFFFFF);
            }
        }

        // module cards
        List<Module> mods = current();
        float max = maxScroll(mods.size());
        int si = scrollIdx();
        SCROLL[si] = Math.max(0f, Math.min(max, SCROLL[si]));
        int cols = cols(), cw = cardW(), vx = viewX(), vy = viewY(), vh = viewH();
        Module hovered = null;
        if (mods.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, "No modules match \"" + query + "\"", vx + (cols * (cw + GAP)) / 2, vy + 40, 0xFF8A8A9C);
        ctx.enableScissor(vx - 2, vy, vx + cols * (cw + GAP), vy + vh);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = vx + (i % cols) * (cw + GAP);
            int y = vy + (i / cols) * (CARD_H + GAP) - (int) SCROLL[si];
            if (y + CARD_H < vy || y > vy + vh) continue;
            boolean hover = mouseX >= x && mouseX <= x + cw && mouseY >= Math.max(y, vy) && mouseY < Math.min(y + CARD_H, vy + vh);
            if (hover) hovered = m;
            int accent = Colors.accent(i * 37);
            Draw.rrect(ctx, x, y, x + cw, y + CARD_H, Colors.withAlpha(hover ? 0x20202C : 0x13131B, alpha));
            if (m.anim > 0.01f) {
                int fw = (int) (cw * m.anim);
                ctx.fillGradient(x, y, x + fw, y + CARD_H,
                        Colors.withAlpha(accent & 0xFFFFFF, 0x78), Colors.withAlpha(Colors.accent(i * 37 + 120) & 0xFFFFFF, 0x28));
                ctx.fill(x, y, x + 2, y + CARD_H, accent);
            }
            ctx.drawTextWithShadow(textRenderer, fit(m.name, cw - 18), x + 7, y + 4, m.isEnabled() ? 0xFFFFFFFF : 0xFFD0D0DA);
            ctx.drawTextWithShadow(textRenderer, fit(m.description, cw - 12), x + 7, y + 15, 0xFF8A8A9C);
            ctx.fill(x + cw - 9, y + 4, x + cw - 5, y + 8, m.isEnabled() ? 0xFF55FF77 : 0xFF553344);
        }
        ctx.disableScissor();

        // scrollbar
        if (max > 0f) {
            int bx = wx + ww - 5;
            ctx.fill(bx, vy, bx + 2, vy + vh, 0x30FFFFFF);
            int total = (int) (max + vh);
            int th = Math.max(16, vh * vh / total);
            int ty = vy + (int) ((vh - th) * (SCROLL[si] / max));
            ctx.fill(bx, ty, bx + 2, ty + th, Colors.accent(0));
        }

        ctx.drawCenteredTextWithShadow(textRenderer, "Type: search   Scroll: more   Up/Down: category   " + Keys.name(Config.menuKey) + ": close",
                width / 2, height - 10, 0xFFA0A0B0);
        if (hovered != null) ctx.drawTooltip(textRenderer, Text.literal(hovered.description), mouseX, mouseY);
    }

    private void drawStars(DrawContext ctx) {
        if (!starsInit) {
            for (int i = 0; i < STARS; i++) { sx[i] = rng.nextFloat() * width; sy[i] = rng.nextFloat() * height; sv[i] = 0.2f + rng.nextFloat() * 0.8f; }
            starsInit = true;
        }
        for (int i = 0; i < STARS; i++) {
            sy[i] -= sv[i];
            if (sy[i] < 0) { sy[i] = height; sx[i] = rng.nextFloat() * width; }
            int size = sv[i] > 0.6f ? 2 : 1;
            ctx.fill((int) sx[i], (int) sy[i], (int) sx[i] + size, (int) sy[i] + size,
                    Colors.withAlpha(Colors.accent(i * 7) & 0xFFFFFF, (int) (open * 170)));
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        Category[] cats = Category.values();
        int wx = winX(), wy = winY(), wh = winH(), rh = rowH();

        for (int i = 0; i < cats.length; i++) {
            int cy = wy + HEAD + 8 + i * rh;
            if (mx >= wx + 4 && mx <= wx + SIDE - 4 && my >= cy && my < cy + rh - 2) { selected = i; query = ""; rebinding = false; return true; }
        }
        int kbY = wy + wh - 50, kbX = wx + 8, kbW = SIDE - 16;
        if (mx >= kbX && mx <= kbX + kbW && my >= kbY && my <= kbY + 14) { rebinding = !rebinding; return true; }
        int chipY = wy + wh - 20;
        for (int t = 0; t < Colors.THEMES.length; t++) {
            int cx = wx + 8 + t * 15;
            if (mx >= cx && mx <= cx + 12 && my >= chipY && my <= chipY + 12) { Colors.theme = t; Config.save(); return true; }
        }
        List<Module> mods = current();
        int cols = cols(), cw = cardW(), vx = viewX(), vy = viewY(), vh = viewH();
        if (my >= vy && my < vy + vh) {
            for (int i = 0; i < mods.size(); i++) {
                int x = vx + (i % cols) * (cw + GAP);
                int y = vy + (i / cols) * (CARD_H + GAP) - (int) SCROLL[scrollIdx()];
                if (mx >= x && mx <= x + cw && my >= y && my < y + CARD_H) { mods.get(i).toggle(); return true; }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        SCROLL[scrollIdx()] -= (float) (verticalAmount * 22);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.getKeycode();
        int n = Category.values().length;

        if (rebinding) {
            if (key != GLFW.GLFW_KEY_ESCAPE) { Config.menuKey = key; Config.save(); }
            rebinding = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (!query.isEmpty()) { query = ""; return true; }
            close();
            return true;
        }
        boolean typing = (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) || (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) || key == GLFW.GLFW_KEY_SPACE;
        if (key == Config.menuKey && System.currentTimeMillis() - openedAt > 250 && (!typing || query.isEmpty())) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
            return true;
        }
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) { query += (char) ('a' + (key - GLFW.GLFW_KEY_A)); SCROLL[SCROLL.length - 1] = 0; return true; }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) { query += (char) ('0' + (key - GLFW.GLFW_KEY_0)); SCROLL[SCROLL.length - 1] = 0; return true; }
        if (key == GLFW.GLFW_KEY_SPACE && !query.isEmpty()) { query += " "; return true; }
        if (query.isEmpty() && key == GLFW.GLFW_KEY_UP) { selected = (selected + n - 1) % n; return true; }
        if (query.isEmpty() && key == GLFW.GLFW_KEY_DOWN) { selected = (selected + 1) % n; return true; }
        return super.keyPressed(input);
    }
}
