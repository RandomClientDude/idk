package com.example.clientclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;

/** Version-sensitive calls are kept in one place so they are easy to fix. */
public final class Compat {
    private Compat() {}

    public static boolean forward(MinecraftClient mc) {
        return mc.options.forwardKey.isPressed();
    }

    public static boolean moving(MinecraftClient mc) {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
    }

    public static boolean isKeyDown(MinecraftClient mc, int code) {
        return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    public static int getSlot(MinecraftClient mc) {
        return mc.player.getInventory().getSelectedSlot();
    }

    public static void setSlot(MinecraftClient mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    public static void particle(MinecraftClient mc, ParticleEffect p, double x, double y, double z, double vx, double vy, double vz) {
        mc.world.addParticleClient(p, x, y, z, vx, vy, vz);
    }
}
