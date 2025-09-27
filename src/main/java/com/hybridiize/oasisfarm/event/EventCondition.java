package com.hybridiize.oasisfarm.event;

/**
 * A simple container for a single event condition (e.g., type="MIN_PLAYERS", value="5").
 */
public class EventCondition {
    private final String type;
    private final String value;

    public EventCondition(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}