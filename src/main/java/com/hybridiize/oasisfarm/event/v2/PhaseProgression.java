package com.hybridiize.oasisfarm.event.v2;

import java.util.List;

public class PhaseProgression {
    private final String mode;
    private final List<Condition> conditions;

    public PhaseProgression(String mode, List<Condition> conditions) {
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