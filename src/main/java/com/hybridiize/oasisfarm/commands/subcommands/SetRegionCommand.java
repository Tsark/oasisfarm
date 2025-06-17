package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.managers.PendingConfirmationManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SetRegionCommand extends SubCommand {
    private final PendingConfirmationManager confirmationManager;

    public SetRegionCommand(PendingConfirmationManager confirmationManager) {
        this.confirmationManager = confirmationManager;
    }

    @Override
    public String getName() { return "setregion"; }
    @Override
    public String getDescription() { return "Begins the process of redefining a farm's area."; }
    @Override
    public String getSyntax() { return "/of setregion <farm_name>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String farmId = args[1];
        if (!plugin.getConfigManager().getFarms().containsKey(farmId)) {
            player.sendMessage(ChatColor.RED + "The farm '" + farmId + "' does not exist.");
            return;
        }

        confirmationManager.setPendingResize(player, farmId);

        player.sendMessage(ChatColor.GOLD + "Now redefining region for '" + farmId + "'.");
        player.sendMessage(ChatColor.AQUA + "Use the wand to select two new points, then type " + ChatColor.YELLOW + "/of confirm" + ChatColor.AQUA + " to save.");
    }
}