package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MobRebalanceCommand extends MobSubCommand {
    @Override
    public String getName() { return "rebalance"; }

    @Override
    public String getDescription() { return "Auto-rebalances a farm's spawn chances to total 100%."; }

    @Override
    public String getSyntax() { return "/of mob rebalance <farm_name>"; }

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

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection mobsSection = config.getConfigurationSection("farms." + farmId + ".mobs");
        if (mobsSection == null || mobsSection.getKeys(false).isEmpty()) {
            player.sendMessage(ChatColor.RED + "This farm has no mobs to rebalance.");
            return;
        }

        // --- UPDATED to read both old and new formats ---
        double totalChance = 0;
        for (String mobKey : mobsSection.getKeys(false)) {
            if (mobsSection.isConfigurationSection(mobKey)) {
                // New format: mobKey.chance
                totalChance += mobsSection.getDouble(mobKey + ".chance", 0.0);
            } else {
                // Old format: mobKey: 0.8
                totalChance += mobsSection.getDouble(mobKey, 0.0);
            }
        }
        // --- END UPDATE ---

        if (totalChance == 0) {
            player.sendMessage(ChatColor.RED + "All mob chances are zero. Cannot rebalance.");
            return;
        }

        // --- UPDATED to write in the correct format ---
        for (String mobKey : mobsSection.getKeys(false)) {
            double currentChance;
            if (mobsSection.isConfigurationSection(mobKey)) {
                // New format
                currentChance = mobsSection.getDouble(mobKey + ".chance");
                double newChance = currentChance / totalChance; // Normalize to 1.0 total
                mobsSection.set(mobKey + ".chance", newChance); // Set the chance inside the map
            } else {
                // Old format
                currentChance = mobsSection.getDouble(mobKey);
                double newChance = currentChance / totalChance; // Normalize to 1.0 total
                mobsSection.set(mobKey, newChance); // Set the simple value
            }
        }
        // --- END UPDATE ---

        plugin.saveConfig();
        plugin.getConfigManager().loadAllConfigs();
        player.sendMessage(ChatColor.GREEN + "Spawn chances for '" + farmId + "' have been rebalanced to total 100%.");
        player.sendMessage(ChatColor.YELLOW + "Run '/of mob list " + farmId + "' to see the new chances.");
    }
}