package com.hybridiize.oasisfarm.event.v2;

import java.util.Map;

public class Condition {
    private final String type;
    private final String value;
    private final Map<String, String> properties; // For extra data like 'mob_id'

    public Condition(String type, String value, Map<String, String> properties) {
        this.type = type;
        this.value = value;
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}