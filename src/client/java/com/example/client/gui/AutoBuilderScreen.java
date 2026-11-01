package com.example.client.gui;

import com.example.autobuilder.engine.BuildSession;
import com.example.autobuilder.engine.InventoryAnalyzer;
import com.example.autobuilder.engine.SchematicData;
import com.example.autobuilder.engine.SchematicLoader;
import com.example.client.inventory.PlayerInventoryScanner;
import com.example.client.automation.AutoPlacementRunner;
import com.example.client.render.PreviewState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class AutoBuilderScreen extends Screen {

    private String status = "No schematic loaded";
    private SchematicData schematic;
    private BuildSession buildSession;

    private int inventoryRequired;
    private int inventoryAvailable;
    private int inventoryMissing;
    private boolean inventoryChecked;

    public AutoBuilderScreen() {
        super(Component.literal("AutoBuilder"));
    }

    Path getSchematicFolder() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("autobuilder")
                .resolve("schematics");
    }

    void loadSelectedSchematic(Path file) {
        try {
            schematic = SchematicLoader.load(file);
            buildSession = null;
            inventoryChecked = false;
            status = "Loaded: " + file.getFileName();
        } catch (IOException | RuntimeException exception) {
            status = "Load failed: " + exception.getMessage();
        }
    }

    private void prepareBuild() {
        Minecraft minecraft = Minecraft.getInstance();

        if (schematic == null) {
            status = "Load a schematic first";
            return;
        }

        if (minecraft.player == null) {
            status = "Enter a world first";
            return;
        }

        if (PreviewState.getAnchor() == null
                || !PreviewState.isActive()) {
            status = "Preview the schematic first to set its origin";
            return;
        }

        buildSession = new BuildSession(
                schematic,
                PlayerInventoryScanner.scan(minecraft.player)
        );

        var result = buildSession.getValidation();

        if (!result.valid()) {
            status = result.message()
                    + " | Missing: " + result.missingBlockCount();
            return;
        }

        AutoPlacementRunner.start(
                schematic,
                PreviewState.getAnchor()
        );

        status = "Automatic placement started";
    }

    private void checkInventory() {
        Minecraft minecraft = Minecraft.getInstance();

        if (schematic == null) {
            status = "Load a schematic first";
            return;
        }

        if (minecraft.player == null) {
            status = "Enter a world first";
            return;
        }

        Map<String, Integer> required =
                InventoryAnalyzer.countRequired(schematic);

        Map<String, Integer> available =
                PlayerInventoryScanner.scan(minecraft.player);

        Map<String, Integer> missing =
                InventoryAnalyzer.calculateMissing(required, available);

        inventoryRequired = required.values().stream()
                .mapToInt(Integer::intValue).sum();

        inventoryMissing = missing.values().stream()
                .mapToInt(Integer::intValue).sum();

        inventoryAvailable = inventoryRequired - inventoryMissing;
        inventoryChecked = true;

        status = inventoryMissing == 0
                ? "All required blocks available!"
                : "You need more building blocks";
    }

    private void pauseBuild() {
        if (buildSession == null) {
            status = "Prepare a build plan first";
            return;
        }

        AutoPlacementRunner.pause();
        status = AutoPlacementRunner.getStatus();
    }

    private void resumeBuild() {
        if (buildSession == null) {
            status = "Prepare a build plan first";
            return;
        }

        AutoPlacementRunner.resume();
        status = AutoPlacementRunner.getStatus();
    }

    private void markNextPlaced() {
        if (buildSession == null) {
            status = "Prepare a build plan first";
            return;
        }

        var queue = buildSession.getQueue();
        var next = queue.getNextEntry();

        if (next == null) {
            status = queue.isPaused()
                    ? "Resume the plan first"
                    : "All planned blocks are marked complete";
            return;
        }

        queue.markCompleted(next.index());
        status = "Marked " + next.block().blockId() + " as placed";
    }

    private void resetBuild() {
        AutoPlacementRunner.stop();
        buildSession = null;
        inventoryChecked = false;
        status = "Build session reset";
    }

    @Override
    protected void init() {
        int left = width / 2 - 140;
        int top = height / 2 - 75;

        addRenderableWidget(Button.builder(
                Component.literal("Load Schematic"),
                button -> Minecraft.getInstance().setScreen(
                        new SchematicSelectionScreen(this)
                )
        ).bounds(left, top, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Preview"),
                button -> {
                    if (schematic == null) {
                        status = "Load a schematic first";
                        return;
                    }

                    var player = Minecraft.getInstance().player;

                    if (player == null) {
                        status = "Enter a world first";
                        return;
                    }

                    float yaw = player.getYRot();
                    double dx = -Math.sin(Math.toRadians(yaw)) * 3;
                    double dz = Math.cos(Math.toRadians(yaw)) * 3;

                    BlockPos anchor = BlockPos.containing(
                            player.getX() + dx,
                            player.getY(),
                            player.getZ() + dz
                    );

                    PreviewState.start(schematic, anchor);
                    status = "3D hologram preview active";
                }
        ).bounds(left + 140, top, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Build Plan"),
                button -> prepareBuild()
        ).bounds(left, top + 32, 90, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Pause"),
                button -> pauseBuild()
        ).bounds(left + 95, top + 32, 90, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Resume"),
                button -> resumeBuild()
        ).bounds(left + 190, top + 32, 90, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Check Inventory"),
                button -> checkInventory()
        ).bounds(left, top + 64, 90, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Placement Status"),
                button -> status = AutoPlacementRunner.getStatus()
        ).bounds(left + 95, top + 64, 90, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Stop / Reset"),
                button -> resetBuild()
        ).bounds(left + 190, top + 64, 90, 22).build());
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        graphics.fill(0, 0, width, height, 0xCC080D18);

        int left = width / 2 - 155;
        int top = height / 2 - 125;

        graphics.fill(
                left, top, left + 310, top + 250,
                0xFF151E30
        );

        graphics.drawCenteredString(
                font,
                Component.literal("AUTOBUILDER"),
                width / 2, top + 12, 0xFF55CCFF
        );

        graphics.drawCenteredString(
                font,
                Component.literal("Schematic Assistant"),
                width / 2, top + 29, 0xFFAAAAAA
        );

        graphics.drawCenteredString(
                font,
                Component.literal(status),
                width / 2, top + 153, 0xFFFFFFFF
        );

        if (schematic != null) {
            graphics.drawCenteredString(
                    font,
                    Component.literal(
                            schematic.width() + " x "
                                    + schematic.height() + " x "
                                    + schematic.length()
                    ),
                    width / 2, top + 169, 0xFF55CCFF
            );

            graphics.drawCenteredString(
                    font,
                    Component.literal(
                            "Blocks: " + schematic.getBlockCount()
                    ),
                    width / 2, top + 185, 0xFF55FF99
            );
        }

        if (inventoryChecked) {
            graphics.drawCenteredString(
                    font,
                    Component.literal(
                            "Materials: " + inventoryAvailable
                                    + "/" + inventoryRequired
                                    + " | Missing: " + inventoryMissing
                    ),
                    width / 2, top + 207,
                    inventoryMissing == 0 ? 0xFF55FF99 : 0xFFFF7777
            );
        }

        if (buildSession != null) {
            int total = AutoPlacementRunner.getTotalCount();
            int completed = AutoPlacementRunner.getCompletedCount();

            double progress = total == 0
                    ? 0.0
                    : completed * 100.0 / total;

            graphics.drawCenteredString(
                    font,
                    Component.literal(
                            "Progress: " + completed + "/" + total
                                    + " (" + String.format(
                                            java.util.Locale.ROOT,
                                            "%.1f",
                                            progress
                                    ) + "%)"
                    ),
                    width / 2, top + 228, 0xFFFFFFFF
            );
        }

        super.render(graphics, mouseX, mouseY, delta);
    }
}
