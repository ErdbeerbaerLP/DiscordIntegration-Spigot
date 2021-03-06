package de.erdbeerbaerlp.dcintegration.spigot.util;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.org.jetbrains.annotations.NotNull;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.spigot.command.DiscordCommandSender;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import javax.lang.model.element.VariableElement;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SpigotServerInterface implements ServerInterface {
    @Override
    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public void sendMCMessage(Component msg) {
        final Collection<? extends Player> l = Bukkit.getOnlinePlayers();
        msg = msg.replaceText(ComponentUtils.replaceLiteral("\\\n", Component.newline()));
        try {
            for (final Player p : l) {
                if (!Variables.discord_instance.ignoringPlayers.contains(p.getUniqueId()) && !(PlayerLinkController.isPlayerLinked(p.getUniqueId()) && PlayerLinkController.getSettings(null, p.getUniqueId()).ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUniqueId(), p.getName());
                    p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(ping.getValue()));
                    if (ping.getKey()) {
                        if (PlayerLinkController.getSettings(null, p.getUniqueId()).pingSound)
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                    }
                }
            }
            //Send to server console too
            Bukkit.getConsoleSender().spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, MessageReaction.ReactionEmote reactionEmote) {
        final Collection<? extends Player> l = Bukkit.getOnlinePlayers();
        for (final Player p : l) {
            if (p.getUniqueId().equals(targetUUID) && !Variables.discord_instance.ignoringPlayers.contains(p.getUniqueId()) && !PlayerLinkController.getSettings(null, p.getUniqueId()).ignoreDiscordChatIngame && !PlayerLinkController.getSettings(null, p.getUniqueId()).ignoreReactions) {
                final String emote = reactionEmote.isEmote() ? ":" + reactionEmote.getEmote().getName() + ":" : MessageUtils.formatEmoteMessage(new ArrayList<>(), reactionEmote.getEmoji());
                String outMsg = Localization.instance().reactionMessage.replace("%name%", member.getEffectiveName()).replace("%name2%", member.getUser().getAsTag()).replace("%emote%", emote);
                if (Localization.instance().reactionMessage.contains("%msg%"))
                    retrieveMessage.submit().thenAccept((m) -> {
                        String outMsg2 = outMsg.replace("%msg%", m.getContentDisplay());
                        p.sendMessage(SpigotMessageUtils.formatEmoteMessage(m.getEmotes(), outMsg2));
                    });
                else p.sendMessage(outMsg);
            }
        }
    }

    @Override
    public void runMcCommand(String cmd, final CompletableFuture<InteractionHook> cmdMsg, User sender) {
        cmdMsg.thenAccept((msg) -> {
            final CompletableFuture<Message> cmdMessage = msg.editOriginal(Localization.instance().commands.executing).submit();

            Bukkit.getScheduler().runTask(DiscordIntegration.INSTANCE, () -> {
                Bukkit.dispatchCommand(new DiscordCommandSender(cmd, cmdMessage,sender), cmd);
            });
        });
    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            players.put(p.getUniqueId(), p.getName());
        }
        return players;
    }

    @Override
    public void sendMCMessage(String msg, UUID uuid) {
        if (uuid == null) return;
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        player.sendMessage(msg);
    }

    @Override
    public String getNameFromUUID(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    public boolean isOnlineMode() {
        return Configuration.instance().bungee.isBehindBungee || Bukkit.getOnlineMode();
    }
}
