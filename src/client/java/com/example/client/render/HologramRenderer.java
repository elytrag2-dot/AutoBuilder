package com.example.client.render;

import com.example.autobuilder.engine.SchematicBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

public final class HologramRenderer {

    private static final int MAX_PREVIEW_BLOCKS = 20000;
    private static final int COLOR = 0xDD55CCFF;

    private HologramRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!PreviewState.isActive()
                    || PreviewState.getSchematic() == null
                    || PreviewState.getAnchor() == null) {
                return;
            }

            if (context.consumers() == null) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.gameRenderer == null) {
                return;
            }

            Vec3 camera = minecraft.gameRenderer
                    .getMainCamera().position();

            var anchor = PreviewState.getAnchor();
            PoseStack matrices = context.matrices();

            matrices.pushPose();
            matrices.translate(
                    anchor.getX() - camera.x,
                    anchor.getY() - camera.y,
                    anchor.getZ() - camera.z
            );

            VertexConsumer lines = context.consumers()
                    .getBuffer(RenderTypes.LINES);

            PoseStack.Pose pose = matrices.last();

            var blocks = PreviewState.getSchematic().blocks();
            int count = Math.min(blocks.size(), MAX_PREVIEW_BLOCKS);

            for (int i = 0; i < count; i++) {
                SchematicBlock block = blocks.get(i);

                drawCube(
                        lines,
                        pose,
                        block.x(),
                        block.y(),
                        block.z()
                );
            }

            matrices.popPose();
        });
    }

    private static void drawCube(
            VertexConsumer lines,
            PoseStack.Pose pose,
            float x,
            float y,
            float z
    ) {
        float x2 = x + 1;
        float y2 = y + 1;
        float z2 = z + 1;

        // Bottom face
        edge(lines, pose, x, y, z, x2, y, z);
        edge(lines, pose, x2, y, z, x2, y, z2);
        edge(lines, pose, x2, y, z2, x, y, z2);
        edge(lines, pose, x, y, z2, x, y, z);

        // Top face
        edge(lines, pose, x, y2, z, x2, y2, z);
        edge(lines, pose, x2, y2, z, x2, y2, z2);
        edge(lines, pose, x2, y2, z2, x, y2, z2);
        edge(lines, pose, x, y2, z2, x, y2, z);

        // Vertical edges
        edge(lines, pose, x, y, z, x, y2, z);
        edge(lines, pose, x2, y, z, x2, y2, z);
        edge(lines, pose, x2, y, z2, x2, y2, z2);
        edge(lines, pose, x, y, z2, x, y2, z2);
    }

    private static void edge(
            VertexConsumer lines,
            PoseStack.Pose pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2
    ) {
        lines.addVertex(pose, x1, y1, z1)
                .setColor(COLOR)
                .setNormal(pose, 0, 1, 0);

        lines.addVertex(pose, x2, y2, z2)
                .setColor(COLOR)
                .setNormal(pose, 0, 1, 0);
    }
}
