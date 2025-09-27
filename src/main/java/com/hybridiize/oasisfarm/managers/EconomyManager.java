package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

public class EconomyManager {
    private final Oasisfarm plugin;
    private Economy economy = null;

    public EconomyManager(Oasisfarm plugin) {
        this.plugin = plugin;
        if (!setupEconomy()) {
            plugin.getLogger().log(Level.WARNING, "Vault not found or no Economy plugin hooked. Economy features will be disabled.");
        } else {
            plugin.getLogger().info("Successfully hooked into Vault Economy.");
        }
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false; // Vault is not on the server.
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false; // No economy plugin has registered with Vault.
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (!hasEconomy() || amount <= 0) return;

        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Vault transaction failed for " + player.getName() + ": " + response.errorMessage);
        }
    }
}