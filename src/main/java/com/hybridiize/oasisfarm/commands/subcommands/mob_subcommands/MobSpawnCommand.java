package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import com.hybridiize.oasisfarm.farm.MobInfo;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import org.bukkit.entity.Entity;

public class MobSpawnCommand extends MobSubCommand {
    @Override
    public String getName() { return "spawn"; }

    @Override
    public String getDescription() { return "Manually spawns a mob from a template."; }

    @Override
    public String getSyntax() { return "/of mob spawn <template_id> [farm_id]"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String templateId = args[2];
        MobInfo mobInfo = plugin.getConfigManager().getMobTemplate(templateId);

        if (mobInfo == null) {
            player.sendMessage(ChatColor.RED + "The mob template '" + templateId + "' does not exist.");
            return;
        }

        Location spawnLocation = player.getLocation();
        LivingEntity spawnedMob = null;

        // --- NEW SPAWNING LOGIC BRANCH ---
        if (plugin.isMythicMobsEnabled() && mobInfo.getMobType().equals("MYTHIC")) {
            try {
                Entity entity = MythicProvider.get().getAPIHelper().spawnMythicMob(mobInfo.getMythicId(), spawnLocation, mobInfo.getMythicLevel());
                if (entity instanceof LivingEntity) {
                    spawnedMob = (LivingEntity) entity;
                }
            } catch (InvalidMobTypeException e) {
                player.sendMessage(ChatColor.RED + "Failed to spawn Mythic Mob. Is the ID '" + mobInfo.getMythicId() + "' correct?");
                return;
            }
        } else {
            // Fallback to vanilla spawning
            spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, mobInfo.getType());
            // Apply our custom attributes only to our manually spawned vanilla mobs
            plugin.getFarmManager().applyMobAttributes(spawnedMob, mobInfo);
        }

        if (spawnedMob == null) {
            player.sendMessage(ChatColor.RED + "An unknown error occurred while trying to spawn the mob.");
            return;
        }

        // Optional tracking logic (same as before)
        if (args.length >= 4) {
            String farmId = args[3];
            if (plugin.getConfigManager().getFarms().containsKey(farmId)) {
                plugin.getFarmManager().trackMob(spawnedMob, farmId, templateId);
                player.sendMessage(ChatColor.GREEN + "Spawned 1x " + templateId + " and tracked to farm '" + farmId + "'.");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Spawned 1x " + templateId + ". Farm '" + farmId + "' not found, so mob is untracked.");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "Spawned 1x " + templateId + ". This mob is untracked.");
        }
    }
}