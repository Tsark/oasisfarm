package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class InfoCommand extends SubCommand {
    @Override
    public String getName() { return "info"; }
    @Override
    public String getDescription() { return "Shows detailed info about a farm."; }
    @Override
    public String getSyntax() { return "/of info <farm_name>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String farmId = args[1];
        Farm farm = plugin.getConfigManager().getFarms().get(farmId);

        if (farm == null) {
            player.sendMessage(ChatColor.RED + "A farm with that name does not exist!");
            return;
        }

        Location p1 = farm.getRegion().getPos1();
        Location p2 = farm.getRegion().getPos2();

        player.sendMessage(ChatColor.GOLD + "--- Info for " + farm.getId() + " ---");
        player.sendMessage(ChatColor.AQUA + "World: " + ChatColor.WHITE + p1.getWorld().getName());
        player.sendMessage(ChatColor.AQUA + "Max Mobs: " + ChatColor.WHITE + farm.getMaxMobs());
        player.sendMessage(ChatColor.AQUA + "Position 1: " + ChatColor.WHITE + p1.getBlockX() + ", " + p1.getBlockY() + ", " + p1.getBlockZ());
        player.sendMessage(ChatColor.AQUA + "Position 2: " + ChatColor.WHITE + p2.getBlockX() + ", " + p2.getBlockY() + ", " + p2.getBlockZ());
        player.sendMessage(ChatColor.AQUA + "Mob Types: " + ChatColor.WHITE + farm.getMobInfoList().size());
    }
}