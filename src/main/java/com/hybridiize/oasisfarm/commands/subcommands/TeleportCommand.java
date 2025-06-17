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

        Location pos1 = farm.getRegion().getPos1();
        Location pos2 = farm.getRegion().getPos2();
        double x = (pos1.getX() + pos2.getX()) / 2.0;
        double y = Math.max(pos1.getY(), pos2.getY()) + 1.0;
        double z = (pos1.getZ() + pos2.getZ()) / 2.0;
        Location teleportLoc = new Location(pos1.getWorld(), x, y, z);

        player.teleport(teleportLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + farmId);
    }
}