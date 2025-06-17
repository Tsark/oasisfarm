package com.hybridiize.oasisfarm.event;

public class ActiveEventTracker {
    private final OasisEvent event;
    private int currentPhaseIndex;
    public long phaseEndTime;

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

    // This method moves to the next phase and sets its timer
    public EventPhase advanceToNextPhase() {
        currentPhaseIndex++;
        if (hasEnded()) {
            return null; // The event is over
        }
        EventPhase newPhase = getCurrentPhase();
        // Set the end time for the new phase
        this.phaseEndTime = System.currentTimeMillis() + (newPhase.getDuration() * 1000L);
        return newPhase;
    }

    public boolean isPhaseOver() {
        return System.currentTimeMillis() >= phaseEndTime;
    }

    public long getPhaseEndTime() {
        return phaseEndTime;
    }
}