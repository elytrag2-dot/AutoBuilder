package com.example.client.gui;

import com.example.autobuilder.engine.SchematicData;
import com.example.autobuilder.engine.SchematicLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AutoBuilderScreen extends Screen {

    private String status = "No schematic loaded";
    private SchematicData schematic;

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
            status = "Loaded: " + file.getFileName();
        } catch (IOException | RuntimeException exception) {
            status = "Load failed: " + exception.getMessage();
        }
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
                        status = "Loaded " + schematic.getBlockCount()
                                + " blocks";
                    }
                }
        ).bounds(left + 140, top, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Build"),
                button -> status =
                        "Building automation is not implemented"
        ).bounds(left, top + 32, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Pause"),
                button -> status = "Nothing is currently building"
        ).bounds(left + 140, top + 32, 130, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Resume"),
                button -> status = "Nothing is paused"
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
        int top = height / 2 - 105;

        graphics.fill(left, top, left + 310, top + 210, 0xFF151E30);

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
                    Component.literal("Blocks: " + schematic.getBlockCount()),
                    width / 2, top + 185, 0xFF55FF99
            );
        }

        super.render(graphics, mouseX, mouseY, delta);
    }
}
