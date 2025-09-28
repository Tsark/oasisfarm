package com.hybridiize.oasisfarm.commands.eventsubcommands;

import com.hybridiize.oasisfarm.event.v2.OasisEventV2;
import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class EventStartCommand extends EventSubCommand {
    @Override
    public String getName() { return "start"; }

    @Override
    public String getDescription() { return "Manually starts an event in a specific farm."; }

    @Override
    public String getSyntax() { return "/of event start <event_id> <farm_id>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }
        String eventId = args[2];
        String farmId = args[3];

        // 1. Check if the event exists
        OasisEventV2 event = plugin.getConfigManager().getEventV2(eventId);
        if (event == null) {
            player.sendMessage(ChatColor.RED + "Event '" + eventId + "' not found in events.yml.");
            return;
        }

        // 2. Check if the farm exists
        Farm farm = plugin.getConfigManager().getFarms().get(farmId);
        if (farm == null) {
            player.sendMessage(ChatColor.RED + "Farm '" + farmId + "' not found in config.yml.");
            return;
        }

        // 3. Check if an event is already running in that farm
        if (plugin.getEventManager().isFarmRunningEvent(farmId)) {
            player.sendMessage(ChatColor.RED + "An event is already running in the farm '" + farmId + "'.");
            return;
        }

        // 4. Start the event
        plugin.getEventManager().startEvent(event, farm);
        player.sendMessage(ChatColor.GREEN + "Manually started event '" + eventId + "' in farm '" + farmId + "'.");
    }
}