package de.erdbeerbaerlp.dcintegration.lilybukkit.commands;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.event.ClickEvent;
import dcshadow.net.kyori.adventure.text.event.HoverEvent;
import dcshadow.net.kyori.adventure.text.format.Style;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.lilybukkit.util.UUIDUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;

public class McDiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand( CommandSender sender, Command command,  String label,  String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Localization.instance().commands.ingameOnly);
            return true;
        }
        final Player p = (Player) sender;
        if (args.length == 0) {
            p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Configuration.instance().ingameCommand.message).style(Style.empty().clickEvent(ClickEvent.openUrl(Configuration.instance().ingameCommand.inviteURL)).hoverEvent(HoverEvent.showText(Component.text(Configuration.instance().ingameCommand.hoverMessage))))));
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "link":
                    if (Configuration.instance().linking.enableLinking && Variables.discord_instance.srv.isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                        if (PlayerLinkController.isPlayerLinked(UUIDUtils.getUUIDFromPlayer(p))) {
                            p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Localization.instance().linking.alreadyLinked.replace("%player%", Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromBedrockPlayer(UUIDUtils.getUUIDFromPlayer(p))).getAsTag())).style(Style.style(TextColors.of(Color.RED)))));
                            break;
                        }
                        final int r = Variables.discord_instance.genLinkNumber(UUIDUtils.getUUIDFromPlayer(p));
                        p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Localization.instance().linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", "/")).style(Style.style(TextColors.of(Color.ORANGE)).clickEvent(ClickEvent.copyToClipboard("" + r)).hoverEvent(HoverEvent.showText(Component.text(Localization.instance().linking.hoverMsg_copyClipboard))))));
                    } else {
                        p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Localization.instance().commands.subcommandDisabled).style(Style.style(TextColors.of(Color.RED)))));
                    }
                    break;
                case "ignore":
                    p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Variables.discord_instance.togglePlayerIgnore(UUIDUtils.getUUIDFromPlayer(p)) ? Localization.instance().commands.commandIgnore_unignore : Localization.instance().commands.commandIgnore_ignore)));
                    break;
                case "restart":
                    if (p.hasPermission("dcintegration.admin"))
                        new Thread(() -> {
                            if (Variables.discord_instance.restart())
                                p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text("Discord Bot restarted")));
                            else
                                p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text("Failed to properly restart the discord bot!").style(Style.style(TextColors.of(Color.RED)))));
                        }).start();
                    else
                        p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Localization.instance().commands.noPermission).style(Style.style(TextColors.of(Color.RED)))));
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
                        p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Localization.instance().commands.configReloaded)));
                    } else {
                        p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Localization.instance().commands.noPermission).style(Style.style(TextColors.of(Color.RED)))));
                    }
                    break;
            }
        } else {
            p.sendMessage(LegacyComponentSerializer.legacySection().serialize(Component.text(Localization.instance().commands.tooManyArguments).style(Style.style(TextColors.of(Color.RED)))));
        }
        return true;
    }
}
