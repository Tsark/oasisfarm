package com.hybridiize.oasisfarm.commands.eventsubcommands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Set;

public class EventListCommand extends EventSubCommand {
    @Override
    public String getName() { return "list"; }

    @Override
    public String getDescription() { return "Lists all configured events."; }

    @Override
    public String getSyntax() { return "/of event list"; }

    @Override
    public void perform(Player player, String[] args) {
        Set<String> eventIds = plugin.getConfigManager().getEvents().keySet();
        if (eventIds.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no events configured in events.yml.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "--- Configured Events ---");
        for (String eventId : eventIds) {
            player.sendMessage(ChatColor.AQUA + "- " + eventId);
        }
    }
}