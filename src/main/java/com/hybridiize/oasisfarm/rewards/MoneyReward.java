package com.hybridiize.oasisfarm.rewards;

import com.hybridiize.oasisfarm.Oasisfarm;
import org.bukkit.entity.Player;

/**
 * A reward that gives a player a pending amount of money via the RewardManager.
 */
public class MoneyReward extends Reward {
    private final double amount;

    public MoneyReward(double chance, double amount) {
        super(chance);
        this.amount = amount;
    }

    @Override
    public void give(Player player) {
        // Instead of giving directly, add it to the pending reward manager.
        Oasisfarm.getInstance().getRewardManager().addPendingMoney(player, this.amount);
    }
}