package com.example.client.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class AutoMovementController {

    private static boolean moving;

    private AutoMovementController() {
    }

    public static void stop(LocalPlayer player) {
        moving = false;

        Minecraft client = Minecraft.getInstance();
        client.options.keyUp.setDown(false);

        if (player != null) {
            player.setSprinting(false);
        }
    }

    public static boolean isMoving() {
        return moving;
    }

    public static void moveToward(
            LocalPlayer player,
            BlockPos target
    ) {
        if (player == null || target == null) {
            stop(player);
            return;
        }

        Vec3 destination = Vec3.atCenterOf(target);
        Vec3 difference = destination.subtract(player.position());

        double distance = Math.sqrt(
                difference.x * difference.x
                        + difference.z * difference.z
        );

        if (distance < 2.5) {
            stop(player);
            return;
        }

        float yaw = (float) Math.toDegrees(
                Math.atan2(difference.z, difference.x)
        ) - 90.0F;

        player.setYRot(yaw);
        player.setYHeadRot(yaw);

        Minecraft.getInstance().options.keyUp.setDown(true);
        player.setSprinting(false);

        moving = true;
    }
}
