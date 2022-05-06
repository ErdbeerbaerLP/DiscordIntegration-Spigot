package de.erdbeerbaerlp.dcintegration.spigot.command;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.event.ClickEvent;
import dcshadow.net.kyori.adventure.text.event.HoverEvent;
import dcshadow.net.kyori.adventure.text.format.Style;
import dcshadow.org.jetbrains.annotations.NotNull;
import dcshadow.org.jetbrains.annotations.Nullable;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.compat.FloodgateUtils;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class McDiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Localization.instance().commands.ingameOnly);
            return true;
        }
        final Player p = (Player) sender;
        if (args.length == 0) {
            p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Configuration.instance().ingameCommand.message).style(Style.empty().clickEvent(ClickEvent.openUrl(Configuration.instance().ingameCommand.inviteURL)).hoverEvent(HoverEvent.showText(Component.text(Configuration.instance().ingameCommand.hoverMessage))))));
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "link":
                    if (sender.getServer().getPluginManager().getPlugin("floodgate-bukkit") != null) {
                        if (FloodgateUtils.linkCommand(p)) break;
                    }
                    if (Configuration.instance().linking.enableLinking && Variables.discord_instance.srv.isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                        if (PlayerLinkController.isPlayerLinked(p.getUniqueId())) {
                            p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().linking.alreadyLinked.replace("%player%", Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromBedrockPlayer(p.getUniqueId())).getAsTag())).style(Style.style(TextColors.of(Color.RED)))));
                            break;
                        }
                        final int r = Variables.discord_instance.genLinkNumber(p.getUniqueId());
                        p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", "/")).style(Style.style(TextColors.of(Color.ORANGE)).clickEvent(ClickEvent.copyToClipboard("" + r)).hoverEvent(HoverEvent.showText(Component.text(Localization.instance().linking.hoverMsg_copyClipboard))))));
                    } else {
                        p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().commands.subcommandDisabled).style(Style.style(TextColors.of(Color.RED)))));
                    }
                    break;
                case "ignore":
                    p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Variables.discord_instance.togglePlayerIgnore(p.getUniqueId()) ? Localization.instance().commands.commandIgnore_unignore : Localization.instance().commands.commandIgnore_ignore)));
                    break;
                case "restart":
                    if (p.hasPermission("dcintegration.admin"))
                        new Thread(() -> {
                            if (Variables.discord_instance.restart())
                                p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text("Discord Bot restarted")));
                            else
                                p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text("Failed to properly restart the discord bot!").style(Style.style(TextColors.of(Color.RED)))));
                        }).start();
                    else
                        p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().commands.noPermission).style(Style.style(TextColors.of(Color.RED)))));
                    break;
                case "reload":
                    if (p.hasPermission("dcintegration.admin")) {
                        try {
                            Configuration.instance().loadConfig();
                        } catch (IOException e) {
                            System.err.println("Config loading failed");
                            e.printStackTrace();
                        }
                        AddonLoader.reloadAll();
                        p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().commands.configReloaded)));
                    } else {
                        p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().commands.noPermission).style(Style.style(TextColors.of(Color.RED)))));
                    }
                    break;
            }
        } else {
            p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().commands.tooManyArguments).style(Style.style(TextColors.of(Color.RED)))));
        }
        return true;
    }

    public static class TabCompleter implements org.bukkit.command.TabCompleter {

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            final ArrayList<String> out = new ArrayList<>();
            if (args.length == 1) {
                final ArrayList<String> cmds = new ArrayList<>();
                cmds.add("link");
                cmds.add("ignore");
                if (sender.hasPermission("dcintegration.admin")) {
                    cmds.add("reload");
                    cmds.add("restart");
                }
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
