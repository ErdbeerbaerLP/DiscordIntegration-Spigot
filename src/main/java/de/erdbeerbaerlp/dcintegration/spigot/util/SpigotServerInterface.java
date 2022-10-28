package de.erdbeerbaerlp.dcintegration.spigot.util;

import dcshadow.net.kyori.adventure.text.Component;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.DiscordIntegration;
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
    public String getLoaderName() {
        return "Spigot";
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
    public void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote) {
        final Collection<? extends Player> l = Bukkit.getOnlinePlayers();
        for (final Player p : l) {
            if (p.getUniqueId().equals(targetUUID) && !Variables.discord_instance.ignoringPlayers.contains(p.getUniqueId()) && !PlayerLinkController.getSettings(null, p.getUniqueId()).ignoreDiscordChatIngame && !PlayerLinkController.getSettings(null, p.getUniqueId()).ignoreReactions) {
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
        cmdMsg.thenAccept((msg) -> {
            final CompletableFuture<Message> cmdMessage = msg.editOriginal(Localization.instance().commands.executing).submit();

            Bukkit.getScheduler().runTask(DiscordIntegration.INSTANCE, () -> {
                try {
                    Bukkit.dispatchCommand(new DiscordCommandSender(cmdMessage, sender), cmd);
                }catch (CommandException e){
                    cmdMessage.thenAccept((a)-> a.editMessage(e.getMessage()+(e.getCause() != null?("\n"+e.getCause().getMessage()):"")).queue());
                }
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
