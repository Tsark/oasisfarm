package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MobSetNameCommand extends MobSubCommand {
    @Override
    public String getName() { return "setname"; }
    @Override
    public String getDescription() { return "Sets the display name of a mob type."; }
    @Override
    public String getSyntax() { return "/of mob setname <template_id> <name...>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String templateId = args[2]; // No farm needed now
        String name = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));

        FileConfiguration config = plugin.getConfig();
        String path = "farms." + farmId + ".mobs." + mobType;

        if (!config.contains(path)) {
            player.sendMessage(ChatColor.RED + "That mob/farm combination does not exist.");
            return;
        }

        config.set(path + ".display-name", name);
        plugin.saveConfig();
        plugin.getConfigManager().loadFarms();

        String coloredName = ChatColor.translateAlternateColorCodes('&', name);
        player.sendMessage(ChatColor.GREEN + "Set display name for " + mobType + " in " + farmId + " to: " + coloredName);
    }
}