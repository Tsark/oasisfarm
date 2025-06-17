package com.hybridiize.oasisfarm.commands;

import com.hybridiize.oasisfarm.commands.subcommands.*;
import com.hybridiize.oasisfarm.managers.PendingConfirmationManager; // <-- IMPORT
import com.hybridiize.oasisfarm.managers.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor {

    private final List<SubCommand> subCommands = new ArrayList<>();

    // THE CONSTRUCTOR NOW ACCEPTS BOTH MANAGERS
    public CommandManager(SelectionManager selectionManager, PendingConfirmationManager confirmationManager) {
        // Register all our commands here
        subCommands.add(new WandCommand());
        subCommands.add(new CreateCommand(selectionManager));
        subCommands.add(new RenameCommand());
        subCommands.add(new SetRegionCommand(confirmationManager)); // This will now be recognized
        subCommands.add(new ConfirmCommand(confirmationManager, selectionManager)); // This will now be recognized
        subCommands.add(new MobCommand());
        subCommands.add(new ListCommand());
        subCommands.add(new DeleteCommand());
        subCommands.add(new InfoCommand());
        subCommands.add(new TeleportCommand());
        subCommands.add(new ReloadCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(player);
            return true;
        }

        // Find and execute the correct sub-command
        for (SubCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(args[0])) {
                subCommand.perform(player, args);
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Unknown command. Use /of help for a list of commands.");
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "--- OasisFarm Help ---");
        for (SubCommand subCommand : subCommands) {
            player.sendMessage(ChatColor.AQUA + subCommand.getSyntax() + ChatColor.GRAY + " - " + subCommand.getDescription());
        }
    }
}