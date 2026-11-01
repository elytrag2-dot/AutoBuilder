package com.example.client.automation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class PlacementAimController {

    private PlacementAimController() {
    }

    public static void aimAt(LocalPlayer player, Vec3 target) {
        if (player == null || target == null) {
            return;
        }

        Vec3 eyes = player.getEyePosition();
        Vec3 difference = target.subtract(eyes);

        double horizontal = Math.sqrt(
                difference.x * difference.x
                        + difference.z * difference.z
        );

        float yaw = (float) Math.toDegrees(
                Math.atan2(difference.z, difference.x)
        ) - 90.0F;

        float pitch = (float) -Math.toDegrees(
                Math.atan2(difference.y, horizontal)
        );

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
    }
}
