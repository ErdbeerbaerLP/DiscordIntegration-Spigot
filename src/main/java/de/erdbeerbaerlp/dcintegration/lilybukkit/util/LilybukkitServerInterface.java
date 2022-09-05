package de.erdbeerbaerlp.dcintegration.lilybukkit.util;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.lilybukkit.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.lilybukkit.commands.DiscordCommandSender;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LilybukkitServerInterface implements ServerInterface {
    @Override
    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().length;
    }

    @Override
    public void sendMCMessage(Component msg) {
        final Player[] l = Bukkit.getOnlinePlayers();
        msg = msg.replaceText(ComponentUtils.replaceLiteral("\\\n", Component.newline()));
        try {
            for (final Player p : l) {
                if (!Variables.discord_instance.ignoringPlayers.contains(UUIDUtils.getUUIDFromPlayer(p)) && !(PlayerLinkController.isPlayerLinked(UUIDUtils.getUUIDFromPlayer(p)) && PlayerLinkController.getSettings(null, UUIDUtils.getUUIDFromPlayer(p)).ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, UUIDUtils.getUUIDFromPlayer(p), p.getName());
                    p.sendMessage(LegacyComponentSerializer.legacySection().serialize(ping.getValue()));
                    if (ping.getKey()) {
                        if (PlayerLinkController.getSettings(null, UUIDUtils.getUUIDFromPlayer(p)).pingSound)
                            p.playEffect(p.getLocation(), Effect.CLICK2, 1);
                    }
                }
            }
            //Send to server console too
            Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().serialize(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote) {
        final Player[] l = Bukkit.getOnlinePlayers();
        for (final Player p : l) {
            if (UUIDUtils.getUUIDFromPlayer(p).equals(targetUUID) && !Variables.discord_instance.ignoringPlayers.contains(UUIDUtils.getUUIDFromPlayer(p)) && !PlayerLinkController.getSettings(null, UUIDUtils.getUUIDFromPlayer(p)).ignoreDiscordChatIngame && !PlayerLinkController.getSettings(null, UUIDUtils.getUUIDFromPlayer(p)).ignoreReactions) {
                final String emote = ":" + reactionEmote.getName() + ":";
                String outMsg = Localization.instance().reactionMessage.replace("%name%", member.getEffectiveName()).replace("%name2%", member.getUser().getAsTag()).replace("%emote%", emote);
                if (Localization.instance().reactionMessage.contains("%msg%"))
                    retrieveMessage.submit().thenAccept((m) -> {
                        String outMsg2 = outMsg.replace("%msg%", m.getContentDisplay());
                        p.sendMessage(LilypadMessageUtils.formatEmoteMessage(m.getMentions().getCustomEmojis(), outMsg2));
                    });
                else p.sendMessage(outMsg);
            }
        }
    }

    @Override
    public void runMcCommand(String cmd, final CompletableFuture<InteractionHook> cmdMsg, User sender) {
        cmdMsg.thenAccept((msg) -> {
            final CompletableFuture<Message> cmdMessage = msg.editOriginal(Localization.instance().commands.executing).submit();

            Bukkit.getScheduler().callSyncMethod(DiscordIntegration.INSTANCE, () -> {
                return Bukkit.dispatchCommand(new DiscordCommandSender(cmd, cmdMessage,sender), cmd);
            });
        });
    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            players.put(UUIDUtils.getUUIDFromPlayer(p), p.getName());
        }
        return players;
    }


    public Player getPlayerFromUUID(UUID uuid){
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if(UUIDUtils.getUUIDFromPlayer(pl).equals(uuid)) return pl;
        }
        return null;
    }

    @Override
    public void sendMCMessage(String msg, UUID uuid) {
        if (uuid == null) return;
        final Player player = getPlayerFromUUID(uuid);
        if (player == null) return;
        player.sendMessage(msg);
    }

    @Override
    public String getNameFromUUID(UUID uuid) {
        return getPlayerFromUUID(uuid).getName();
    }

    @Override
    public boolean isOnlineMode() {
        return true;
    }
}
