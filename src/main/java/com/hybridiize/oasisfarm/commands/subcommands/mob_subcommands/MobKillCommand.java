package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MobKillCommand extends MobSubCommand {
    @Override
    public String getName() { return "kill"; }

    @Override
    public String getDescription() { return "Kills tracked mobs in a farm or globally."; }

    @Override
    public String getSyntax() { return "/of mob kill <farm_id | all> [template_id | all]"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String farmId = args[2];
        String templateId = (args.length > 3) ? args[3] : "all";

        // We will add the killTrackedMobs method to the FarmManager next
        int killCount = plugin.getFarmManager().killTrackedMobs(farmId, templateId);

        player.sendMessage(ChatColor.GREEN + "Successfully removed " + killCount + " mobs matching the criteria.");
    }
}