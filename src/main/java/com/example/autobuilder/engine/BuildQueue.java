package com.example.autobuilder.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BuildQueue {

    public record QueueEntry(int index, SchematicBlock block) {}

    private final List<QueueEntry> entries;
    private final boolean[] completed;

    private int currentIndex;
    private boolean paused;
    private boolean stopped;

    public BuildQueue(SchematicData schematic) {
        this.entries = new ArrayList<>();

        for (int i = 0; i < schematic.blocks().size(); i++) {
            entries.add(new QueueEntry(i, schematic.blocks().get(i)));
        }

        entries.sort(
                Comparator.comparingInt((QueueEntry e) -> e.block().y())
                        .thenComparingInt(e -> e.block().z())
                        .thenComparingInt(e -> e.block().x())
        );

        completed = new boolean[entries.size()];
    }

    public synchronized QueueEntry getNextEntry() {
        if (paused || stopped) return null;

        while (currentIndex < entries.size()
                && completed[entries.get(currentIndex).index()]) {
            currentIndex++;
        }

        return currentIndex < entries.size()
                ? entries.get(currentIndex)
                : null;
    }

    public synchronized void markCompleted(int originalIndex) {
        if (originalIndex >= 0 && originalIndex < completed.length) {
            completed[originalIndex] = true;
        }
    }

    public synchronized void pause() {
        if (!stopped) paused = true;
    }

    public synchronized void resume() {
        if (!stopped) paused = false;
    }

    public synchronized void stop() {
        stopped = true;
        paused = false;
    }

    public synchronized int getCompletedCount() {
        int count = 0;
        for (boolean value : completed) {
            if (value) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return entries.size();
    }

    public synchronized double getProgress() {
        return entries.isEmpty()
                ? 100.0
                : getCompletedCount() * 100.0 / entries.size();
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized boolean isFinished() {
        return getCompletedCount() == entries.size();
    }
}
