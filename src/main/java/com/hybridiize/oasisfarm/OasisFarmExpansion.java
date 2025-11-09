package com.hybridiize.oasisfarm;

import com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2;
import com.hybridiize.oasisfarm.event.v2.Condition;
import com.hybridiize.oasisfarm.farm.Farm;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OasisFarmExpansion extends PlaceholderExpansion {

    private final Oasisfarm plugin;
    private final Pattern numberPattern = Pattern.compile("\\d+\\.?\\d*");

    public OasisFarmExpansion(Oasisfarm plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "oasisfarm"; // This is the prefix, e.g., %oasisfarm_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "Hybridiize"; // Or your name
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // The placeholders don't depend on a player being online
    }

    /**
     * This is the core logic for the placeholders.
     * Placeholders will be in the format: %oasisfarm_<type>_<farm_id>%
     * Example: %oasisfarm_mobs_current_starter_zone%
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Split the placeholder params
        String[] parts = params.split("_");
        if (parts.length < 2) {
            return null; // Invalid format
        }

        // The last part is always the farm ID
        String farmId = parts[parts.length - 1];
        Farm farm = plugin.getConfigManager().getFarms().get(farmId);
        if (farm == null) {
            return "Invalid Farm";
        }

        // Rebuild the "type" (e.g., "mobs_current", "event_name")
        String type = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));

        switch (type.toLowerCase()) {
            case "mobs_current":
                return String.valueOf(plugin.getFarmManager().getTrackedMobCount(farmId));

            case "mobs_max":
                return String.valueOf(farm.getMaxMobs());

            // Event-related placeholders
            case "event_name": {
                ActiveEventTrackerV2 tracker = plugin.getEventManager().getActiveEventTracker(farmId);
                return (tracker != null) ? tracker.getEvent().getId() : "None";
            }

            case "event_phase": {
                ActiveEventTrackerV2 tracker = plugin.getEventManager().getActiveEventTracker(farmId);
                return (tracker != null && tracker.getCurrentPhase() != null) ? tracker.getCurrentPhase().getPhaseId() : "N/A";
            }

            // Progress placeholders
            case "event_progress_type":
            case "event_progress_current":
            case "event_progress_required":
                return getProgressPlaceholder(farmId, type.toLowerCase());

            default:
                return null; // Unknown placeholder
        }
    }

    private String getProgressPlaceholder(String farmId, String type) {
        ActiveEventTrackerV2 tracker = plugin.getEventManager().getActiveEventTracker(farmId);
        if (tracker == null || tracker.getCurrentPhase() == null) {
            return (type.equals("event_progress_type")) ? "Progress" : "0";
        }

        List<Condition> conditions = tracker.getCurrentPhase().getProgression().getConditions();
        if (conditions == null) {
            return (type.equals("event_progress_type")) ? "Progress" : "0";
        }

        for (Condition condition : conditions) {
            String conditionType = condition.getType().toUpperCase();
            if (conditionType.equals("MOB_KILLS_IN_FARM") || conditionType.equals("DURATION")) {
                long required = parseRequiredValue(condition.getValue());
                long current = 0;
                String progressType = "Progress";

                if (conditionType.equals("MOB_KILLS_IN_FARM")) {
                    String mobId = condition.getProperties().get("mob_id");
                    if (mobId != null) {
                        current = tracker.getMobKills(mobId);
                        progressType = "Kills";
                    }
                } else { // DURATION
                    current = (System.currentTimeMillis() - tracker.getPhaseStartTime()) / 1000;
                    progressType = "Time";
                }

                switch (type) {
                    case "event_progress_type":
                        return progressType;
                    case "event_progress_current":
                        return String.valueOf(current);
                    case "event_progress_required":
                        return String.valueOf(required);
                }
            }
        }

        // No valid progress condition found
        return (type.equals("event_progress_type")) ? "Progress" : "0";
    }

    private long parseRequiredValue(String valueString) {
        Matcher matcher = numberPattern.matcher(valueString);
        if (matcher.find()) {
            return Long.parseLong(matcher.group());
        }
        return 0;
    }
}