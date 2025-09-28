package com.hybridiize.oasisfarm.commands.eventsubcommands;

import com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Map;

public class EventStatusCommand extends EventSubCommand {
    @Override
    public String getName() { return "status"; }

    @Override
    public String getDescription() { return "Shows the status of all currently running events."; }

    @Override
    public String getSyntax() { return "/of event status"; }

    @Override
    public void perform(Player player, String[] args) {
        player.sendMessage(ChatColor.GOLD + "--- OasisFarm Event Status ---");

        // Get the new map of active events
        Map<String, ActiveEventTrackerV2> runningEvents = plugin.getEventManager().getActiveFarmEvents();

        if (runningEvents.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "No events are currently running.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Running Events:");
        for (Map.Entry<String, ActiveEventTrackerV2> entry : runningEvents.entrySet()) {
            String farmId = entry.getKey();
            ActiveEventTrackerV2 tracker = entry.getValue();

            String eventId = tracker.getEvent().getId();
            String currentPhaseId = tracker.getCurrentPhase() != null ? tracker.getCurrentPhase().getPhaseId() : "N/A";
            long phaseStartTime = tracker.getPhaseStartTime();
            long timeInPhase = (System.currentTimeMillis() - phaseStartTime) / 1000; // in seconds

            player.sendMessage(ChatColor.AQUA + "- Farm: " + ChatColor.WHITE + farmId);
            player.sendMessage(ChatColor.GRAY + "  Event: " + ChatColor.WHITE + eventId);
            player.sendMessage(ChatColor.GRAY + "  Current Phase: " + ChatColor.WHITE + currentPhaseId);
            player.sendMessage(ChatColor.GRAY + "  Time in Phase: " + ChatColor.WHITE + timeInPhase + "s");
        }
    }
}