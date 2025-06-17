package com.hybridiize.oasisfarm.commands.eventsubcommands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class EventStopCommand extends EventSubCommand {
    @Override
    public String getName() { return "stop"; }
    @Override
    public String getDescription() { return "Manually stops a running event."; }
    @Override
    public String getSyntax() { return "/of event stop <event_id>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }
        String eventId = args[2];

        if (!plugin.getEventManager().isEventRunning(eventId)) {
            player.sendMessage(ChatColor.RED + "Event '" + eventId + "' is not currently running.");
            return;
        }

        plugin.getEventManager().endEvent(eventId);
        player.sendMessage(ChatColor.GREEN + "Manually stopped event: " + eventId);
    }
}