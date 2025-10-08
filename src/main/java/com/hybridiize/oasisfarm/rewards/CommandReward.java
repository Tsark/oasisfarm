package com.hybridiize.oasisfarm.rewards;

import com.hybridiize.oasisfarm.Oasisfarm;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

        new BukkitRunnable() {
            @Override
            public void run() {
                String parsedCommand = PlaceholderAPI.setPlaceholders(targetPlayer, command);
                Oasisfarm.getInstance().getServer().dispatchCommand(
                        Oasisfarm.getInstance().getServer().getConsoleSender(),
                        parsedCommand
                );
            }
        }.runTask(Oasisfarm.getInstance());
    }
}