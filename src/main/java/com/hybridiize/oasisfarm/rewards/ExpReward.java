package com.hybridiize.oasisfarm.rewards;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

/**
 * A reward that gives a player a pending amount of experience points via the RewardManager.
 */
public class ExpReward extends Reward {
    private final int amount;

    public ExpReward(double chance, int amount) {
        super(chance);
        this.amount = amount;
    }

    @Override
    public void give(Player player) {
        // Instead of giving directly, add it to the pending reward manager.
        Oasisfarm.getInstance().getRewardManager().addPendingExp(player, this.amount);
    }
}