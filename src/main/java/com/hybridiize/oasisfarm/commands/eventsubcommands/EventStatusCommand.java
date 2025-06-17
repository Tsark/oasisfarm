package com.hybridiize.oasisfarm.commands.eventsubcommands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Map;

public class EventStatusCommand extends EventSubCommand {
    @Override
    public String getName() { return "status"; }
    @Override
    public String getDescription() { return "Shows status of running and cooling down events."; }
    @Override
    public String getSyntax() { return "/of event status"; }

    @Override
    public void perform(Player player, String[] args) {
        player.sendMessage(ChatColor.GOLD + "--- Event Status ---");

        // Running Events
        Map<String, String> runningEvents = plugin.getEventManager().getRunningEventFarmMap();
        if (runningEvents.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "No events are currently running.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Running Events:");
            for (Map.Entry<String, String> entry : runningEvents.entrySet()) {
                player.sendMessage(ChatColor.AQUA + "- " + entry.getValue() + " (in farm: " + entry.getKey() + ")");
            }
        }

        // Cooldowns
        player.sendMessage(""); // Spacer
        Map<String, Long> cooldowns = plugin.getEventManager().getEventCooldowns();
        if (cooldowns.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "No events are on cooldown.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Event Cooldowns:");
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
                if (entry.getValue() > now) {
                    long timeLeft = (entry.getValue() - now) / 1000;
                    player.sendMessage(ChatColor.AQUA + "- " + entry.getKey() + ": " + timeLeft + "s remaining");
                }
            }
        }
    }
}