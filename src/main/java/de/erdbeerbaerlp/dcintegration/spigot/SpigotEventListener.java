package de.erdbeerbaerlp.dcintegration.spigot;

import dcshadow.org.apache.commons.lang3.ArrayUtils;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.api.SpigotDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.spigot.compat.FloodgateUtils;
import de.erdbeerbaerlp.dcintegration.spigot.util.AdvancementUtil;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class SpigotEventListener implements Listener {
    public static final ArrayList<UUID> timeouts = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent ev) {
        if (Configuration.instance().linking.whitelistMode && discord_instance.srv.isOnlineMode()) {
            try {
                if (!PlayerLinkController.isPlayerLinked(ev.getPlayer().getUniqueId())) {
                    ev.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Localization.instance().linking.notWhitelistedCode.replace("%code%", "" + (FloodgateUtils.isBedrockPlayer(ev.getPlayer()) ? Variables.discord_instance.genBedrockLinkNumber(ev.getPlayer().getUniqueId()) : Variables.discord_instance.genLinkNumber(ev.getPlayer().getUniqueId()))));
                }else if(!Variables.discord_instance.canPlayerJoin(ev.getPlayer().getUniqueId())){
                    ev.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Localization.instance().linking.notWhitelistedRole);
                }
            } catch (IllegalStateException e) {
                ev.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Please check " + Variables.discordDataDir + "LinkedPlayers.json\n\n" + e.toString());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
            discord_instance.sendMessage(Localization.instance().playerJoin.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));

            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            final Thread fixLinkStatus = new Thread(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = ev.getPlayer().getUniqueId();
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent ev) throws NoSuchFieldException {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
            final AdvancementUtil.Advancement advancement = AdvancementUtil.getAdvancement(ev);
            if (advancement != null)
                discord_instance.sendMessage(Localization.instance().advancementMessage.replace("%player%",
                                MessageUtils.removeFormatting(SpigotMessageUtils.formatPlayerName(ev.getPlayer())))
                        .replace("%name%",
                                MessageUtils.removeFormatting(advancement.name))
                        .replace("%desc%",
                                MessageUtils.removeFormatting(advancement.description))
                        .replace("\\n", "\n"));

        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
        if (discord_instance != null && !timeouts.contains(ev.getPlayer().getUniqueId()))
            discord_instance.sendMessage(Localization.instance().playerLeave.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));
        else if (discord_instance != null && timeouts.contains(ev.getPlayer().getUniqueId())) {
            discord_instance.sendMessage(Localization.instance().playerTimeout.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));
            timeouts.remove(ev.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
        String command = ev.getMessage().replaceFirst(Pattern.quote("/"), "");
        if (!Configuration.instance().commandLog.channelID.equals("0")) {
            if (!ArrayUtils.contains(Configuration.instance().commandLog.ignoredCommands, command.split(" ")[0]))
                discord_instance.sendMessage(Configuration.instance().commandLog.message
                        .replace("%sender%", ev.getPlayer().getName())
                        .replace("%cmd%", command)
                        .replace("%cmd-no-args%", command.split(" ")[0]), discord_instance.getChannel(Configuration.instance().commandLog.channelID));
        }
        if (discord_instance != null) {
            boolean raw = false;
            if (((command.startsWith("say")) && Configuration.instance().messages.sendOnSayCommand) || ((command.startsWith("me")) && Configuration.instance().messages.sendOnMeCommand)) {
                String msg = command;
                if (command.startsWith("say"))
                    msg = msg.replaceFirst("say", "");
                if (command.startsWith("me")) {
                    raw = true;
                    msg = "*" + MessageUtils.escapeMarkdown(msg.replace("me", "").trim()) + "*";
                }
                if (!msg.trim().isEmpty())
                    discord_instance.sendMessage(ev.getPlayer().getName(), ev.getPlayer().getUniqueId().toString(), new DiscordMessage(null, msg.trim(), !raw), discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(PlayerDeathEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getEntity().getUniqueId()).hideFromDiscord) return;
            final String deathMessage = ev.getDeathMessage();
            discord_instance.sendMessage(new DiscordMessage(Localization.instance().playerDeath.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getEntity())).replace("%msg%", MessageUtils.removeFormatting(deathMessage).replace(ev.getEntity().getName() + " ", ""))), discord_instance.getChannel(Configuration.instance().advanced.deathsChannelID));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
        if (discord_instance != null) {
            if (discord_instance.callEvent((e) -> {
                if (e instanceof SpigotDiscordEventHandler)
                    return ((SpigotDiscordEventHandler) e).onMcChatMessage(ev);
                return false;
            })) return;

            String text = MessageUtils.escapeMarkdown(ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
            final TextChannel channel = discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            discord_instance.sendMessage(SpigotMessageUtils.formatPlayerName(ev.getPlayer()), ev.getPlayer().getUniqueId().toString(), new DiscordMessage(null, text, true), channel);

            //Set chat message to a more readable format
            if (channel != null) ev.setMessage(MessageUtils.mentionsToNames(ev.getMessage(), channel.getGuild()));
        }
    }
}
