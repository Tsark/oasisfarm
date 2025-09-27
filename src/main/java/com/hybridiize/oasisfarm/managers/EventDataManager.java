package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class EventDataManager {
    private final Oasisfarm plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public EventDataManager(Oasisfarm plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        dataFile = new File(plugin.getDataFolder(), "event-data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create event-data.yml!", e);
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
            plugin.getLogger().log(Level.SEVERE, "Could not save event-data.yml!", e);
        }
    }

    public long getLastRan(String eventId) {
        return getConfig().getLong("events." + eventId + ".last-ran-timestamp", 0);
    }

    public void setLastRan(String eventId, long timestamp) {
        getConfig().set("events." + eventId + ".last-ran-timestamp", timestamp);
        saveData();
    }
}