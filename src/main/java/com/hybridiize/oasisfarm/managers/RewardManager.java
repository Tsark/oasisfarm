package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.rewards.PendingRewards;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RewardManager {
    private final Oasisfarm plugin;
    private final ConcurrentHashMap<UUID, PendingRewards> pendingRewardsMap = new ConcurrentHashMap<>();

    public RewardManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startPayoutTicker();
    }

    public void addPendingMoney(Player player, double amount) {
        pendingRewardsMap.computeIfAbsent(player.getUniqueId(), k -> new PendingRewards()).addMoney(amount);
    }

    public void addPendingExp(Player player, int amount) {
        pendingRewardsMap.computeIfAbsent(player.getUniqueId(), k -> new PendingRewards()).addExp(amount);
    }

    public void incrementKillCount(Player player) {
        pendingRewardsMap.computeIfAbsent(player.getUniqueId(), k -> new PendingRewards()).incrementKillCount();
    }

    private void startPayoutTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                payoutAll();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 30, 20L * 30); // Payout every 30 seconds
    }

    private void payoutAll() {
        if (pendingRewardsMap.isEmpty()) return;

        for (UUID uuid : pendingRewardsMap.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PendingRewards rewards = pendingRewardsMap.remove(uuid);
                if (rewards == null) continue;

                if (rewards.getPendingMoney() > 0) {
                    final double moneyToGive = rewards.getPendingMoney();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.getEconomyManager().deposit(player, moneyToGive);
                        }
                    }.runTask(plugin);
                }

                if (rewards.getPendingExp() > 0) {
                    final int expToGive = rewards.getPendingExp();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.giveExp(expToGive);
                        }
                    }.runTask(plugin);
                }

                if (rewards.getMobKillCount() > 0) {
                    // We run the message synchronously to avoid any potential async chat issues.
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&a&l[OasisFarm] &7You earned &e$" + String.format("%.2f", rewards.getPendingMoney()) +
                                            " &7and &b" + rewards.getPendingExp() + " XP &7from slaying &d" +
                                            rewards.getMobKillCount() + " &7mobs."));
                        }
                    }.runTask(plugin);
                }
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        PendingRewards rewards = pendingRewardsMap.remove(player.getUniqueId());
        if (rewards != null && rewards.getPendingMoney() > 0) {
            // This is called from a synchronous event, so no need for a runnable.
            plugin.getEconomyManager().deposit(player, rewards.getPendingMoney());
        }
    }
}