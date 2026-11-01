package com.example.autobuilder.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlacementPlanner {

    public record PlacementStep(
            int originalIndex,
            SchematicBlock block,
            boolean needsSupport
    ) {}

    private PlacementPlanner() {
    }

    public static List<PlacementStep> createPlan(
            SchematicData schematic
    ) {
        List<PlacementStep> plan = new ArrayList<>();
        Set<String> plannedPositions = new HashSet<>();

        List<SchematicBlock> blocks = schematic.blocks();

        for (int i = 0; i < blocks.size(); i++) {
            SchematicBlock block = blocks.get(i);

            boolean hasBlockBelow = plannedPositions.contains(
                    key(block.x(), block.y() - 1, block.z())
            );

            plan.add(new PlacementStep(
                    i,
                    block,
                    !hasBlockBelow && block.y() > 0
            ));

            plannedPositions.add(
                    key(block.x(), block.y(), block.z())
            );
        }

        return List.copyOf(plan);
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}
