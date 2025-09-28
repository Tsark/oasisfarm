package com.hybridiize.oasisfarm.commands.eventsubcommands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class EventStopCommand extends EventSubCommand {
    @Override
    public String getName() { return "stop"; }

    @Override
    public String getDescription() { return "Manually stops the event running in a specific farm."; }

    @Override
    public String getSyntax() { return "/of event stop <farm_id>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }
        String farmId = args[2];

        // 1. Check if an event is running in the farm
        if (!plugin.getEventManager().isFarmRunningEvent(farmId)) {
            player.sendMessage(ChatColor.RED + "No event is currently running in the farm '" + farmId + "'.");
            return;
        }

        // 2. Stop the event (we will add the stopEventInFarm method next)
        plugin.getEventManager().stopEventInFarm(farmId);
        player.sendMessage(ChatColor.GREEN + "Manually stopped the event in farm: " + farmId);
    }
}