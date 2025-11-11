package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.FarmMobConfig; // --- IMPORT ADDED ---
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

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

        player.sendMessage(ChatColor.GOLD + "--- Mobs for " + farm.getId() + " (Type: " + farm.getSpawningType() + ") ---");
        if (farm.getMobs().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "This farm has no mobs configured.");
            return;
        }

        double totalChance = 0;
        // --- THIS IS THE CORRECTED LOOP ---
        for (Map.Entry<String, FarmMobConfig> entry : farm.getMobs().entrySet()) {
            FarmMobConfig mobConfig = entry.getValue();
            double chance = mobConfig.getChance();
            int maxCap = mobConfig.getMaxPerFarm();

            String capDisplay = (maxCap == -1) ? "None" : String.valueOf(maxCap);

            player.sendMessage(ChatColor.AQUA + entry.getKey() + ": " +
                    ChatColor.WHITE + String.format("%.2f", chance * 100) + "%" +
                    ChatColor.GRAY + " (Cap: " + capDisplay + ")");

            totalChance += chance;
        }
        player.sendMessage(ChatColor.GRAY + "Total Chance: " + String.format("%.2f", totalChance * 100) + "%");
    }
}