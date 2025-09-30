package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2;
import com.hybridiize.oasisfarm.event.v2.Condition;
import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionManager {

    private final Oasisfarm plugin;

    public ConditionManager(Oasisfarm plugin) {
        this.plugin = plugin;
    }

    /**
     * The main method to check if a list of conditions is met.
     * @param conditions The list of conditions from the config.
     * @param mode "AND" or "OR".
     * @param farm The farm the event is running in.
     * @param tracker The active event tracker (can be null for trigger checks).
     * @return True if the conditions are met, false otherwise.
     */
    public boolean areConditionsMet(List<Condition> conditions, String mode, Farm farm, ActiveEventTrackerV2 tracker) {
        if (conditions == null || conditions.isEmpty()) {
            // If checking for triggers, no conditions means it can't trigger.
            // If checking for progression, no conditions means it can't progress.
            return false;
        }

        boolean isOrMode = "OR".equalsIgnoreCase(mode);

        for (Condition condition : conditions) {
            boolean result = check(condition, farm, tracker);

            if (isOrMode && result) {
                return true; // In OR mode, we only need one success.
            }
            if (!isOrMode && !result) {
                return false; // In AND mode, we only need one failure.
            }
        }

        return !isOrMode;
    }

    /**
     * Checks a single condition.
     */
    private boolean check(Condition condition, Farm farm, ActiveEventTrackerV2 tracker) {
        switch (condition.getType().toUpperCase()) {
            case "PLAYER_COUNT_IN_FARM":
                return checkPlayerCount(condition, farm);
            case "TIME_OF_DAY":
                return checkTimeOfDay(condition, farm);
            case "TOTAL_KILLS_IN_FARM":
                return checkTotalKills(condition, farm);
            case "DURATION":
                return checkDuration(condition, tracker);
            case "MOB_KILLS_IN_FARM":
                return checkMobKills(condition, tracker);
            default:
                plugin.getLogger().warning("Unknown event condition type: " + condition.getType());
                return false;
        }
    }

    // --- Private Helper Methods for each Condition Type ---

    private boolean checkPlayerCount(Condition condition, Farm farm) {
        long playerCount = 0;
        World farmWorld = farm.getRegion().getPos1().getWorld();
        if (farmWorld != null) {
            for (Player player : farmWorld.getPlayers()) {
                if (farm.getRegion().contains(player.getLocation())) {
                    playerCount++;
                }
            }
        }
        return compareNumeric(playerCount, condition.getValue());
    }

    private boolean checkTimeOfDay(Condition condition, Farm farm) {
        World farmWorld = farm.getRegion().getPos1().getWorld();
        if (farmWorld == null) return false;

        long time = farmWorld.getTime();
        String requiredTime = condition.getValue().toUpperCase();

        boolean isDay = time >= 0 && time < 12300;
        boolean isNight = time >= 12300 && time < 23850;

        if (requiredTime.equals("DAY")) return isDay;
        if (requiredTime.equals("NIGHT")) return isNight;
        return false;
    }

    private boolean checkTotalKills(Condition condition, Farm farm) {
        int currentKills = plugin.getFarmDataManager().getKillCount(farm.getId());
        return compareNumeric(currentKills, condition.getValue());
    }

    private boolean checkDuration(Condition condition, ActiveEventTrackerV2 tracker) {
        if (tracker == null) return false; // Duration is only valid during an event phase

        long phaseStartTime = tracker.getPhaseStartTime();
        long elapsedMillis = System.currentTimeMillis() - phaseStartTime;

        // Parse the duration from the config value (e.g., "300s")
        String valueString = condition.getValue().toLowerCase().replace("s", "");
        try {
            long requiredSeconds = Long.parseLong(valueString);
            return compareNumeric(elapsedMillis / 1000.0, ">= " + requiredSeconds);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid duration format: " + condition.getValue());
            return false;
        }
    }

    private boolean checkMobKills(Condition condition, ActiveEventTrackerV2 tracker) {
        if (tracker == null) return false; // Mob kills are only tracked during an event

        String mobId = condition.getProperties().get("mob_id");
        if (mobId == null) {
            plugin.getLogger().warning("MOB_KILLS_IN_FARM condition is missing 'mob_id' property.");
            return false;
        }

        int currentKills = tracker.getMobKills(mobId);
        return compareNumeric(currentKills, condition.getValue());
    }


    /**
     * A powerful helper method to compare numbers using operators from the config.
     * Example: valueString = ">= 10"
     * @param current The current value from the game (e.g., player count).
     * @param valueString The string from the config (e.g., ">= 10").
     * @return True if the comparison is successful.
     */
    private boolean compareNumeric(double current, String valueString) {
        // Regex to extract the operator and the number
        Pattern pattern = Pattern.compile("([<>=!]+)\\s*(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(valueString);

        if (!matcher.find()) {
            plugin.getLogger().warning("Invalid numeric condition format: " + valueString);
            return false;
        }

        String operator = matcher.group(1);
        double required = Double.parseDouble(matcher.group(2));

        switch (operator) {
            case "==":
            case "=":
                return current == required;
            case "!=":
                return current != required;
            case ">":
                return current > required;
            case "<":
                return current < required;
            case ">=":
                return current >= required;
            case "<=":
                return current <= required;
            default:
                return false;
        }
    }
}