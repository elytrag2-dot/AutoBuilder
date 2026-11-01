package com.example.autobuilder.engine;

import java.util.HashMap;
import java.util.Map;

public final class InventoryAnalyzer {

    private InventoryAnalyzer() {
    }

    public static Map<String, Integer> countRequired(
            SchematicData schematic
    ) {
        Map<String, Integer> required = new HashMap<>();

        for (SchematicBlock block : schematic.blocks()) {
            required.merge(block.blockId(), 1, Integer::sum);
        }

        return required;
    }

    public static Map<String, Integer> calculateMissing(
            Map<String, Integer> required,
            Map<String, Integer> available
    ) {
        Map<String, Integer> missing = new HashMap<>();

        for (var entry : required.entrySet()) {
            int have = available.getOrDefault(entry.getKey(), 0);
            int need = entry.getValue();

            if (need > have) {
                missing.put(entry.getKey(), need - have);
            }
        }

        return missing;
    }
}
