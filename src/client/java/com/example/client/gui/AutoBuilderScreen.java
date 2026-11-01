package com.example.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AutoBuilderScreen extends Screen {

    private String status = "No schematic loaded";

    public AutoBuilderScreen() {
        super(Component.literal("AutoBuilder"));
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
                button -> status = "Schematic loader coming soon"
        ).bounds(left, top, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(
                Component.literal("Preview"),
                button -> status = "Preview coming soon"
        ).bounds(left + buttonWidth + gap, top,
                buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(
                Component.literal("Build"),
                button -> status = "Building system not implemented yet"
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

        int panelWidth = 300;
        int panelHeight = 190;
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
                top + 14,
                0xFF55CCFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("Layer-by-layer schematic builder"),
                this.width / 2,
                top + 34,
                0xFFAAAAAA
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("Status: " + status),
                this.width / 2,
                top + 145,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("Progress: 0%"),
                this.width / 2,
                top + 162,
                0xFF55FF99
        );

        super.render(graphics, mouseX, mouseY, delta);
    }
}
