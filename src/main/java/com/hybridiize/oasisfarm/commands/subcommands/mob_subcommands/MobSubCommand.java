package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

public abstract class MobSubCommand {
    protected final Oasisfarm plugin = Oasisfarm.getInstance();

    /**
     * @return The name of the mob sub-command (e.g., "add", "spawn").
     */
    public abstract String getName();

    /**
     * @return A brief description of what the command does.
     */
    public abstract String getDescription();

    /**
     * @return The correct command syntax (e.g., "/of mob add <farm> <template> <chance>").
     */
    public abstract String getSyntax();

    /**
     * The code that is executed when the command is run.
     * @param player The player who ran the command.
     * @param args The full command arguments.
     */
    public abstract void perform(Player player, String[] args);
}