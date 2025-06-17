package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MobListCommand extends MobSubCommand {
    @Override
    public String getName() { return "list"; }
    @Override
    public String getDescription() { return "Lists all mobs and their chances for a farm."; }
    @Override
    public String getSyntax() { return "/of mob list <farm_name>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }
        String farmId = args[2];
        Farm farm = plugin.getConfigManager().getFarms().get(farmId);

        if (farm == null) {
            player.sendMessage(ChatColor.RED + "Farm '" + farmId + "' not found.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "--- Mobs for " + farm.getId() + " ---");
        if (farm.getMobInfoList().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "This farm has no mobs configured.");
            return;
        }

        for (MobInfo mobInfo : farm.getMobInfoList()) {
            player.sendMessage(ChatColor.AQUA + mobInfo.getType().name() + ": " +
                    ChatColor.WHITE + (mobInfo.getSpawnChance() * 100) + "%");
        }
    }
}