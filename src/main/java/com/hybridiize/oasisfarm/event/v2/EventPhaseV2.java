package com.hybridiize.oasisfarm.event.v2;

import java.util.List;

public class EventPhaseV2 {
    private final String phaseId;
    private final List<PhaseAction> actions;
    private final PhaseProgression progression;

    public EventPhaseV2(String phaseId, List<PhaseAction> actions, PhaseProgression progression) {
        this.phaseId = phaseId;
        this.actions = actions;
        this.progression = progression;
    }

    public String getPhaseId() {
        return phaseId;
    }

    public List<PhaseAction> getActions() {
        return actions;
    }

    public PhaseProgression getProgression() {
        return progression;
    }
}