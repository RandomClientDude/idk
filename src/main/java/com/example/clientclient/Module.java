package com.example.clientclient;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), HUD("HUD"), PLAYER("Player"),
        PARTICLES("Particles"), OVERLAY("Overlays"), INFO("Info"), AUTO("Auto"), FUN("Fun");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public final String name;
    public final String description;
    public final int defaultKey;
    public final Category category;
    private boolean enabled;
    public float anim = 0f;

    protected Module(String name, String description, int defaultKey, Category category) {
        this.name = name;
        this.description = description;
        this.defaultKey = defaultKey;
        this.category = category;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabledSilently(boolean value) { enabled = value; anim = value ? 1f : 0f; }

    public void toggle() {
        enabled = !enabled;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            if (enabled) onEnable(mc); else onDisable(mc);
        }
        Notifications.push(name, enabled);
    }

    protected void onEnable(MinecraftClient mc) {}
    protected void onDisable(MinecraftClient mc) {}
    public void onTick(MinecraftClient mc) {}
}
