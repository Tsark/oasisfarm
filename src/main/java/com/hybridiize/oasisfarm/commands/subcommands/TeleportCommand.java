package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportCommand extends SubCommand {
    @Override
    public String getName() { return "tp"; }

    @Override
    public String getDescription() { return "Teleports you to a farm's center."; }

    @Override
    public String getSyntax() { return "/of tp <farm_name>"; }

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

        // Teleport to the center, one block above the calculated middle Y-level
        Location teleportLoc = farm.getRegion().getCenter().add(0, 1, 0);

        player.teleport(teleportLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + farmId);
    }
}