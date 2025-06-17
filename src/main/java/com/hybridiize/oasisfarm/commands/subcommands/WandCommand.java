package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.util.Constants;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WandCommand extends SubCommand {
    @Override
    public String getName() { return "wand"; }
    @Override
    public String getDescription() { return "Gives you the farm selection wand."; }
    @Override
    public String getSyntax() { return "/of wand"; }

    @Override
    public void perform(Player player, String[] args) {
        player.getInventory().addItem(Constants.WAND_ITEM);
        player.sendMessage(ChatColor.GREEN + "You have received the OasisFarm Wand!");
    }
}