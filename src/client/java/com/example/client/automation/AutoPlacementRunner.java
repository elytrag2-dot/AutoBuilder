package com.example.client.automation;


import com.example.autobuilder.engine.BuildQueue;
import com.example.autobuilder.engine.SchematicBlock;
import com.example.autobuilder.engine.SchematicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AutoPlacementRunner {

    private static BuildQueue queue;
    private static BlockPos anchor;

    private static BlockPos pendingPosition;
    private static Block pendingBlock;
    private static int pendingIndex = -1;
    private static int pendingTicks;

    private static int cooldown;
    private static boolean running;
    private static String status = "Idle";

    private AutoPlacementRunner() {
    }

    public static void start(SchematicData schematic, BlockPos origin) {
        if (schematic == null || schematic.blocks().isEmpty()) {
            status = "Schematic is empty";
            return;
        }

        if (origin == null) {
            status = "Preview the schematic to set its origin";
            return;
        }

        queue = new BuildQueue(schematic);
        anchor = origin;
        pendingPosition = null;
        pendingBlock = null;
        pendingIndex = -1;
        pendingTicks = 0;
        cooldown = 0;
        running = true;
        status = "Automatic building started";
    }

    public static void pause() {
        running = false;
        status = "Placement paused";
    }

    public static void resume() {
        if (queue != null && !queue.isStopped()) {
            running = true;
            status = "Placement resumed";
        }
    }

    public static void stop() {
        running = false;

        if (queue != null) {
            queue.stop();
        }

        pendingPosition = null;
        pendingBlock = null;
        pendingIndex = -1;
        status = "Placement stopped";
    }

    public static boolean isRunning() {
        return running;
    }

    public static String getStatus() {
        return status;
    }

    public static int getCompletedCount() {
        return queue == null ? 0 : queue.getCompletedCount();
    }

    public static int getTotalCount() {
        return queue == null ? 0 : queue.getTotalCount();
    }

    public static void tick(Minecraft client) {
        if (!running || queue == null) {
            return;
        }

        if (client.player == null || client.level == null
                || client.gameMode == null) {
            status = "Player or world unavailable";
            running = false;
            return;
        }

        if (client.screen != null) {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        LocalPlayer player = client.player;

        if (pendingPosition != null) {
            BlockState actual =
                    client.level.getBlockState(pendingPosition);

            if (actual.is(pendingBlock)) {
                queue.markCompleted(pendingIndex);

                pendingPosition = null;
                pendingBlock = null;
                pendingIndex = -1;
                pendingTicks = 0;
                cooldown = 3;

                status = "Placed block "
                        + queue.getCompletedCount()
                        + "/" + queue.getTotalCount();
                return;
            }

            pendingTicks++;

            if (pendingTicks > 20) {
                status = "Placement not confirmed; stopped safely";
                running = false;
            }

            return;
        }

        BuildQueue.QueueEntry entry = queue.getNextEntry();

        if (entry == null) {
            running = false;
            status = "Build plan completed";
            return;
        }

        SchematicBlock schematicBlock = entry.block();

        BlockPos target = anchor.offset(
                schematicBlock.x(),
                schematicBlock.y(),
                schematicBlock.z()
        );

        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse(schematicBlock.blockId())
        );

        if (block.defaultBlockState().isAir()) {
            status = "Unsupported block: " + schematicBlock.blockId();
            running = false;
            return;
        }

        if (client.level.getBlockState(target).is(block)) {
            queue.markCompleted(entry.index());
            return;
        }

        if (!client.level.getBlockState(target).canBeReplaced()) {
            status = "Target obstructed at " + target;
            running = false;
            return;
        }

        int hotbarSlot = findHotbarBlock(client, player, block);

        if (hotbarSlot < 0) {
            status = "Block missing from hotbar: "
                    + schematicBlock.blockId();
            running = false;
            return;
        }

        player.getInventory().setSelectedSlot(hotbarSlot);

        for (Direction face : Direction.values()) {
            BlockPos support = target.relative(face.getOpposite());

            BlockState supportState =
                    client.level.getBlockState(support);

            if (supportState.getCollisionShape(
                    client.level, support
            ).isEmpty()
                    || !supportState.isFaceSturdy(
                            client.level,
                            support,
                            face
                    )) {
                continue;
            }

            Vec3 hitPosition = Vec3.atCenterOf(support).add(
                    face.getStepX() * 0.5,
                    face.getStepY() * 0.5,
                    face.getStepZ() * 0.5
            );

            if (player.getEyePosition().distanceToSqr(hitPosition)
                    > 25.0) {
                continue;
            }

            PlacementAimController.aimAt(player, hitPosition);

            BlockHitResult hit = new BlockHitResult(
                    hitPosition,
                    face,
                    support,
                    false
            );

            InteractionResult result = client.gameMode.useItemOn(
                    player,
                    InteractionHand.MAIN_HAND,
                    hit
            );

            if (result.consumesAction()) {
                pendingPosition = target;
                pendingBlock = block;
                pendingIndex = entry.index();
                pendingTicks = 0;
                status = "Waiting for placement confirmation";
                return;
            }
        }

        status = "No reachable support face for " + target;
        running = false;
    }

    private static int findHotbarBlock(
            Minecraft client,
            LocalPlayer player,
            Block requiredBlock
    ) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);

            if (!stack.isEmpty()
                    && stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() == requiredBlock) {
                return slot;
            }
        }

        int selectedSlot = player.getInventory().getSelectedSlot();

        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);

            if (!stack.isEmpty()
                    && stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() == requiredBlock) {

                client.gameMode.handleInventoryMouseClick(
                        player.inventoryMenu.containerId,
                        slot,
                        selectedSlot,
                        ClickType.SWAP,
                        player
                );

                return selectedSlot;
            }
        }

        return -1;
    }
}
