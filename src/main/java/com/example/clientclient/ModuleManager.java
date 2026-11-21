package com.example.clientclient;

import com.example.clientclient.Module.Category;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Module, KeyBinding> KEYS = new HashMap<>();
    private static final int NONE = GLFW.GLFW_KEY_UNKNOWN;
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("clientclient", "main"));

    private ModuleManager() {}

    public static Module get(String name) {
        for (Module m : MODULES) if (m.name.equals(name)) return m;
        return null;
    }

    public static boolean on(String name) {
        Module m = get(name);
        return m != null && m.isEnabled();
    }

    public static List<Module> inCategory(Category c) {
        return MODULES.stream().filter(m -> m.category == c).toList();
    }

    private static void setGlow(MinecraftClient mc, Class<?> type, boolean glow) {
        for (Entity e : mc.world.getEntities())
            if (type.isInstance(e) && e != mc.player) e.setGlowing(glow);
    }

    private static boolean moving(MinecraftClient mc) {
        return Compat.moving(mc);
    }

    public static void init() {
        // =================== COMBAT ===================
        add(new Module("MobAura", "Auto-attacks hostile mobs within 4 blocks", NONE, Category.COMBAT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player.getAttackCooldownProgress(0f) < 1f) return;
                LivingEntity best = null;
                double bestDist = 16.0;
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof Monster && e instanceof LivingEntity le && le.isAlive()) {
                        double d = mc.player.squaredDistanceTo(e);
                        if (d < bestDist) { bestDist = d; best = le; }
                    }
                }
                if (best != null) {
                    mc.interactionManager.attackEntity(mc.player, best);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        });

        add(new Module("TriggerBot", "Attacks whatever your crosshair is on", NONE, Category.COMBAT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player.getAttackCooldownProgress(0f) < 1f) return;
                if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity) {
                    mc.interactionManager.attackEntity(mc.player, hit.getEntity());
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        });

        add(new Module("AutoTotem", "Keeps a totem in your offhand", NONE, Category.COMBAT) {
            private int timer;
            @Override public void onTick(MinecraftClient mc) {
                if (++timer < 5) return;
                timer = 0;
                if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
                for (int i = 0; i < 36; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                        int slot = i < 9 ? 36 + i : i;
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
                        return;
                    }
                }
            }
        });

        // =================== MOVEMENT ===================
        add(new Module("Flight", "Creative-style flying", GLFW.GLFW_KEY_G, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                mc.player.getAbilities().allowFlying = true;
                mc.player.getAbilities().setFlySpeed(0.08f);
            }
            @Override protected void onDisable(MinecraftClient mc) {
                if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                    mc.player.getAbilities().allowFlying = false;
                    mc.player.getAbilities().flying = false;
                }
            }
        });

        add(new Module("Speed", "Faster ground movement", GLFW.GLFW_KEY_K, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player.isOnGround() && moving(mc)) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x * 1.15, v.y, v.z * 1.15);
                }
            }
        });

        add(new Module("AutoSprint", "Always sprint when moving", GLFW.GLFW_KEY_J, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                if (Compat.forward(mc) && !mc.player.horizontalCollision)
                    mc.player.setSprinting(true);
            }
        });

        add(new Module("Glide", "Fall slowly like a feather", NONE, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                Vec3d v = mc.player.getVelocity();
                if (!mc.player.isOnGround() && !mc.player.getAbilities().flying && v.y < -0.1)
                    mc.player.setVelocity(v.x, -0.1, v.z);
            }
        });

        add(new Module("WaterWalk", "Float on top of water", NONE, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player.isTouchingWater() && !mc.options.sneakKey.isPressed()) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x, 0.12, v.z);
                }
            }
        });

        add(new Module("Step", "Walk up full blocks", NONE, Category.MOVEMENT) {
            private double old = 0.6;
            @Override protected void onEnable(MinecraftClient mc) {
                EntityAttributeInstance a = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
                if (a != null) { old = a.getBaseValue(); a.setBaseValue(1.0); }
            }
            @Override protected void onDisable(MinecraftClient mc) {
                EntityAttributeInstance a = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
                if (a != null) a.setBaseValue(old);
            }
        });

        add(new Module("BHop", "Automatic bunny hopping", NONE, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player.isOnGround() && moving(mc)) mc.player.jump();
            }
        });

        add(new Module("Spider", "Climb walls", NONE, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player.horizontalCollision && Compat.forward(mc)) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x, 0.2, v.z);
                }
            }
        });

        add(new Module("HighJump", "Jump much higher", NONE, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.options.jumpKey.isPressed() && mc.player.isOnGround()) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x, 0.75, v.z);
                }
            }
        });

        add(new Module("Jetpack", "Hold jump to fly with flames", NONE, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround()) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x, Math.min(v.y + 0.08, 0.5), v.z);
                    for (int i = 0; i < 3; i++)
                        Compat.particle(mc, ParticleTypes.FLAME, mc.player.getX() + (Math.random() - 0.5) * 0.3,
                                mc.player.getY(), mc.player.getZ() + (Math.random() - 0.5) * 0.3, 0, -0.15, 0);
                }
            }
        });

        add(new Module("FastFall", "Fall faster", NONE, Category.MOVEMENT) {
            @Override public void onTick(MinecraftClient mc) {
                Vec3d v = mc.player.getVelocity();
                if (!mc.player.isOnGround() && v.y < -0.1 && !mc.player.getAbilities().flying)
                    mc.player.setVelocity(v.x, Math.max(v.y * 1.2, -3.0), v.z);
            }
        });

        // =================== RENDER ===================
        add(new Module("Fullbright", "See in the dark", GLFW.GLFW_KEY_H, Category.RENDER) {
            @Override public void onTick(MinecraftClient mc) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false));
            }
            @Override protected void onDisable(MinecraftClient mc) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });

        add(new Module("ESP", "Glowing outlines on players", GLFW.GLFW_KEY_L, Category.RENDER) {
            @Override public void onTick(MinecraftClient mc) { setGlow(mc, PlayerEntity.class, true); }
            @Override protected void onDisable(MinecraftClient mc) { setGlow(mc, PlayerEntity.class, false); }
        });

        add(new Module("MobESP", "Glowing outlines on hostile mobs", NONE, Category.RENDER) {
            @Override public void onTick(MinecraftClient mc) { setGlow(mc, Monster.class, true); }
            @Override protected void onDisable(MinecraftClient mc) { setGlow(mc, Monster.class, false); }
        });

        add(new Module("ItemESP", "Glowing outlines on dropped items", NONE, Category.RENDER) {
            @Override public void onTick(MinecraftClient mc) { setGlow(mc, ItemEntity.class, true); }
            @Override protected void onDisable(MinecraftClient mc) { setGlow(mc, ItemEntity.class, false); }
        });

        add(new Module("Zoom", "Optifine-style zoom", GLFW.GLFW_KEY_C, Category.RENDER) {
            private int oldFov = 70;
            @Override protected void onEnable(MinecraftClient mc) {
                oldFov = mc.options.getFov().getValue();
                mc.options.getFov().setValue(30);
            }
            @Override protected void onDisable(MinecraftClient mc) { mc.options.getFov().setValue(oldFov); }
        });

        add(new Module("WideFOV", "Maximum field of view", NONE, Category.RENDER) {
            private int oldFov = 70;
            @Override protected void onEnable(MinecraftClient mc) {
                oldFov = mc.options.getFov().getValue();
                mc.options.getFov().setValue(110);
            }
            @Override protected void onDisable(MinecraftClient mc) { mc.options.getFov().setValue(oldFov); }
        });

        add(new Module("Trails", "Rainbow particle trail behind you", NONE, Category.RENDER) {
            @Override public void onTick(MinecraftClient mc) {
                Vec3d v = mc.player.getVelocity();
                if (Math.hypot(v.x, v.z) < 0.04) return;
                int c = Colors.accent(mc.player.age * 6);
                for (int i = 0; i < 2; i++)
                    Compat.particle(mc, new DustParticleEffect((c & 0xFFFFFF), 1.3f),
                            mc.player.getX() + (Math.random() - 0.5) * 0.4, mc.player.getY() + 0.1 + Math.random() * 0.3,
                            mc.player.getZ() + (Math.random() - 0.5) * 0.4, 0, 0, 0);
            }
        });

        add(new Module("Halo", "Glowing ring above your head", NONE, Category.RENDER) {
            @Override public void onTick(MinecraftClient mc) {
                double base = mc.player.age * 0.2;
                for (int k = 0; k < 6; k++) {
                    double a = base + k * Math.PI / 3;
                    Compat.particle(mc, ParticleTypes.END_ROD, mc.player.getX() + Math.cos(a) * 0.4,
                            mc.player.getY() + mc.player.getHeight() + 0.3, mc.player.getZ() + Math.sin(a) * 0.4, 0, 0, 0);
                }
            }
        });

        add(new Module("Orbit", "Colored orbs circle around you", NONE, Category.RENDER) {
            @Override public void onTick(MinecraftClient mc) {
                double base = mc.player.age * 0.15;
                for (int k = 0; k < 3; k++) {
                    double a = base + k * Math.PI * 2 / 3;
                    int c = Colors.accent(k * 120 + mc.player.age * 4);
                    Compat.particle(mc, new DustParticleEffect((c & 0xFFFFFF), 1.6f),
                            mc.player.getX() + Math.cos(a) * 1.1, mc.player.getY() + 1.0 + Math.sin(base * 0.7 + k) * 0.5,
                            mc.player.getZ() + Math.sin(a) * 1.1, 0, 0, 0);
                }
            }
        });

        // =================== HUD (drawn in Hud.java) ===================
        add(new Module("Radar", "Mini radar with nearby players", NONE, Category.HUD) {});
        add(new Module("Keystrokes", "WASD key display", NONE, Category.HUD) {});
        add(new Module("Crosshair+", "Animated crosshair", NONE, Category.HUD) {});
        add(new Module("ArmorHUD", "Armor and durability on screen", NONE, Category.HUD) {});
        add(new Module("TargetHUD", "Health bar of the mob you aim at", NONE, Category.HUD) {});
        add(new Module("Compass", "Heading strip at the top", NONE, Category.HUD) {});
        add(new Module("PotionHUD", "Active effects with timers", NONE, Category.HUD) {});
        add(new Module("Speedometer", "Blocks per second readout", NONE, Category.HUD) {});
        add(new Module("Snowfall", "Falling snow over your screen", NONE, Category.HUD) {});
        add(new Module("BorderGlow", "Glowing screen edges", NONE, Category.HUD) {});

        // =================== PLAYER ===================
        add(new Module("AutoEat", "Eats food when hungry", NONE, Category.PLAYER) {
            private int prev = -1;
            private boolean eating;
            @Override public void onTick(MinecraftClient mc) {
                int hunger = mc.player.getHungerManager().getFoodLevel();
                int food = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack s = mc.player.getInventory().getStack(i);
                    if (s.get(DataComponentTypes.FOOD) != null && !s.isOf(Items.ROTTEN_FLESH) && !s.isOf(Items.SPIDER_EYE)
                            && !s.isOf(Items.POISONOUS_POTATO) && !s.isOf(Items.PUFFERFISH)) { food = i; break; }
                }
                if (hunger <= 14 && food >= 0) {
                    if (!eating) prev = Compat.getSlot(mc);
                    Compat.setSlot(mc, food);
                    mc.options.useKey.setPressed(true);
                    eating = true;
                } else if (eating) stop(mc);
            }
            private void stop(MinecraftClient mc) {
                mc.options.useKey.setPressed(false);
                if (prev >= 0) Compat.setSlot(mc, prev);
                prev = -1;
                eating = false;
            }
            @Override protected void onDisable(MinecraftClient mc) { if (eating) stop(mc); }
        });

        add(new Module("AutoRespawn", "Respawns instantly when you die", NONE, Category.PLAYER) {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.currentScreen instanceof DeathScreen) mc.player.requestRespawn();
            }
        });

        add(new Module("AutoWalk", "Holds forward for you", NONE, Category.PLAYER) {
            @Override public void onTick(MinecraftClient mc) { mc.options.forwardKey.setPressed(true); }
            @Override protected void onDisable(MinecraftClient mc) { mc.options.forwardKey.setPressed(false); }
        });

        add(new Module("AutoSneak", "Always sneaking", NONE, Category.PLAYER) {
            @Override public void onTick(MinecraftClient mc) { mc.options.sneakKey.setPressed(true); }
            @Override protected void onDisable(MinecraftClient mc) { mc.options.sneakKey.setPressed(false); }
        });

        add(new Module("AntiAFK", "Jumps and turns so you don't get kicked", NONE, Category.PLAYER) {
            private int timer;
            @Override public void onTick(MinecraftClient mc) {
                if (++timer >= 300) {
                    timer = 0;
                    mc.player.jump();
                    mc.player.setYaw(mc.player.getYaw() + (float) ((Math.random() - 0.5) * 20));
                }
            }
        });

        add(new Module("Spin", "Spin around (visual fun)", NONE, Category.PLAYER) {
            @Override public void onTick(MinecraftClient mc) { mc.player.setYaw(mc.player.getYaw() + 18f); }
        });

        add(new Module("NoFall", "Cancels fall damage", NONE, Category.PLAYER) {
            @Override public void onTick(MinecraftClient mc) {
                if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.5)
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
            }
        });

        add(new Module("Panic", "Instantly turns every module off", GLFW.GLFW_KEY_END, Category.PLAYER) {
            @Override protected void onEnable(MinecraftClient mc) {
                for (Module m : MODULES) if (m != this && m.isEnabled()) m.toggle();
                setEnabledSilently(false);
                mc.setScreen(null);
            }
        });

        ExtraModules.init();
        MoreModules.init();
        Modules3.init();
        for (String n : new String[]{"KeyHint", "TopBar", "SlotHighlight", "HotbarGlow", "WelcomeBanner", "Watermark", "ArrayList", "CPS", "Hitmarker", "Ping", "Radar", "Keystrokes", "Crosshair+", "ArmorHUD", "TargetHUD", "Compass", "PotionHUD", "Speedometer"})
            get(n).setEnabledSilently(true);
    }

    static void addNoKey(Module m) {
        MODULES.add(m);
    }

    static void add(Module m) {
        MODULES.add(m);
        KEYS.put(m, KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.clientclient." + m.name.toLowerCase().replace("+", "plus"), m.defaultKey, CATEGORY)));
    }

    public static void tick(MinecraftClient mc) {
        for (Module m : MODULES) {
            KeyBinding key = KEYS.get(m);
            if (key != null) while (key.wasPressed()) m.toggle();
            m.anim += ((m.isEnabled() ? 1f : 0f) - m.anim) * 0.2f;
            if (m.isEnabled()) m.onTick(mc);
        }
    }
}
