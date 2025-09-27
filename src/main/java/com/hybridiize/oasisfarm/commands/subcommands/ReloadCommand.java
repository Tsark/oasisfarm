package com.hybridiize.oasisfarm.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ReloadCommand extends SubCommand {
    @Override
    public String getName() { return "reload"; }

    @Override
    public String getDescription() { return "Reloads all plugin configuration files."; }

    @Override
    public String getSyntax() { return "/of reload"; }

    @Override
    public void perform(Player player, String[] args) {
        plugin.getHologramManager().removeAllHolograms();
        plugin.getConfigManager().loadAllConfigs();
        player.sendMessage(ChatColor.GREEN + "OasisFarm configuration files reloaded!");
    }
}