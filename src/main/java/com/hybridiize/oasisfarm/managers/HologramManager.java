package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2;
import com.hybridiize.oasisfarm.event.v2.Condition;
import com.hybridiize.oasisfarm.farm.Farm;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramManager {

    private final Oasisfarm plugin;
    private final boolean decentHologramsEnabled;
    private final Pattern numberPattern = Pattern.compile("\\d+\\.?\\d*");

    public HologramManager(Oasisfarm plugin) {
        this.plugin = plugin;
        this.decentHologramsEnabled = plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms");
        if (!decentHologramsEnabled) {
            plugin.getLogger().warning("DecentHolograms not found. Hologram feature will be disabled.");
        }
    }

    public void createOrUpdateFarmHologram(Farm farm, int currentMobs) {
        if (!decentHologramsEnabled) return;

        String hologramName = "oasisfarm-" + farm.getId();
        Location location = getHologramLocation(farm);

        List<String> lines = new ArrayList<>();
        String templateName = plugin.getConfig().getString("farms." + farm.getId() + ".hologram-template", "default-farm-hologram");
        ConfigurationSection hologramTemplate = plugin.getConfigManager().getHologramsConfig().getConfigurationSection("templates." + templateName);

        if (hologramTemplate == null) {
            plugin.getLogger().warning("Could not find 'templates.default-farm-hologram' in holograms.yml");
            return;
        }

        List<String> templateLines = hologramTemplate.getStringList("lines");

        // V2 Event System Integration
        ActiveEventTrackerV2 tracker = plugin.getEventManager().getActiveEventTracker(farm.getId());

        for (String line : templateLines) {
            String processedLine = ChatColor.translateAlternateColorCodes('&', line);
            processedLine = processedLine.replace("{farm_name}", farm.getId());
            processedLine = processedLine.replace("{farm_mobs_current}", String.valueOf(currentMobs));
            processedLine = processedLine.replace("{farm_mobs_max}", String.valueOf(farm.getMaxMobs()));

            if (tracker != null && tracker.getCurrentPhase() != null) {
                // Handle Event Placeholders
                processedLine = processedLine.replace("{event_name}", tracker.getEvent().getId());
                processedLine = processedLine.replace("{event_phase}", tracker.getCurrentPhase().getPhaseId());

                // Handle Progress Placeholders
                if (processedLine.contains("{event_progress")) {
                    processedLine = processEventProgressLine(processedLine, tracker);
                }
            } else {
                // If no event is running, remove event lines or replace with a default
                processedLine = processedLine.replace("{event_name}", "None");
                processedLine = processedLine.replace("{event_phase}", "N/A");
                if (processedLine.contains("{event_progress")) {
                    continue; // Don't add progress lines if no event is active
                }
            }
            lines.add(processedLine);
        }

        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram != null) {
            DHAPI.setHologramLines(hologram, lines);
        } else {
            DHAPI.createHologram(hologramName, location, lines);
        }
    }

    private String processEventProgressLine(String line, ActiveEventTrackerV2 tracker) {
        List<Condition> progressConditions = tracker.getCurrentPhase().getProgression().getConditions();

        for (Condition condition : progressConditions) {
            String type = condition.getType().toUpperCase();
            if (type.equals("MOB_KILLS_IN_FARM") || type.equals("DURATION")) {
                long currentValue = 0;
                long requiredValue = parseRequiredValue(condition.getValue());

                if (type.equals("MOB_KILLS_IN_FARM")) {
                    String mobId = condition.getProperties().get("mob_id");
                    if (mobId != null) {
                        currentValue = tracker.getMobKills(mobId);
                        line = line.replace("{event_progress_type}", "Kills");
                    }
                } else { // DURATION
                    currentValue = (System.currentTimeMillis() - tracker.getPhaseStartTime()) / 1000;
                    line = line.replace("{event_progress_type}", "Time");
                }

                line = line.replace("{event_progress_current}", String.valueOf(currentValue));
                line = line.replace("{event_progress_required}", String.valueOf(requiredValue));
                return line; // Return after processing the first valid condition
            }
        }

        // If no progress condition was found, remove the line
        return null;
    }

    private long parseRequiredValue(String valueString) {
        Matcher matcher = numberPattern.matcher(valueString);
        if (matcher.find()) {
            return Long.parseLong(matcher.group());
        }
        return 0;
    }

    public void removeFarmHologram(String farmId) {
        if (!decentHologramsEnabled) return;

        String hologramName = "oasisfarm-" + farmId;
        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram != null) {
            hologram.delete();
        }
    }

    public void removeAllHolograms() {
        if (!decentHologramsEnabled) return;
        if (plugin.getConfigManager().getFarms() == null) return;
        for (Farm farm : plugin.getConfigManager().getFarms().values()) {
            removeFarmHologram(farm.getId());
        }
    }



    private Location getHologramLocation(Farm farm) {
        Location pos1 = farm.getRegion().getPos1();
        Location pos2 = farm.getRegion().getPos2();
        double x = (pos1.getX() + pos2.getX()) / 2.0;
        double y = Math.max(pos1.getY(), pos2.getY()) + 3.0;
        double z = (pos1.getZ() + pos2.getZ()) / 2.0;
        return new Location(pos1.getWorld(), x, y, z);
    }
}