package com.hybridiize.oasisfarm.event.v2.action;

import com.hybridiize.oasisfarm.event.v2.PhaseAction;
import java.util.Map;

public class CommandAction implements PhaseAction {
    private final String command;

    public CommandAction(Map<?, ?> data) {
        String foundCommand = ""; // Default to an empty string
        Object rawCommand = data.get("command");

        // Only assign the value if it's actually a String
        if (rawCommand instanceof String) {
            foundCommand = (String) rawCommand;
        }

        this.command = foundCommand;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String getType() {
        return "COMMAND";
    }

    @Override
    public void execute() {}
}