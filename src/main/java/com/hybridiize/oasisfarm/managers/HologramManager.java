package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.OasisEvent;
import com.hybridiize.oasisfarm.farm.Farm;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HologramManager {

    private final Oasisfarm plugin;
    private final boolean decentHologramsEnabled;

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

        List<String> lines = new ArrayList<>(Arrays.asList(
                ChatColor.AQUA + "" + ChatColor.BOLD + farm.getId(),
                ChatColor.GRAY + "Mobs: " + ChatColor.GREEN + currentMobs + ChatColor.GRAY + "/" + ChatColor.YELLOW + farm.getMaxMobs()
        ));

        for (OasisEvent event : plugin.getConfigManager().getEvents().values()) {
            if (event.getTargetFarm().equals(farm.getId())) {
                String eventPath = "events." + event.getId();
                ConfigurationSection progressSection = plugin.getConfigManager().getEventsConfig().getConfigurationSection(eventPath + ".progress-tracking");

                if (progressSection != null && progressSection.getBoolean("hologram-enabled", false)) {
                    String trackedConditionType = progressSection.getString("tracked-condition", "");
                    String lineFormat = progressSection.getString("hologram-line");

                    if (trackedConditionType.equalsIgnoreCase("TOTAL_KILLS_IN_FARM") && lineFormat != null) {
                        String requiredValueStr = plugin.getConfigManager().getEventsConfig().getString(eventPath + ".conditions.TOTAL_KILLS_IN_FARM");
                        if (requiredValueStr != null) {
                            int requiredValue = Integer.parseInt(requiredValueStr);
                            int currentValue = plugin.getFarmDataManager().getKillCount(farm.getId());

                            String progressLine = lineFormat
                                    .replace("{current_value}", String.valueOf(currentValue))
                                    .replace("{required_value}", String.valueOf(requiredValue));

                            lines.add(ChatColor.translateAlternateColorCodes('&', progressLine));
                        }
                        break;
                    }
                }
            }
        }

        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram != null) {
            DHAPI.setHologramLines(hologram, lines);
        } else {
            DHAPI.createHologram(hologramName, location, lines);
        }
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