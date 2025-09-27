package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

public abstract class SubCommand {

    protected final Oasisfarm plugin = Oasisfarm.getInstance();

    /**
     * @return The name of the sub-command (e.g., "create", "list")
     */
    public abstract String getName();

    /**
     * @return A brief description of what the command does for the help message.
     */
    public abstract String getDescription();

    /**
     * @return The correct command syntax for the help message (e.g., "/of create <name>").
     */
    public abstract String getSyntax();

    /**
     * The code that is executed when the command is run.
     * @param player The player who ran the command.
     * @param args The command arguments passed to the command.
     */
    public abstract void perform(Player player, String[] args);
}