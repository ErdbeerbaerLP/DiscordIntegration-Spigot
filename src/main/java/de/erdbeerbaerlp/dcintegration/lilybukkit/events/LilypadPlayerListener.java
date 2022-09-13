package de.erdbeerbaerlp.dcintegration.lilybukkit.events;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.lilybukkit.api.LilybukkitEventHandler;
import de.erdbeerbaerlp.dcintegration.lilybukkit.util.LilypadMessageUtils;
import de.erdbeerbaerlp.dcintegration.lilybukkit.util.UUIDUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class LilypadPlayerListener extends PlayerListener {
    @Override
    public void onPlayerChat(PlayerChatEvent ev) {
        if(ev.isCancelled()) return;
        if (PlayerLinkController.getSettings(null, UUIDUtils.getUUIDFromPlayer(ev.getPlayer())).hideFromDiscord) return;
        if (discord_instance != null) {
            if (discord_instance.callEvent((e) -> {
                if (e instanceof LilybukkitEventHandler)
                    return ((LilybukkitEventHandler) e).onMcChatMessage(ev);
                return false;
            })) return;

            String text = MessageUtils.escapeMarkdown(ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
            final TextChannel channel = discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            discord_instance.sendMessage(LilypadMessageUtils.formatPlayerName(ev.getPlayer()), UUIDUtils.getUUIDFromPlayer(ev.getPlayer()).toString(), new DiscordMessage(null, text, true), channel);

            //Set chat message to a more readable format
            if (channel != null) ev.setMessage(MessageUtils.mentionsToNames(ev.getMessage(), channel.getGuild()));
        }
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, UUIDUtils.getUUIDFromPlayer(ev.getPlayer())).hideFromDiscord) return;
            discord_instance.sendMessage(Localization.instance().playerJoin.replace("%player%", LilypadMessageUtils.formatPlayerName(ev.getPlayer())));

            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            final Thread fixLinkStatus = new Thread(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = UUIDUtils.getUUIDFromPlayer(ev.getPlayer());
                if (!PlayerLinkController.isPlayerLinked(uuid)) return;
                final Guild guild = discord_instance.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                final Member member = guild.getMemberById(PlayerLinkController.getDiscordFromPlayer(uuid));
                if (PlayerLinkController.isPlayerLinked(uuid)) {
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
            fixLinkStatus.setDaemon(true);
            fixLinkStatus.start();
        }
    }

    @Override
    public void onPlayerPreLogin(PlayerPreLoginEvent ev) {
        if (Configuration.instance().linking.whitelistMode && discord_instance.srv.isOnlineMode()) {
            try {
                if (!PlayerLinkController.isPlayerLinked(UUIDUtils.getUUIDFromName(ev.getName()))) {
                    ev.disallow(PlayerPreLoginEvent.Result.KICK_WHITELIST, Localization.instance().linking.notWhitelistedCode.replace("%code%", "" + (Variables.discord_instance.genLinkNumber(UUIDUtils.getUUIDFromName(ev.getName())))));
                }else if(!Variables.discord_instance.canPlayerJoin(UUIDUtils.getUUIDFromName(ev.getName()))){
                    ev.disallow(PlayerPreLoginEvent.Result.KICK_WHITELIST, Localization.instance().linking.notWhitelistedRole);
                }
            } catch (IllegalStateException e) {
                ev.disallow(PlayerPreLoginEvent.Result.KICK_OTHER, "Please check " + Variables.discordDataDir + "LinkedPlayers.json\n\n" + e.toString());
            }
        }
    }

    @Override
    public void onPlayerLogin(PlayerLoginEvent event) {
        super.onPlayerLogin(event);
    }

    public static final ArrayList<UUID> timeouts = new ArrayList<>();
    @Override
    public void onPlayerQuit(PlayerQuitEvent ev) {
        final UUID uuid = UUIDUtils.getUUIDFromPlayer(ev.getPlayer());
        if (PlayerLinkController.getSettings(null, uuid).hideFromDiscord) return;
        if (discord_instance != null && !timeouts.contains(uuid))
            discord_instance.sendMessage(Localization.instance().playerLeave.replace("%player%", LilypadMessageUtils.formatPlayerName(ev.getPlayer())));
        else if (discord_instance != null && timeouts.contains(uuid)) {
            discord_instance.sendMessage(Localization.instance().playerTimeout.replace("%player%", LilypadMessageUtils.formatPlayerName(ev.getPlayer())));
            timeouts.remove(uuid);
        }
    }

}
