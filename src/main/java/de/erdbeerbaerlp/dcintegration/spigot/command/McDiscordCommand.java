package de.erdbeerbaerlp.dcintegration.spigot.command;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.event.ClickEvent;
import dcshadow.net.kyori.adventure.text.event.HoverEvent;
import dcshadow.net.kyori.adventure.text.format.Style;
import dcshadow.org.jetbrains.annotations.NotNull;
import dcshadow.org.jetbrains.annotations.Nullable;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.MCSubCommand;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class McDiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Configuration.instance().ingameCommand.message).style(Style.empty().clickEvent(ClickEvent.openUrl(Configuration.instance().ingameCommand.inviteURL)).hoverEvent(HoverEvent.showText(Component.text(Configuration.instance().ingameCommand.hoverMessage))))));
        } else {
            for (MCSubCommand mcSubCommand : McCommandRegistry.getCommands()) {

                if (args[0].equals(mcSubCommand.getName())) {
                    final String[] cmdArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                    switch (mcSubCommand.getType()) {
                        case CONSOLE_ONLY:
                            if ((sender instanceof final ConsoleCommandSender cmd)) {
                                sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(mcSubCommand.execute(cmdArgs, null)));
                            }else sender.sendMessage(Localization.instance().commands.consoleOnly);
                            return true;
                        case PLAYER_ONLY:
                            if ((sender instanceof final Player p)) {
                                if (!mcSubCommand.needsOP()) {
                                    sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(mcSubCommand.execute(cmdArgs, p.getUniqueId())));
                                }else if(p.hasPermission("dcintegration.admin")) {
                                    sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(mcSubCommand.execute(cmdArgs, p.getUniqueId())));
                                }else{
                                    sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().commands.noPermission)));
                                }
                            }else sender.sendMessage(Localization.instance().commands.ingameOnly);
                            return true;
                        case BOTH:
                            if ((sender instanceof final Player p)) {
                                if (!mcSubCommand.needsOP()) {
                                    sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(mcSubCommand.execute(cmdArgs, p.getUniqueId())));
                                }else if(p.hasPermission("dcintegration.admin")) {
                                    sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(mcSubCommand.execute(cmdArgs, p.getUniqueId())));
                                }else{
                                    sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().commands.noPermission)));
                                }
                            } else {
                                sender.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(mcSubCommand.execute(cmdArgs, null)));
                            }
                            return true;
                    }
                }
            }
        }
        return true;
    }

    public static class TabCompleter implements org.bukkit.command.TabCompleter {

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            final ArrayList<String> out = new ArrayList<>();
            if (args.length == 1) {
                final ArrayList<String> cmds = new ArrayList<>();
                McCommandRegistry.getCommands().forEach((c) -> {
                    if (c.getType() == MCSubCommand.CommandType.BOTH || c.getType() == MCSubCommand.CommandType.PLAYER_ONLY)
                        if (!c.needsOP() || (c.needsOP() && sender.hasPermission("dcintegration.admin"))) {
                            cmds.add(c.getName());
                        }
                });
                /*
                cmds.add("link");
                cmds.add("ignore");
                if (sender.hasPermission("dcintegration.admin")) {
                    cmds.add("reload");
                    cmds.add("restart");
                }*/
                if (!args[0].isEmpty())
                    for (String cmd : cmds) {
                        if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                            out.add(cmd);
                        }
                    }
                else out.addAll(cmds);
            }
            return out;
        }
    }
}
