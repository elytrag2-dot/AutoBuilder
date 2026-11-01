package com.example.client.render;

import com.example.autobuilder.engine.SchematicBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class HologramRenderer {
    private static final int MAX_PREVIEW_BLOCKS = 20000;

    private static final int CORRECT = 0xDD55FF77;
    private static final int UNBUILT = 0xDD55CCFF;
    private static final int INCORRECT = 0xDDFF5555;

    private static final int CURRENT_CORRECT = 0xFFFFEE55;
    private static final int CURRENT_UNBUILT = 0xFF55FFFF;
    private static final int CURRENT_INCORRECT = 0xFFFFAA33;

    private HologramRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!PreviewState.isActive()
                    || PreviewState.getSchematic() == null
                    || PreviewState.getAnchor() == null
                    || context.consumers() == null) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gameRenderer == null || minecraft.level == null) {
                return;
            }

            Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
            BlockPos anchor = PreviewState.getAnchor();
            PoseStack matrices = context.matrices();

            var blocks = PreviewState.getSchematic().blocks();
            int count = Math.min(blocks.size(), MAX_PREVIEW_BLOCKS);

            int currentLayer = findCurrentLayer(minecraft, anchor, blocks, count);

            matrices.pushPose();
            matrices.translate(
                    anchor.getX() - camera.x,
                    anchor.getY() - camera.y,
                    anchor.getZ() - camera.z
            );

            VertexConsumer lines = context.consumers()
                    .getBuffer(RenderTypes.LINES);
            PoseStack.Pose pose = matrices.last();

            for (int i = 0; i < count; i++) {
                SchematicBlock block = blocks.get(i);
                Block required = resolveBlock(block.blockId());
                if (required == null) {
                    continue;
                }

                BlockPos target = anchor.offset(
                        block.x(), block.y(), block.z()
                );
                BlockState actual = minecraft.level.getBlockState(target);

                boolean correct = actual.is(required);
                boolean unbuilt = !correct && actual.canBeReplaced();
                boolean isCurrentLayer = block.y() == currentLayer;

                int color;
                if (correct) {
                    color = isCurrentLayer ? CURRENT_CORRECT : CORRECT;
                } else if (unbuilt) {
                    color = isCurrentLayer ? CURRENT_UNBUILT : UNBUILT;
                } else {
                    color = isCurrentLayer ? CURRENT_INCORRECT : INCORRECT;
                }

                drawCube(
                        lines, pose,
                        block.x(), block.y(), block.z(),
                        color
                );
            }

            matrices.popPose();
        });
    }

    private static int findCurrentLayer(
            Minecraft minecraft,
            BlockPos anchor,
            java.util.List<SchematicBlock> blocks,
            int count
    ) {
        int currentLayer = Integer.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            SchematicBlock block = blocks.get(i);
            Block required = resolveBlock(block.blockId());
            if (required == null) {
                continue;
            }

            BlockPos target = anchor.offset(
                    block.x(), block.y(), block.z()
            );

            if (!minecraft.level.getBlockState(target).is(required)) {
                currentLayer = Math.min(currentLayer, block.y());
            }
        }

        return currentLayer;
    }

    private static Block resolveBlock(String blockId) {
        try {
            return BuiltInRegistries.BLOCK
                    .get(Identifier.parse(blockId))
                    .map(reference -> reference.value())
                    .orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void drawCube(
            VertexConsumer lines,
            PoseStack.Pose pose,
            float x, float y, float z,
            int color
    ) {
        float x2 = x + 1;
        float y2 = y + 1;
        float z2 = z + 1;

        edge(lines, pose, x, y, z, x2, y, z, color);
        edge(lines, pose, x2, y, z, x2, y, z2, color);
        edge(lines, pose, x2, y, z2, x, y, z2, color);
        edge(lines, pose, x, y, z2, x, y, z, color);

        edge(lines, pose, x, y2, z, x2, y2, z, color);
        edge(lines, pose, x2, y2, z, x2, y2, z2, color);
        edge(lines, pose, x2, y2, z2, x, y2, z2, color);
        edge(lines, pose, x, y2, z2, x, y2, z, color);

        edge(lines, pose, x, y, z, x, y2, z, color);
        edge(lines, pose, x2, y, z, x2, y2, z, color);
        edge(lines, pose, x2, y, z2, x2, y2, z2, color);
        edge(lines, pose, x, y, z2, x, y2, z2, color);
    }

    private static void edge(
            VertexConsumer lines,
            PoseStack.Pose pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            int color
    ) {
        lines.addVertex(pose, x1, y1, z1)
                .setColor(color)
                .setNormal(pose, 0, 1, 0);
        lines.addVertex(pose, x2, y2, z2)
                .setColor(color)
                .setNormal(pose, 0, 1, 0);
    }
}
