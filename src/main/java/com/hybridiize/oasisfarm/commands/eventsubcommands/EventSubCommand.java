package com.hybridiize.oasisfarm.commands.eventsubcommands;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

public abstract class EventSubCommand {
    protected final Oasisfarm plugin = Oasisfarm.getInstance();

    /**
     * @return The name of the event sub-command (e.g., "start", "list").
     */
    public abstract String getName();

    /**
     * @return A brief description of what the command does.
     */
    public abstract String getDescription();

    /**
     * @return The correct command syntax (e.g., "/of event start <event_id>").
     */
    public abstract String getSyntax();

    /**
     * The code that is executed when the command is run.
     * @param player The player who ran the command.
     * @param args The full command arguments.
     */
    public abstract void perform(Player player, String[] args);
}