package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

public abstract class SubCommand {

    protected Oasisfarm plugin = Oasisfarm.getInstance();

    // The name of the sub-command (e.g., "wand", "list")
    public abstract String getName();

    // A brief description of what it does
    public abstract String getDescription();

    // The syntax for the command (e.g., "/of create <name>")
    public abstract String getSyntax();

    // The code that runs when the command is executed
    public abstract void perform(Player player, String[] args);
}