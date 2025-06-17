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
    public String getDescription() { return "Auto-rebalances spawn chances to total 100%."; }
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

        double totalChance = mobsSection.getKeys(false).stream()
                .mapToDouble(key -> mobsSection.getDouble(key + ".spawn-chance"))
                .sum();

        if (totalChance == 0) {
            player.sendMessage(ChatColor.RED + "All mob chances are zero. Cannot rebalance.");
            return;
        }

        for (String mobKey : mobsSection.getKeys(false)) {
            double currentChance = mobsSection.getDouble(mobKey + ".spawn-chance");
            double newChance = currentChance / totalChance; // Normalize to 1.0 total
            mobsSection.set(mobKey + ".spawn-chance", newChance);
        }

        plugin.saveConfig();
        plugin.getConfigManager().loadFarms();
        player.sendMessage(ChatColor.GREEN + "Spawn chances for '" + farmId + "' have been rebalanced to total 100%.");
        player.sendMessage(ChatColor.YELLOW + "Run '/of mob list " + farmId + "' to see the new chances.");
    }
}