package com.hybridiize.oasisfarm.rewards;

/**
 * A simple data container to hold aggregated rewards for a single player.
 */
public class PendingRewards {
    private double pendingMoney = 0.0;
    private int pendingExp = 0;
    private int mobKillCount = 0;

    public void addMoney(double amount) {
        this.pendingMoney += amount;
    }

    public void addExp(int amount) {
        this.pendingExp += amount;
    }

    public void incrementKillCount() {
        this.mobKillCount++;
    }

    public double getPendingMoney() {
        return pendingMoney;
    }

    public int getPendingExp() {
        return pendingExp;
    }

    public int getMobKillCount() {
        return mobKillCount;
    }
}