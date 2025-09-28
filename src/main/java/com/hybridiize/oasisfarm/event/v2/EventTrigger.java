package com.hybridiize.oasisfarm.event.v2;

import java.util.List;

public class EventTrigger {
    private final String mode; // "AND" or "OR"
    private final List<Condition> conditions;

    public EventTrigger(String mode, List<Condition> conditions) {
        this.mode = mode;
        this.conditions = conditions;
    }

    public String getMode() {
        return mode;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}