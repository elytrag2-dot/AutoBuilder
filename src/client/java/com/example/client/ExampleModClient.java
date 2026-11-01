package com.example.client;

import com.example.ExampleMod;
import com.example.client.gui.AutoBuilderScreen;
import com.example.client.automation.AutoPlacementRunner;
import com.example.client.render.HologramRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        ExampleMod.LOGGER.info("AutoBuilder client initialized!");

        HologramRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(
                AutoPlacementRunner::tick
        );

        openGuiKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.autobuilder.open_gui",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        KeyMapping.Category.MISC
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new AutoBuilderScreen());
                }
            }
        });
    }
}
