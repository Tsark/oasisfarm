package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class FarmDataManager {
    private final Oasisfarm plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public FarmDataManager(Oasisfarm plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        dataFile = new File(plugin.getDataFolder(), "farm-data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create farm-data.yml!", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getConfig() {
        return dataConfig;
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save farm-data.yml!", e);
        }
    }

    public int getKillCount(String farmId) {
        return getConfig().getInt("farms." + farmId + ".total-kills", 0);
    }

    public void incrementKillCount(String farmId) {
        int currentKills = getKillCount(farmId);
        getConfig().set("farms." + farmId + ".total-kills", currentKills + 1);
        // Note: We save data centrally on shutdown to reduce disk I/O.
    }

    public void resetKillCount(String farmId) {
        getConfig().set("farms." + farmId + ".total-kills", 0);
    }
}