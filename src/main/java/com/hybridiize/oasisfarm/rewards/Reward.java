package com.hybridiize.oasisfarm.rewards;

import org.bukkit.entity.Player;

/**
 * The abstract blueprint for any reward type.
 */
public abstract class Reward {
    // The chance of this reward being given, from 0.0 to 1.0
    private final double chance;

    public Reward(double chance) {
        this.chance = chance;
    }

    public double getChance() {
        return chance;
    }

    /**
     * The method that will be called to give the reward to a player.
     * @param player The player to receive the reward.
     */
    public abstract void give(Player player);
}