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
import java.util.Comparator;

public class AutoBuilderScreen extends Screen {

    private String status = "No schematic loaded";
    private SchematicData schematic;

    public AutoBuilderScreen() {
        super(Component.literal("AutoBuilder"));
    }

    private Path getSchematicFolder() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("autobuilder")
                .resolve("schematics");
    }

    private void loadSchematic() {
        try {
            Path folder = getSchematicFolder();
            Files.createDirectories(folder);

            Path file;

            try (var files = Files.list(folder)) {
                file = files
                        .filter(path -> {
                            String name = path.getFileName()
                                    .toString().toLowerCase();

                            return name.endsWith(".litematic")
                                    || name.endsWith(".schem");
                        })
                        .max(Comparator.comparingLong(
                                path -> path.toFile().lastModified()
                        ))
                        .orElse(null);
            }

            if (file == null) {
                status = "No schematic found. Add a file to the folder.";
                return;
            }

            schematic = SchematicLoader.load(file);
            status = "Loaded: " + file.getFileName();

        } catch (IOException | RuntimeException exception) {
            status = "Load failed: " + exception.getMessage();
        }
    }

    @Override
    protected void init() {
        int panelWidth = 280;
        int left = (this.width - panelWidth) / 2;
        int top = this.height / 2 - 65;

        int buttonWidth = 130;
        int buttonHeight = 22;
        int gap = 10;

        addRenderableWidget(Button.builder(
                Component.literal("Load Schematic"),
                button -> loadSchematic()
        ).bounds(left, top, buttonWidth, buttonHeight).build());

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
        ).bounds(left + buttonWidth + gap, top,
                buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(
                Component.literal("Build"),
                button -> status =
                        "Building automation is not implemented"
        ).bounds(left, top + 32, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(
                Component.literal("Pause"),
                button -> status = "Nothing is currently building"
        ).bounds(left + buttonWidth + gap, top + 32,
                buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(
                Component.literal("Resume"),
                button -> status = "Nothing is paused"
        ).bounds(left, top + 64, panelWidth, buttonHeight).build());
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        graphics.fill(
                0, 0, this.width, this.height,
                0xCC080D18
        );

        int panelWidth = 310;
        int panelHeight = 210;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        graphics.fill(
                left, top, left + panelWidth, top + panelHeight,
                0xFF151E30
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("AUTOBUILDER"),
                this.width / 2,
                top + 12,
                0xFF55CCFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("Schematic Assistant"),
                this.width / 2,
                top + 29,
                0xFFAAAAAA
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(status),
                this.width / 2,
                top + 153,
                0xFFFFFFFF
        );

        if (schematic != null) {
            graphics.drawCenteredString(
                    this.font,
                    Component.literal(
                            schematic.width() + " x "
                            + schematic.height() + " x "
                            + schematic.length()
                    ),
                    this.width / 2,
                    top + 169,
                    0xFF55CCFF
            );

            graphics.drawCenteredString(
                    this.font,
                    Component.literal(
                            "Blocks: " + schematic.getBlockCount()
                    ),
                    this.width / 2,
                    top + 185,
                    0xFF55FF99
            );
        }

        super.render(graphics, mouseX, mouseY, delta);
    }
}
