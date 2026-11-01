package com.example.autobuilder.engine;

public final class BuildController {

    private BuildQueue queue;
    private boolean running;

    public synchronized void start(SchematicData schematic) {
        if (schematic == null || schematic.blocks().isEmpty()) {
            throw new IllegalArgumentException("Schematic is empty");
        }

        queue = new BuildQueue(schematic);
        running = true;
    }

    public synchronized void pause() {
        if (queue != null) queue.pause();
    }

    public synchronized void resume() {
        if (queue != null && !queue.isStopped()) {
            queue.resume();
            running = true;
        }
    }

    public synchronized void stop() {
        if (queue != null) queue.stop();
        running = false;
    }

    public synchronized BuildQueue.QueueEntry getNextEntry() {
        return queue == null ? null : queue.getNextEntry();
    }

    public synchronized void markCompleted(int originalIndex) {
        if (queue != null) queue.markCompleted(originalIndex);
    }

    public synchronized int getCompletedCount() {
        return queue == null ? 0 : queue.getCompletedCount();
    }

    public synchronized int getTotalCount() {
        return queue == null ? 0 : queue.getTotalCount();
    }

    public synchronized double getProgress() {
        return queue == null ? 0 : queue.getProgress();
    }

    public synchronized boolean isRunning() {
        return running && queue != null
                && !queue.isPaused()
                && !queue.isStopped();
    }

    public synchronized boolean isFinished() {
        return queue != null && queue.isFinished();
    }
}
