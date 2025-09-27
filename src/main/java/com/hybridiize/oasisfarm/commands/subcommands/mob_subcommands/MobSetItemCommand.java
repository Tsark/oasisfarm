package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.List;

public class MobSetItemCommand extends MobSubCommand {
    private final List<String> validSlots = Arrays.asList("HAND", "OFFHAND", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");

    @Override
    public String getName() { return "setitem"; }

    @Override
    public String getDescription() { return "Sets the equipment for a mob template."; }

    @Override
    public String getSyntax() { return "/of mob setitem <template_id> <slot> <item_name|none>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            player.sendMessage(ChatColor.RED + "Valid slots: HAND, OFFHAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS");
            return;
        }

        String templateId = args[2];
        String slot = args[3].toUpperCase();
        String itemName = args[4].toUpperCase();

        if (!validSlots.contains(slot)) {
            player.sendMessage(ChatColor.RED + "Invalid slot '" + slot + "'.");
            return;
        }

        try {
            if (!itemName.equalsIgnoreCase("AIR") && !itemName.equalsIgnoreCase("NONE")) {
                Material.valueOf(itemName);
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid item name: " + itemName);
            return;
        }

        FileConfiguration mobTemplatesConfig = plugin.getConfigManager().getMobTemplatesConfig();
        String path = templateId;

        if (!mobTemplatesConfig.contains(path)) {
            player.sendMessage(ChatColor.RED + "The mob template '" + templateId + "' does not exist.");
            return;
        }

        String itemToSet = (itemName.equalsIgnoreCase("AIR") || itemName.equalsIgnoreCase("NONE")) ? null : itemName;
        mobTemplatesConfig.set(path + ".equipment." + slot, itemToSet);

        plugin.getConfigManager().saveMobTemplatesConfig();
        plugin.getConfigManager().loadAllConfigs();
        player.sendMessage(ChatColor.GREEN + "Set " + slot + " for template '" + templateId + "' to " + itemName + ".");
    }
}