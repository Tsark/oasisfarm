package com.hybridiize.oasisfarm.rewards;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * A reward that sends a formatted chat message to a player.
 */
public class MessageReward extends Reward {
    private final String message;

    public MessageReward(double chance, String message) {
        super(chance);
        this.message = message;
    }

    @Override
    public void give(Player player) {
        if (message == null || message.isEmpty()) return;
        String parsedMessage = PlaceholderAPI.setPlaceholders(player, message);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', parsedMessage));
    }
}