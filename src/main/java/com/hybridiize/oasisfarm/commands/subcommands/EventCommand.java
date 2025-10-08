package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.commands.eventsubcommands.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class EventCommand extends SubCommand {
    private final List<EventSubCommand> subCommands = new ArrayList<>();

    public EventCommand() {
        subCommands.add(new EventListCommand());
        subCommands.add(new EventStartCommand());
        subCommands.add(new EventStopCommand());
        subCommands.add(new EventStatusCommand());
    }

    @Override
    public String getName() { return "event"; }

    @Override
    public String getDescription() { return "Manages OasisFarm events."; }

    @Override
    public String getSyntax() { return "/of event <subcommand> [...]"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        for (EventSubCommand sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(args[1])) {
                sub.perform(player, args);
                return;
            }
        }
        sendHelp(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- OasisFarm Event Commands ---", NamedTextColor.GOLD));
        for (EventSubCommand sub : subCommands) {
            player.sendMessage(
                    Component.text(sub.getSyntax(), NamedTextColor.AQUA)
                            .append(Component.text(" - ", NamedTextColor.GRAY))
                            .append(Component.text(sub.getDescription(), NamedTextColor.GRAY))
            );
        }
    }
}