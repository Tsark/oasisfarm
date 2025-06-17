package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MobCommand extends SubCommand {
    private final List<MobSubCommand> subCommands = new ArrayList<>();

    public MobCommand() {
        // Register our mob-specific commands
        subCommands.add(new MobListCommand());
        subCommands.add(new MobAddCommand());
        subCommands.add(new MobRemoveCommand());subCommands.add(new MobSetNameCommand());
        subCommands.add(new MobSetHealthCommand());
        subCommands.add(new MobSetItemCommand());
        subCommands.add(new MobRebalanceCommand());
    }

    @Override
    public String getName() { return "mob"; }
    @Override
    public String getDescription() { return "Manages mobs within a farm."; }
    @Override
    public String getSyntax() { return "/of mob <list|add|remove> ..."; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        for (MobSubCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(args[1])) {
                subCommand.perform(player, args);
                return;
            }
        }

        sendHelp(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "--- OasisFarm Mob Commands ---");
        for (MobSubCommand subCommand : subCommands) {
            player.sendMessage(ChatColor.AQUA + subCommand.getSyntax() + ChatColor.GRAY + " - " + subCommand.getDescription());
        }
    }
}