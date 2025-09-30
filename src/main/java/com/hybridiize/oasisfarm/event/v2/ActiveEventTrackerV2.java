package com.hybridiize.oasisfarm.event.v2;

import com.hybridiize.oasisfarm.farm.Farm;
import java.util.HashMap;
import java.util.Map;

public class ActiveEventTrackerV2 {

    private final OasisEventV2 event;
    private final Farm farm;
    private int currentPhaseIndex;
    private long phaseStartTime;
    private final Map<String, Integer> mobKills = new HashMap<>();

    public ActiveEventTrackerV2(OasisEventV2 event, Farm farm) {
        this.event = event;
        this.farm = farm;
        this.currentPhaseIndex = -1; // -1 means it hasn't started the first phase yet
    }

    public OasisEventV2 getEvent() {
        return event;
    }

    public Farm getFarm() {
        return farm;
    }

    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public EventPhaseV2 getCurrentPhase() {
        if (currentPhaseIndex < 0 || currentPhaseIndex >= event.getPhases().size()) {
            return null;
        }
        return event.getPhases().get(currentPhaseIndex);
    }

    public boolean hasEnded() {
        return currentPhaseIndex >= event.getPhases().size();
    }

    public long getPhaseStartTime() {
        return phaseStartTime;
    }

    /**
     * Moves the tracker to the next phase in the list.
     * @return The new EventPhase, or null if the event has ended.
     */
    public EventPhaseV2 advanceToNextPhase() {
        currentPhaseIndex++;
        if (hasEnded()) {
            return null; // The event is over
        }
        this.phaseStartTime = System.currentTimeMillis();
        this.mobKills.clear(); // Reset kills for the new phase
        return getCurrentPhase();
    }

    public void incrementMobKills(String mobId) {
        mobKills.put(mobId, mobKills.getOrDefault(mobId, 0) + 1);
    }

    public int getMobKills(String mobId) {
        return mobKills.getOrDefault(mobId, 0);
    }
}