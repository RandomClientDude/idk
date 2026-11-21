package com.example.clientclient;

import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}

    public static String name(int code) {
        switch (code) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            case GLFW.GLFW_KEY_GRAVE_ACCENT: return "` (Backtick)";
            case GLFW.GLFW_KEY_MENU: return "Menu";
            case GLFW.GLFW_KEY_PRINT_SCREEN: return "Print Screen";
            case GLFW.GLFW_KEY_SCROLL_LOCK: return "Scroll Lock";
            case GLFW.GLFW_KEY_PAUSE: return "Pause";
            case GLFW.GLFW_KEY_NUM_LOCK: return "Num Lock";
            default: break;
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F12) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) return "Keypad " + (code - GLFW.GLFW_KEY_KP_0);
        String n = GLFW.glfwGetKeyName(code, 0);
        if (n != null) return n.toUpperCase();
        return "Key " + code;
    }
}
