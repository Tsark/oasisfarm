package com.hybridiize.oasisfarm.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class Constants {

    public static final ItemStack WAND_ITEM = createWand();

    private static ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "OasisFarm Wand");
        meta.setLore(Arrays.asList(
                ChatColor.AQUA + "Left-Click to set Position 1",
                ChatColor.AQUA + "Right-Click to set Position 2"
        ));
        wand.setItemMeta(meta);
        return wand;
    }
}