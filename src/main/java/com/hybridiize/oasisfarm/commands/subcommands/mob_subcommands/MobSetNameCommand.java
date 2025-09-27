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
    public String getDescription() { return "Sets the display name of a mob template."; }

    @Override
    public String getSyntax() { return "/of mob setname <template_id> <name...>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String templateId = args[2];
        String name = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));

        FileConfiguration mobTemplatesConfig = plugin.getConfigManager().getMobTemplatesConfig();
        String path = templateId;

        if (!mobTemplatesConfig.contains(path)) {
            player.sendMessage(ChatColor.RED + "The mob template '" + templateId + "' does not exist.");
            return;
        }

        mobTemplatesConfig.set(path + ".display-name", name);
        plugin.getConfigManager().saveMobTemplatesConfig();
        plugin.getConfigManager().loadAllConfigs();

        String coloredName = ChatColor.translateAlternateColorCodes('&', name);
        player.sendMessage(ChatColor.GREEN + "Set display name for template '" + templateId + "' to: " + coloredName);
    }
}