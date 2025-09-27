package com.hybridiize.oasisfarm.event;

public class ActiveEventTracker {
    private final OasisEvent event;
    private int currentPhaseIndex;
    private long phaseEndTime;

    public ActiveEventTracker(OasisEvent event) {
        this.event = event;
        this.currentPhaseIndex = -1; // -1 means it hasn't started the first phase yet
    }

    public OasisEvent getEvent() {
        return event;
    }

    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public EventPhase getCurrentPhase() {
        if (currentPhaseIndex < 0 || currentPhaseIndex >= event.getPhases().size()) {
            return null;
        }
        return event.getPhases().get(currentPhaseIndex);
    }

    public boolean hasEnded() {
        return currentPhaseIndex >= event.getPhases().size();
    }

    /**
     * Moves the tracker to the next phase in the list and sets the timer for the new phase.
     * @return The new EventPhase, or null if the event has ended.
     */
    public EventPhase advanceToNextPhase() {
        currentPhaseIndex++;
        if (hasEnded()) {
            return null; // The event is over
        }
        EventPhase newPhase = getCurrentPhase();
        if (newPhase != null) {
            // Set the end time for the new phase
            this.phaseEndTime = System.currentTimeMillis() + (newPhase.getDuration() * 1000L);
        }
        return newPhase;
    }

    public boolean isPhaseOver() {
        // If the index is -1, the first phase hasn't started, so it's "over" and ready to begin.
        if (currentPhaseIndex < 0) return true;
        return System.currentTimeMillis() >= phaseEndTime;
    }

    public long getPhaseEndTime() {
        return phaseEndTime;
    }
}