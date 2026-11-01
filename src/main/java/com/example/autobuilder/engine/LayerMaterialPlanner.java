package com.example.autobuilder.engine;

import java.util.HashMap;
import java.util.Map;

public final class LayerMaterialPlanner {
    public record MaterialReport(
            int layerY,
            Map<String, Integer> required,
            Map<String, Integer> available,
            Map<String, Integer> missing,
            int totalRequired,
            int totalAvailable,
            int totalMissing
    ) {}

    private LayerMaterialPlanner() {}

    public static MaterialReport analyze(
            SchematicData schematic,
            int layerY,
            Map<String, Integer> inventory
    ) {
        if (schematic == null) {
            throw new IllegalArgumentException("Schematic is required");
        }

        Map<String, Integer> required = new HashMap<>();
        for (SchematicBlock block : schematic.blocks()) {
            if (block.y() == layerY) {
                required.merge(block.blockId(), 1, Integer::sum);
            }
        }

        Map<String, Integer> available = new HashMap<>();
        Map<String, Integer> missing = new HashMap<>();

        int totalRequired = 0;
        int totalAvailable = 0;
        int totalMissing = 0;

        for (var entry : required.entrySet()) {
            int need = entry.getValue();
            int have = Math.max(0, inventory.getOrDefault(entry.getKey(), 0));
            int absent = Math.max(0, need - have);

            available.put(entry.getKey(), Math.min(need, have));
            if (absent > 0) {
                missing.put(entry.getKey(), absent);
            }

            totalRequired += need;
            totalAvailable += Math.min(need, have);
            totalMissing += absent;
        }

        return new MaterialReport(
                layerY,
                Map.copyOf(required),
                Map.copyOf(available),
                Map.copyOf(missing),
                totalRequired,
                totalAvailable,
                totalMissing
        );
    }
}
