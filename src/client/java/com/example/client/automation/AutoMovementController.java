package com.example.client.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class AutoMovementController {

    private static boolean moving;

    private AutoMovementController() {
    }

    private static void releaseKeys() {
        var options = Minecraft.getInstance().options;
        options.keyUp.setDown(false);
        options.keyDown.setDown(false);
        options.keyLeft.setDown(false);
        options.keyRight.setDown(false);
        options.keyJump.setDown(false);
    }

    public static void stop(LocalPlayer player) {
        releaseKeys();
        moving = false;

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

        Vec3 difference = Vec3.atCenterOf(target)
                .subtract(player.position());

        double distance = Math.sqrt(
                difference.x * difference.x
                        + difference.z * difference.z
        );

        if (distance < 2.5) {
            stop(player);
            return;
        }

        double targetYaw = Math.toDegrees(
                Math.atan2(difference.x, difference.z)
        );

        double relativeYaw = targetYaw - player.getYRot();

        while (relativeYaw <= -180) relativeYaw += 360;
        while (relativeYaw > 180) relativeYaw -= 360;

        releaseKeys();

        var options = Minecraft.getInstance().options;

        if (Math.abs(relativeYaw) <= 45) {
            options.keyUp.setDown(true);
        } else if (Math.abs(relativeYaw) >= 135) {
            options.keyDown.setDown(true);
        } else if (relativeYaw > 0) {
            options.keyRight.setDown(true);
        } else {
            options.keyLeft.setDown(true);
        }

        if (target.getY() > player.blockPosition().getY()
                && distance < 4.0) {
            options.keyJump.setDown(true);
        }

        player.setSprinting(false);
        moving = true;
    }
}
