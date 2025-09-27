package com.hybridiize.oasisfarm.rewards;

import com.hybridiize.oasisfarm.Oasisfarm;
import me.clip.placeholderapi.PlaceholderAPI;
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

        // Finalize the player variable for use in the runnable
        final Player targetPlayer = player;
        final String parsedCommand = PlaceholderAPI.setPlaceholders(targetPlayer, command);

        // Run the command on the main server thread to ensure thread safety
        new BukkitRunnable() {
            @Override
            public void run() {
                Oasisfarm.getInstance().getServer().dispatchCommand(
                        Oasisfarm.getInstance().getServer().getConsoleSender(),
                        parsedCommand
                );
            }
        }.runTask(Oasisfarm.getInstance());
    }
}