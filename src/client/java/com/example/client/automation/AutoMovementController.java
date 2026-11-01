package com.example.client.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class AutoMovementController {
    private static boolean moving;
    private static final float MAX_TURN_PER_TICK = 12.0F;

    private AutoMovementController() {}

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

    public static void moveToward(LocalPlayer player, BlockPos target) {
        if (player == null || target == null) {
            stop(player);
            return;
        }

        Vec3 difference = Vec3.atCenterOf(target).subtract(player.position());
        double distance = Math.sqrt(
                difference.x * difference.x + difference.z * difference.z
        );

        if (distance < 2.5) {
            stop(player);
            return;
        }

        float targetYaw = (float) Math.toDegrees(
                Math.atan2(difference.x, difference.z)
        );

        float relativeYaw = targetYaw - player.getYRot();
        while (relativeYaw <= -180.0F) relativeYaw += 360.0F;
        while (relativeYaw > 180.0F) relativeYaw -= 360.0F;

        float turn = Math.max(
                -MAX_TURN_PER_TICK,
                Math.min(MAX_TURN_PER_TICK, relativeYaw)
        );

        player.setYRot(player.getYRot() + turn);
        player.setYHeadRot(player.getYRot());

        releaseKeys();
        var options = Minecraft.getInstance().options;

        if (Math.abs(relativeYaw) <= 90.0F) {
            options.keyUp.setDown(true);
        } else {
            options.keyDown.setDown(true);
        }

        if (target.getY() > player.blockPosition().getY()
                && distance < 4.0) {
            options.keyJump.setDown(true);
        }

        player.setSprinting(false);
        moving = true;
    }
}
