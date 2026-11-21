package com.example.clientclient;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Saved settings: the menu key and the theme. Stored in config/clientclient.properties. */
public final class Config {
    public static int menuKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private Config() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("clientclient.properties");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(f)) { p.load(in); }
            menuKey = Integer.parseInt(p.getProperty("menuKey", String.valueOf(menuKey)));
            int t = Integer.parseInt(p.getProperty("theme", "0"));
            Colors.theme = Math.max(0, Math.min(Colors.THEMES.length - 1, t));
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("menuKey", String.valueOf(menuKey));
            p.setProperty("theme", String.valueOf(Colors.theme));
            try (OutputStream out = Files.newOutputStream(file())) { p.store(out, "ClientClient"); }
        } catch (Exception ignored) {
        }
    }
}
