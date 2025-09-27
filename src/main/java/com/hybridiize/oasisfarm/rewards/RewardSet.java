package com.hybridiize.oasisfarm.rewards;

import java.util.List;

/**
 * Represents a "bundle" or "set" of rewards that are rolled for together.
 */
public class RewardSet {
    private final double chance;
    private final List<Reward> rewards;

    public RewardSet(double chance, List<Reward> rewards) {
        this.chance = chance;
        this.rewards = rewards;
    }

    public double getChance() {
        return chance;
    }

    public List<Reward> getRewards() {
        return rewards;
    }
}