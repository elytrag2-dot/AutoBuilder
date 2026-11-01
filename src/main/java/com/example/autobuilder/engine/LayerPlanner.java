package com.example.autobuilder.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

public class LayerPlanner {

    public static Map<Integer, List<SchematicBlock>> calculateLayers(
            SchematicData schematic
    ) {
        Map<Integer, List<SchematicBlock>> layers = new TreeMap<>();

        for (SchematicBlock block : schematic.blocks()) {
            layers.computeIfAbsent(
                    block.y(),
                    y -> new ArrayList<>()
            ).add(block);
        }

        return layers;
    }

    public static Map<String, Integer> countRequiredBlocks(
            List<SchematicBlock> blocks
    ) {
        Map<String, Integer> counts = new HashMap<>();

        for (SchematicBlock block : blocks) {
            counts.merge(block.blockId(), 1, Integer::sum);
        }

        return counts;
    }
}
