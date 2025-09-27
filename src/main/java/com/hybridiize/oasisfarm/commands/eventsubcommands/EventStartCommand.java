package com.hybridiize.oasisfarm.commands.eventsubcommands;

import com.hybridiize.oasisfarm.event.OasisEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class EventStartCommand extends EventSubCommand {
    @Override
    public String getName() { return "start"; }

    @Override
    public String getDescription() { return "Manually starts an event, bypassing conditions."; }

    @Override
    public String getSyntax() { return "/of event start <event_id>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }
        String eventId = args[2];
        OasisEvent event = plugin.getConfigManager().getEvents().get(eventId);

        if (event == null) {
            player.sendMessage(ChatColor.RED + "Event '" + eventId + "' not found in events.yml.");
            return;
        }

        if (plugin.getEventManager().isEventRunning(eventId)) {
            player.sendMessage(ChatColor.RED + "Event '" + eventId + "' is already running.");
            return;
        }

        plugin.getEventManager().startEvent(event);
        player.sendMessage(ChatColor.GREEN + "Manually started event: " + eventId);
    }
}