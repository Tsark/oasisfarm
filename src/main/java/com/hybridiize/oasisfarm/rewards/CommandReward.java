package com.hybridiize.oasisfarm.rewards;

import com.hybridiize.oasisfarm.Oasisfarm;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A reward that executes a server command from the console.
 */
public class CommandReward extends Reward {
    private final String command;

    public CommandReward(double chance, String command) {
        super(chance);
        this.command = command;
    }

    @Override
    public void give(Player player) {
        if (command == null || command.isEmpty()) return;

        final Player targetPlayer = player;
        String parsedCommand = command; // Default to the raw command

        // Check if PlaceholderAPI is running before trying to use it
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            parsedCommand = PlaceholderAPI.setPlaceholders(targetPlayer, command);
        }

        final String finalCommand = parsedCommand;

        new BukkitRunnable() {
            @Override
            public void run() {
                Oasisfarm.getInstance().getServer().dispatchCommand(
                        Oasisfarm.getInstance().getServer().getConsoleSender(),
                        finalCommand
                );
            }
        }.runTask(Oasisfarm.getInstance());
    }
}