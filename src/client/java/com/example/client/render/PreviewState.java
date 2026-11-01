package com.example.client.render;

import com.example.autobuilder.engine.SchematicData;
import net.minecraft.core.BlockPos;

public final class PreviewState {

    private static SchematicData schematic;
    private static BlockPos anchor;
    private static boolean active;

    private PreviewState() {
    }

    public static void start(SchematicData data, BlockPos position) {
        schematic = data;
        anchor = position;
        active = true;
    }

    public static void stop() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static SchematicData getSchematic() {
        return schematic;
    }

    public static BlockPos getAnchor() {
        return anchor;
    }
}
