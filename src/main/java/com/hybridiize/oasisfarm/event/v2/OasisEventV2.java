package com.hybridiize.oasisfarm.event.v2;

import java.util.List;

public class OasisEventV2 {
    private final String id;
    private final EventTrigger trigger;
    private final List<EventPhaseV2> phases;

    public OasisEventV2(String id, EventTrigger trigger, List<EventPhaseV2> phases) {
        this.id = id;
        this.trigger = trigger;
        this.phases = phases;
    }

    public String getId() {
        return id;
    }

    public EventTrigger getTrigger() {
        return trigger;
    }

    public List<EventPhaseV2> getPhases() {
        return phases;
    }
}