package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;

public class HologramManager {

    private final Oasisfarm plugin;
    private final boolean decentHologramsEnabled;

    public HologramManager(Oasisfarm plugin) {
        this.plugin = plugin;
        // Check if the DecentHolograms plugin is actually running on the server
        this.decentHologramsEnabled = plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms");
        if (!decentHologramsEnabled) {
            plugin.getLogger().warning("DecentHolograms not found. Hologram feature will be disabled.");
        }
    }

    public void createOrUpdateFarmHologram(Farm farm, int currentMobs) {
        if (!decentHologramsEnabled) return;

        String hologramName = "oasisfarm-" + farm.getId();
        Location location = getHologramLocation(farm);
        List<String> lines = Arrays.asList(
                ChatColor.AQUA + "" + ChatColor.BOLD + farm.getId(),
                ChatColor.GRAY + "Mobs: " + ChatColor.GREEN + currentMobs + ChatColor.GRAY + "/" + ChatColor.YELLOW + farm.getMaxMobs()
        );

        // Check if the hologram already exists
        if (DHAPI.getHologram(hologramName) != null) {
            // It exists, so we just update the lines
            Hologram hologram = DHAPI.getHologram(hologramName);
            DHAPI.setHologramLines(hologram, lines);
        } else {
            // It doesn't exist, so we create a new one
            DHAPI.createHologram(hologramName, location, lines);
        }
    }

    public void removeFarmHologram(String farmId) {
        if (!decentHologramsEnabled) return;

        String hologramName = "oasisfarm-" + farmId;
        // First, we get the Hologram object from the API
        Hologram hologram = DHAPI.getHologram(hologramName);

        // Then, we check if the object actually exists (is not null)
        if (hologram != null) {
            // If it exists, we call the delete() method on the object itself
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
        // Calculate the center of the farm and place the hologram a bit above it
        Location pos1 = farm.getRegion().getPos1();
        Location pos2 = farm.getRegion().getPos2();

        double x = (pos1.getX() + pos2.getX()) / 2.0;
        double y = Math.max(pos1.getY(), pos2.getY()) + 3.0; // 3 blocks above the highest point
        double z = (pos1.getZ() + pos2.getZ()) / 2.0;

        return new Location(pos1.getWorld(), x, y, z);
    }
}