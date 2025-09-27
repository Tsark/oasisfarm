package com.hybridiize.oasisfarm.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class Constants {

    public static final ItemStack WAND_ITEM = createWand();

    private static ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "OasisFarm Wand");
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.AQUA + "Left-Click Block" + ChatColor.GRAY + " to set Position 1",
                    ChatColor.AQUA + "Right-Click Block" + ChatColor.GRAY + " to set Position 2"
            ));
            // Add a flag to hide enchantment glint if we add one later for looks
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    // Private constructor to prevent instantiation of this utility class
    private Constants() {}
}