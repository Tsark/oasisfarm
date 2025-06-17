package com.hybridiize.oasisfarm.event;

// A simple container for a single condition
public class EventCondition {
    private final String type; // e.g., "MIN_PLAYERS"
    private final String value; // e.g., "5"

    public EventCondition(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() { return type; }
    public String getValue() { return value; }
}