package com.example.autobuilder.engine;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BuildPlanValidator {

    public record ValidationResult(
            boolean valid,
            int totalBlocks,
            int duplicatePositions,
            int missingBlockTypes,
            int missingBlockCount,
            String message
    ) {}

    private BuildPlanValidator() {
    }

    public static ValidationResult validate(
            SchematicData schematic,
            Map<String, Integer> available
    ) {
        if (schematic == null || schematic.blocks().isEmpty()) {
            return new ValidationResult(
                    false, 0, 0, 0, 0, "Schematic is empty"
            );
        }

        Set<String> positions = new HashSet<>();
        int duplicates = 0;

        for (SchematicBlock block : schematic.blocks()) {
            String position = block.x() + "," + block.y()
                    + "," + block.z();

            if (!positions.add(position)) {
                duplicates++;
            }
        }

        Map<String, Integer> required =
                InventoryAnalyzer.countRequired(schematic);

        Map<String, Integer> missing =
                InventoryAnalyzer.calculateMissing(required, available);

        int missingCount = missing.values().stream()
                .mapToInt(Integer::intValue).sum();

        String message = duplicates > 0
                ? "Duplicate schematic positions detected"
                : missingCount > 0
                ? "Not enough blocks in inventory"
                : "Build plan validated";

        return new ValidationResult(
                duplicates == 0 && missingCount == 0,
                schematic.getBlockCount(),
                duplicates,
                missing.size(),
                missingCount,
                message
        );
    }
}
