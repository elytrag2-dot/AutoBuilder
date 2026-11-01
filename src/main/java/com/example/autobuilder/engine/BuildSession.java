package com.example.autobuilder.engine;

import java.util.List;
import java.util.Map;

public final class BuildSession {

    private final SchematicData schematic;
    private final BuildQueue queue;
    private final List<PlacementPlanner.PlacementStep> plan;
    private final BuildPlanValidator.ValidationResult validation;

    public BuildSession(
            SchematicData schematic,
            Map<String, Integer> available
    ) {
        if (schematic == null) {
            throw new IllegalArgumentException("Schematic is required");
        }

        this.schematic = schematic;
        this.queue = new BuildQueue(schematic);
        this.plan = PlacementPlanner.createPlan(schematic);
        this.validation = BuildPlanValidator.validate(
                schematic, available
        );
    }

    public SchematicData getSchematic() {
        return schematic;
    }

    public BuildQueue getQueue() {
        return queue;
    }

    public List<PlacementPlanner.PlacementStep> getPlan() {
        return plan;
    }

    public BuildPlanValidator.ValidationResult getValidation() {
        return validation;
    }

    public boolean canStart() {
        return validation.valid();
    }

    public int getCompletedCount() {
        return queue.getCompletedCount();
    }

    public int getTotalCount() {
        return queue.getTotalCount();
    }

    public double getProgress() {
        return queue.getProgress();
    }
}
