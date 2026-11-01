package com.example.autobuilder.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BuildQueue {

    private final List<SchematicBlock> blocks;
    private final boolean[] completed;
    private int currentIndex;
    private boolean paused;
    private boolean stopped;

    public BuildQueue(SchematicData schematic) {
        this.blocks = new ArrayList<>(schematic.blocks());

        this.blocks.sort(
                Comparator.comparingInt(SchematicBlock::y)
                        .thenComparingInt(SchematicBlock::z)
                        .thenComparingInt(SchematicBlock::x)
        );

        this.completed = new boolean[blocks.size()];
    }

    public synchronized SchematicBlock getNextBlock() {
        if (paused || stopped) {
            return null;
        }

        while (currentIndex < blocks.size()
                && completed[currentIndex]) {
            currentIndex++;
        }

        return currentIndex < blocks.size()
                ? blocks.get(currentIndex)
                : null;
    }

    public synchronized void markCompleted(int index) {
        if (index >= 0 && index < completed.length) {
            completed[index] = true;
        }
    }

    public synchronized void pause() {
        paused = true;
    }

    public synchronized void resume() {
        if (!stopped) {
            paused = false;
        }
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
        return blocks.size();
    }

    public synchronized double getProgress() {
        return blocks.isEmpty()
                ? 100.0
                : getCompletedCount() * 100.0 / blocks.size();
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized boolean isFinished() {
        return getCompletedCount() == blocks.size();
    }
}
