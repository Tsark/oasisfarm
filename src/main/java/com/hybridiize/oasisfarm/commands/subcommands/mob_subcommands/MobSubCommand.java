package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

public abstract class MobSubCommand {
    protected Oasisfarm plugin = Oasisfarm.getInstance();

    public abstract String getName();
    public abstract String getDescription();
    public abstract String getSyntax();
    public abstract void perform(Player player, String[] args);
}