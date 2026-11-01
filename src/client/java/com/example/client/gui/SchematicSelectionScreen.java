package com.example.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SchematicSelectionScreen extends Screen {

    private static final int PAGE_SIZE = 6;

    private final AutoBuilderScreen parent;
    private List<Path> schematics = List.of();
    private int page = 0;
    private String message = "";

    public SchematicSelectionScreen(AutoBuilderScreen parent) {
        super(Component.literal("Select Schematic"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 140;
        int top = height / 2 - 105;

        try {
            Path folder = parent.getSchematicFolder();
            Files.createDirectories(folder);

            try (var stream = Files.list(folder)) {
                schematics = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName()
                                    .toString().toLowerCase(Locale.ROOT);
                            return name.endsWith(".litematic")
                                    || name.endsWith(".schem");
                        })
                        .sorted(Comparator.comparing(
                                path -> path.getFileName().toString()
                                        .toLowerCase(Locale.ROOT)
                        ))
                        .toList();
            }
        } catch (IOException exception) {
            message = "Could not read schematic folder";
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, schematics.size());

        for (int i = start; i < end; i++) {
            Path file = schematics.get(i);
            String name = file.getFileName().toString();

            if (name.length() > 34) {
                name = name.substring(0, 31) + "...";
            }

            addRenderableWidget(Button.builder(
                    Component.literal(name),
                    button -> {
                        parent.loadSelectedSchematic(file);
                        Minecraft.getInstance().setScreen(parent);
                    }
            ).bounds(left, top + (i - start) * 27, 280, 23).build());
        }

        int navigationY = top + PAGE_SIZE * 27 + 5;

        addRenderableWidget(Button.builder(
                Component.literal("Previous"),
                button -> {
                    if (page > 0) {
                        page--;
                        rebuildWidgets();
                    }
                }
        ).bounds(left, navigationY, 85, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Next"),
                button -> {
                    if ((page + 1) * PAGE_SIZE < schematics.size()) {
                        page++;
                        rebuildWidgets();
                    }
                }
        ).bounds(left + 95, navigationY, 85, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(left + 190, navigationY, 90, 22).build());
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

        graphics.fill(left, top, left + 310, top + 250, 0xFF151E30);

        graphics.drawCenteredString(
                font,
                Component.literal("SELECT SCHEMATIC"),
                width / 2, top + 12, 0xFF55CCFF
        );

        graphics.drawCenteredString(
                font,
                Component.literal("Found: " + schematics.size() + " files"),
                width / 2, top + 30, 0xFFAAAAAA
        );

        if (schematics.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.literal(
                            message.isEmpty()
                                    ? "No .litematic or .schem files found"
                                    : message
                    ),
                    width / 2, height / 2, 0xFFFFFFFF
            );
        }

        graphics.drawCenteredString(
                font,
                Component.literal(
                        "Page " + (page + 1) + " / "
                        + Math.max(1, (schematics.size() + PAGE_SIZE - 1)
                        / PAGE_SIZE)
                ),
                width / 2, top + 225, 0xFF55FF99
        );

        super.render(graphics, mouseX, mouseY, delta);
    }
}
