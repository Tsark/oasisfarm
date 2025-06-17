package com.hybridiize.oasisfarm.commands.eventsubcommands;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

public abstract class EventSubCommand {
    protected Oasisfarm plugin = Oasisfarm.getInstance();

    public abstract String getName();
    public abstract String getDescription();
    public abstract String getSyntax();
    public abstract void perform(Player player, String[] args);
}