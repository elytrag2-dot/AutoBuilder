package com.example.client.gui;

import com.example.autobuilder.engine.InventoryAnalyzer;
import com.example.autobuilder.engine.SchematicData;
import com.example.autobuilder.engine.SchematicLoader;
import com.example.client.inventory.PlayerInventoryScanner;
import com.example.client.render.PreviewState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class AutoBuilderScreen extends Screen {

    private String status = "No schematic loaded";
    private SchematicData schematic;

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
            inventoryChecked = false;
            status = "Loaded: " + file.getFileName();
        } catch (IOException | RuntimeException exception) {
            status = "Load failed: " + exception.getMessage();
        }
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

    @Override
    protected void init() {
        int left = this.width / 2 - 140;
        int top = this.height / 2 - 65;

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
                    } else {
                        var player = Minecraft.getInstance().player;

                        if (player == null) {
                            status = "Enter a world first";
                        } else {
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
                    }
                }
        ).bounds(left + 140, top, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Build"),
                button -> status =
                        "Automatic placement is not enabled here"
        ).bounds(left, top + 32, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Pause"),
                button -> status = "Nothing is currently building"
        ).bounds(left + 140, top + 32, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Check Inventory"),
                button -> checkInventory()
        ).bounds(left, top + 64, 280, 22).build());
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
        int top = height / 2 - 115;

        graphics.fill(
                left, top, left + 310, top + 230,
                0xFF151E30
        );

        graphics.drawCenteredString(
                font, Component.literal("AUTOBUILDER"),
                width / 2, top + 12, 0xFF55CCFF
        );

        graphics.drawCenteredString(
                font, Component.literal("Schematic Assistant"),
                width / 2, top + 29, 0xFFAAAAAA
        );

        graphics.drawCenteredString(
                font, Component.literal(status),
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

        super.render(graphics, mouseX, mouseY, delta);
    }
}
