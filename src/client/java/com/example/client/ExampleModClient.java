package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ExampleModClient implements ClientModInitializer {

    private static long ticks = 0;

    @Override
    public void onInitializeClient() {

        ExampleMod.LOGGER.info("AutoBuilder client initialized!");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;

            // Future continuous layer-building controller.
            // Actual block placement will be implemented separately.
        });
    }
}
