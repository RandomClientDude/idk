package com.example.clientclient;

import java.awt.Color;

public final class Colors {
    private Colors() {}

    public static int theme = 0;
    public static final String[] THEMES = {"Rainbow", "Ocean", "Sunset", "Neon", "Blood", "Mint"};

    /** Main accent color for the current theme, animated over time. */
    public static int accent(int offset) {
        switch (theme) {
            case 1:  return pulse(0x00C6FF, 0x7F5FFF, offset);
            case 2:  return pulse(0xFF512F, 0xFFC837, offset);
            case 3:  return pulse(0xFF00E5, 0x00FFC8, offset);
            case 4:  return pulse(0xFF0040, 0x8A0020, offset);
            case 5:  return pulse(0x9BFFB0, 0x2FD0FF, offset);
            default: return rainbow(offset);
        }
    }

    public static int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() / 18 + offset) % 360) / 360f;
        return Color.HSBtoRGB(hue, 0.65f, 1f) | 0xFF000000;
    }

    private static int pulse(int a, int b, int offset) {
        float t = (float) (Math.sin((System.currentTimeMillis() + offset * 12L) / 500.0) * 0.5 + 0.5);
        return lerp(a | 0xFF000000, b | 0xFF000000, t) | 0xFF000000;
    }

    public static org.joml.Vector3f vec(int c) {
        return new org.joml.Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
    }

    public static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, aa = (a >>> 24);
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255, ba = (b >>> 24);
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
