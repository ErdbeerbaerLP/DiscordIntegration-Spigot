package de.erdbeerbaerlp.dcintegration.spigot.util;

import dcshadow.net.kyori.adventure.text.Component;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.McServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import de.erdbeerbaerlp.dcintegration.spigot.DiscordIntegrationPlugin;
import de.erdbeerbaerlp.dcintegration.spigot.command.DiscordCommandSender;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SpigotServerInterface implements McServerInterface {
    @Override
    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public String getLoaderName() {
        return "Spigot";
    }

    @Override
    public boolean playerHasPermissions(UUID player, String... permissions) {
        final Player p = Bukkit.getPlayer(player);
        if (p == null)
            return false;
        for (String permission : permissions) {

            if (p.hasPermission(permission)) return true;
        }
        return false;
    }

    @Override
    public String runMCCommand(String cmd) {
        final DiscordCommandSender s = new DiscordCommandSender();
        try {
            Bukkit.getScheduler().callSyncMethod(DiscordIntegrationPlugin.INSTANCE, () -> {
                Bukkit.dispatchCommand(s, cmd.trim());
                return null;
            }).get();
            return s.message.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public void sendIngameMessage(Component msg) {
        final Collection<? extends Player> l = Bukkit.getOnlinePlayers();
        msg = msg.replaceText(ComponentUtils.replaceLiteral("\\\n", Component.newline()));
        try {
            for (final Player p : l) {
                if (!playerHasPermissions(p.getUniqueId(), MinecraftPermission.READ_MESSAGES, MinecraftPermission.USER))
                    return;
                if (!DiscordIntegration.INSTANCE.ignoringPlayers.contains(p.getUniqueId()) && !(LinkManager.isPlayerLinked(p.getUniqueId()) && LinkManager.getLink(null, p.getUniqueId()).settings.ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUniqueId(), p.getName());
                    p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(ping.getValue()));
                    if (ping.getKey()) {
                        if (LinkManager.isPlayerLinked(p.getUniqueId()) && LinkManager.getLink(null, p.getUniqueId()).settings.pingSound)
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
    public void sendIngameReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote) {
        final Collection<? extends Player> l = Bukkit.getOnlinePlayers();
        for (final Player p : l) {
            if (!playerHasPermissions(p.getUniqueId(), MinecraftPermission.READ_MESSAGES, MinecraftPermission.USER))
                return;
            if (p.getUniqueId().equals(targetUUID) && !DiscordIntegration.INSTANCE.ignoringPlayers.contains(p.getUniqueId()) && (LinkManager.isPlayerLinked(p.getUniqueId()) && !LinkManager.getLink(null, p.getUniqueId()).settings.ignoreDiscordChatIngame && !LinkManager.getLink(null, p.getUniqueId()).settings.ignoreReactions)) {
                final String emote = ":" + reactionEmote.getName() + ":";
                String outMsg = Localization.instance().reactionMessage.replace("%name%", member.getEffectiveName()).replace("%name2%", member.getUser().getAsTag()).replace("%emote%", emote);
                if (Localization.instance().reactionMessage.contains("%msg%"))
                    retrieveMessage.submit().thenAccept((m) -> {
                        String outMsg2 = outMsg.replace("%msg%", m.getContentDisplay());
                        p.sendMessage(SpigotMessageUtils.formatEmoteMessage(m.getMentions().getCustomEmojis(), outMsg2));
                    });
                else p.sendMessage(outMsg);
            }
        }
    }

    @Override
    public void runMcCommand(String cmd, final CompletableFuture<InteractionHook> cmdMsg, User sender) {
        Bukkit.getScheduler().runTask(DiscordIntegrationPlugin.INSTANCE, () -> {
            final DiscordCommandSender s = new DiscordCommandSender(cmdMsg, sender);
            try {
                Bukkit.dispatchCommand(s, cmd);
            } catch (CommandException e) {
                s.sendMessage(e.getMessage());
            }
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
    public void sendIngameMessage(String msg, UUID uuid) {
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
