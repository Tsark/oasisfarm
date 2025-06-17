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
    public String getDescription() { return "Sets the equipment for a mob type."; }
    @Override
    public String getSyntax() { return "/of mob setitem <template_id> <item...>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            player.sendMessage(ChatColor.RED + "Valid slots: HAND, OFFHAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS");
            return;
        }

        String templateId = args[2]; // No farm needed now
        String name = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));
        String slot = args[4].toUpperCase();
        String itemName = args[5].toUpperCase();

        if (!validSlots.contains(slot)) {
            player.sendMessage(ChatColor.RED + "Invalid slot '" + slot + "'.");
            player.sendMessage(ChatColor.RED + "Valid slots: HAND, OFFHAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS");
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

        FileConfiguration config = plugin.getConfig();
        String path = "farms." + farmId + ".mobs." + mobType;

        if (!config.contains(path)) {
            player.sendMessage(ChatColor.RED + "That mob/farm combination does not exist.");
            return;
        }

        // Use "null" to remove the item from the slot
        String itemToSet = (itemName.equalsIgnoreCase("AIR") || itemName.equalsIgnoreCase("NONE")) ? null : itemName;
        config.set(path + ".equipment." + slot, itemToSet);

        plugin.saveConfig();
        plugin.getConfigManager().loadFarms();
        player.sendMessage(ChatColor.GREEN + "Set " + slot + " for " + mobType + " in " + farmId + " to " + itemName + ".");
    }
}