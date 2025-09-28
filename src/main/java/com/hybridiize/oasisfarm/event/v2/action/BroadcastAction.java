package com.hybridiize.oasisfarm.event.v2.action;

import com.hybridiize.oasisfarm.event.v2.PhaseAction;
import java.util.Map;

public class BroadcastAction implements PhaseAction {
    private final String message;

    public BroadcastAction(Map<?, ?> data) {
        String foundMessage = ""; // Default to an empty string
        Object rawMessage = data.get("message");

        // Only assign the value if it's actually a String
        if (rawMessage instanceof String) {
            foundMessage = (String) rawMessage;
        }

        this.message = foundMessage;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return "BROADCAST";
    }

    @Override
    public void execute() {
        // Execution logic will be in EventManager
    }
}