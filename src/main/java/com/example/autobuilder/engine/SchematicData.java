package com.example.autobuilder.engine;

import java.util.List;

public record SchematicData(
        String name,
        int width,
        int height,
        int length,
        List<SchematicBlock> blocks
) {
    public SchematicData {
        blocks = List.copyOf(blocks);
    }

    public int getBlockCount() {
        return blocks.size();
    }
}
