package com.hybridiize.oasisfarm.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Set;

public class ListCommand extends SubCommand {
    @Override
    public String getName() { return "list"; }
    @Override
    public String getDescription() { return "Lists all available farms."; }
    @Override
    public String getSyntax() { return "/of list"; }

    @Override
    public void perform(Player player, String[] args) {
        Set<String> farmNames = plugin.getConfigManager().getFarms().keySet();
        if (farmNames.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no farms defined.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "--- Oasis Farms ---");
        for (String farmName : farmNames) {
            player.sendMessage(ChatColor.AQUA + "- " + farmName);
        }
    }
}