package com.example.clientclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class ClientClient implements ClientModInitializer {
    private static boolean menuWasDown;

    @Override
    public void onInitializeClient() {
        Config.load();
        ModuleManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // The menu key is stored in our own config, so it can be changed from inside the menu.
            boolean down = mc.currentScreen == null && Compat.isKeyDown(mc, Config.menuKey);
            if (down && !menuWasDown && mc.player != null) mc.setScreen(new ClickGui());
            menuWasDown = down;

            if (mc.player == null) return;
            ExtraModules.globalTick(mc);
            ModuleManager.tick(mc);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world instanceof ClientWorld) ExtraModules.onAttack(entity);
            return ActionResult.PASS;
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of("clientclient", "hud"), (ctx, tickCounter) -> Hud.render(ctx));

        TitleFx.register();
    }
}
