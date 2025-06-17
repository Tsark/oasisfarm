package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MobRemoveCommand extends MobSubCommand {
    @Override
    public String getName() { return "remove"; }
    @Override
    public String getDescription() { return "Removes a mob type from a farm."; }
    @Override
    public String getSyntax() { return "/of mob remove <template_id> <name...>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String templateId = args[2]; // No farm needed now
        String name = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
        FileConfiguration config = plugin.getConfig();
        String mobPath = "farms." + farmId + ".mobs." + mobTypeStr;

        if (!config.contains(mobPath)) {
            player.sendMessage(ChatColor.RED + "Mob '" + mobTypeStr + "' not found in farm '" + farmId + "'.");
            return;
        }

        config.set(mobPath, null);
        plugin.saveConfig();
        plugin.getConfigManager().loadFarms();
        player.sendMessage(ChatColor.GREEN + "Removed " + mobTypeStr + " from farm " + farmId + ".");
    }
}