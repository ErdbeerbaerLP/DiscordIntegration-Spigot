package de.erdbeerbaerlp.dcintegration.spigot;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import dcshadow.org.apache.commons.lang3.ArrayUtils;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.WorkThread;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.MCSubCommand;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.spigot.api.SpigotDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.spigot.util.AdvancementUtil;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.advancement.AdvancementDisplay;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

public class SpigotEventListener implements Listener {
    public static final ArrayList<UUID> timeouts = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent ev) {
        final Player profile = ev.getPlayer();
        if (DiscordIntegration.INSTANCE == null) return;
        LinkManager.checkGlobalAPI(profile.getUniqueId());
        if(DiscordIntegration.INSTANCE.getServerInterface().playerHasPermissions(profile.getUniqueId(), MinecraftPermission.BYPASS_WHITELIST,MinecraftPermission.ADMIN)){
            return;
        }
        if (Configuration.instance().linking.whitelistMode && DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) {
            try {
                if (!LinkManager.isPlayerLinked(profile.getUniqueId())) {
                    ev.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Localization.instance().linking.notWhitelistedCode.replace("%code%", "" + LinkManager.genLinkNumber(profile.getUniqueId())));
                } else if (!DiscordIntegration.INSTANCE.canPlayerJoin(profile.getUniqueId())) {
                    ev.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Localization.instance().linking.notWhitelistedRole);
                }
            } catch (IllegalStateException e) {
                ev.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "An error occured\nPlease check Server Log for more information\n\n" + e);
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent ev) {
        final Player p = ev.getPlayer();
        if (DiscordIntegration.INSTANCE != null) {
            if (LinkManager.isPlayerLinked(p.getUniqueId()) && LinkManager.getLink(null, p.getUniqueId()).settings.hideFromDiscord)
                return;
            LinkManager.checkGlobalAPI(p.getUniqueId());
            if (!Localization.instance().playerJoin.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerJoinMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", p.getUniqueId().toString()).replace("%uuid_dashless%", p.getUniqueId().toString().replace("-", "")).replace("%name%", p.getName()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.playerJoinMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbedJson(Configuration.instance().embedMode.playerJoinMessage.customJSON
                                .replace("%uuid%", p.getUniqueId().toString())
                                .replace("%uuid_dashless%", p.getUniqueId().toString().replace("-", ""))
                                .replace("%name%", SpigotMessageUtils.formatPlayerName(p))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(p.getUniqueId()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbed();
                        b.setAuthor(SpigotMessageUtils.formatPlayerName(p), null, avatarURL)
                                .setDescription(Localization.instance().playerJoin.replace("%player%", SpigotMessageUtils.formatPlayerName(p)));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerJoin.replace("%player%", SpigotMessageUtils.formatPlayerName(p)));
            }
            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            WorkThread.executeJob(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = p.getUniqueId();
                if (!LinkManager.isPlayerLinked(uuid)) return;
                final Guild guild = DiscordIntegration.INSTANCE.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                if (LinkManager.isPlayerLinked(uuid)) {
                    final Member member = DiscordIntegration.INSTANCE.getMemberById(LinkManager.getLink(null, uuid).discordID);
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent ev) {
        if (DiscordIntegration.INSTANCE == null) return;
        if (ev.getAdvancement().getDisplay() == null) return;
        final Player owner = ev.getPlayer();
        AdvancementUtil.Advancement advancement;
        try {
            final AdvancementDisplay display = ev.getAdvancement().getDisplay();
            advancement = new AdvancementUtil.Advancement(display.getTitle(), display.getDescription());
        } catch (NoSuchMethodError e) {
            advancement = AdvancementUtil.getAdvancement(ev);
        }
        if (advancement == null) return;
        if (LinkManager.isPlayerLinked(owner.getUniqueId()) && LinkManager.getLink(null, owner.getUniqueId()).settings.hideFromDiscord)
            return;
        if (!Localization.instance().advancementMessage.isBlank()) {
            if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.advancementMessage.asEmbed) {
                final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", owner.getUniqueId().toString()).replace("%uuid_dashless%", owner.getUniqueId().toString().replace("-", "")).replace("%name%", owner.getName()).replace("%randomUUID%", UUID.randomUUID().toString());
                if (!Configuration.instance().embedMode.advancementMessage.customJSON.isBlank()) {
                    final EmbedBuilder b = Configuration.instance().embedMode.advancementMessage.toEmbedJson(Configuration.instance().embedMode.advancementMessage.customJSON
                            .replace("%uuid%", owner.getUniqueId().toString())
                            .replace("%uuid_dashless%", owner.getUniqueId().toString().replace("-", ""))
                            .replace("%name%", SpigotMessageUtils.formatPlayerName(owner))
                            .replace("%randomUUID%", UUID.randomUUID().toString())
                            .replace("%avatarURL%", avatarURL)
                            .replace("%advName%", ChatColor.stripColor(advancement.getTitle()))
                            .replace("%advDesc%", ChatColor.stripColor(advancement.description()))
                            .replace("%advNameURL%", URLEncoder.encode(ChatColor.stripColor(ev.getAdvancement().getDisplay().getTitle()), StandardCharsets.UTF_8))
                            .replace("%advDescURL%", URLEncoder.encode(ChatColor.stripColor(ev.getAdvancement().getDisplay().getDescription()), StandardCharsets.UTF_8))
                            .replace("%avatarURL%", avatarURL)
                            .replace("%playerColor%", "" + TextColors.generateFromUUID(owner.getUniqueId()).getRGB())
                    );
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                } else {
                    EmbedBuilder b = Configuration.instance().embedMode.advancementMessage.toEmbed();
                    b = b.setAuthor(SpigotMessageUtils.formatPlayerName(owner), null, avatarURL)
                            .setDescription(Localization.instance().advancementMessage.replace("%player%",
                                            ChatColor.stripColor(SpigotMessageUtils.formatPlayerName(owner))).replace("%advName%",
                                            ChatColor.stripColor(advancement
                                                    .getTitle()))
                                    .replace("%advDesc%",
                                            ChatColor.stripColor(advancement
                                                    .description()))
                                    .replace("\\n", "\n").replace("%advNameURL%", URLEncoder.encode(ChatColor.stripColor(ev.getAdvancement().getDisplay().getTitle()), StandardCharsets.UTF_8))
                                    .replace("%advDescURL%", URLEncoder.encode(ChatColor.stripColor(ev.getAdvancement().getDisplay().getDescription()), StandardCharsets.UTF_8))
                            );
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                }
            } else
                DiscordIntegration.INSTANCE.sendMessage(Localization.instance().advancementMessage.replace("%player%",
                                ChatColor.stripColor(SpigotMessageUtils.formatPlayerName(owner)))
                        .replace("%advName%",
                                ChatColor.stripColor(advancement
                                        .getTitle()))
                        .replace("%advDesc%",
                                ChatColor.stripColor(advancement
                                        .description()))
                        .replace("\\n", "\n").replace("%advNameURL%", URLEncoder.encode(ChatColor.stripColor(ev.getAdvancement().getDisplay().getTitle()), StandardCharsets.UTF_8))
                        .replace("%advDescURL%", URLEncoder.encode(ChatColor.stripColor(ev.getAdvancement().getDisplay().getDescription()), StandardCharsets.UTF_8))
                );
        }


    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent ev) {
        final Player player = ev.getPlayer();
        if (LinkManager.isPlayerLinked(player.getUniqueId()) && LinkManager.getLink(null, player.getUniqueId()).settings.hideFromDiscord)
            return;
        final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", player.getUniqueId().toString()).replace("%uuid_dashless%", player.getUniqueId().toString().replace("-", "")).replace("%name%", player.getName()).replace("%randomUUID%", UUID.randomUUID().toString());
        if (DiscordIntegration.INSTANCE != null) {
            if (!Localization.instance().playerLeave.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.playerLeaveMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbedJson(Configuration.instance().embedMode.playerLeaveMessages.customJSON
                                .replace("%uuid%", player.getUniqueId().toString())
                                .replace("%uuid_dashless%", player.getUniqueId().toString().replace("-", ""))
                                .replace("%name%", SpigotMessageUtils.formatPlayerName(player))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUniqueId()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed();
                        b = b.setAuthor(SpigotMessageUtils.formatPlayerName(player), null, avatarURL)
                                .setDescription(Localization.instance().playerLeave.replace("%player%", SpigotMessageUtils.formatPlayerName(player)));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerLeave.replace("%player%", SpigotMessageUtils.formatPlayerName(player)));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent ev) {
        String command = ev.getMessage();
        final Player source = ev.getPlayer();
        final boolean isServer = source instanceof ConsoleCommandSender;
        command = command.replaceFirst(Pattern.quote("/"), "");
        if (!Configuration.instance().commandLog.channelID.equals("0")) {
            if (!ArrayUtils.contains(Configuration.instance().commandLog.ignoredCommands, command.split(" ")[0])) {
                DiscordIntegration.INSTANCE.sendMessage(Configuration.instance().commandLog.message
                        .replace("%sender%", source.getName())
                        .replace("%cmd%", command)
                        .replace("%cmd-no-args%", command.split(" ")[0]), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().commandLog.channelID));
            }
        }
        if (DiscordIntegration.INSTANCE != null) {
            boolean raw = false;

            if (((command.startsWith("say")) && Configuration.instance().messages.sendOnSayCommand) || (command.startsWith("me") && Configuration.instance().messages.sendOnMeCommand)) {
                String msg = command.replace("say ", "");
                if (command.startsWith("say")) {
                    msg = msg.replaceFirst("say ", "");
                }
                if (command.startsWith("me")) {
                    raw = true;
                    msg = "*" + MessageUtils.escapeMarkdown(msg.replaceFirst("me ", "").trim()) + "*";
                }
                DiscordIntegration.INSTANCE.sendMessage(source.getName(), isServer ? source.getUniqueId().toString() : "0000000", new DiscordMessage(null, msg, !raw), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }

            if (command.startsWith("discord ") || command.startsWith("dc ")) {
                final String[] args = command.replace("discord ", "").replace("dc ", "").split(" ");
                for (MCSubCommand mcSubCommand : McCommandRegistry.getCommands()) {
                    if (args[0].equals(mcSubCommand.getName())) {
                        final String[] cmdArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                        switch (mcSubCommand.getType()) {
                            case CONSOLE_ONLY:
                                if (!isServer) {
                                    source.spigot().sendMessage(TextComponent.fromLegacyText(Localization.instance().commands.consoleOnly));
                                } else {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));
                                    source.spigot().sendMessage(ComponentSerializer.parse(txt));
                                }
                                break;
                            case PLAYER_ONLY:
                                if (!isServer) {
                                    if (!mcSubCommand.needsOP() && source.hasPermission(MinecraftPermission.RUN_DISCORD_COMMAND.getAsString())) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, source.getUniqueId()));
                                        source.spigot().sendMessage(ComponentSerializer.parse(txt));
                                    } else if (source.hasPermission(MinecraftPermission.ADMIN.getAsString()) || source.hasPermission(MinecraftPermission.RUN_DISCORD_COMMAND_ADMIN.getAsString())) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, source.getUniqueId()));
                                        source.spigot().sendMessage(ComponentSerializer.parse(txt));
                                    } else {
                                        source.spigot().sendMessage(TextComponent.fromLegacyText((Localization.instance().commands.noPermission)));
                                    }
                                } else {
                                    source.spigot().sendMessage(TextComponent.fromLegacyText(Localization.instance().commands.ingameOnly));
                                }
                                break;
                            case BOTH:
                                if (!isServer) {
                                    if (!mcSubCommand.needsOP() && source.hasPermission(MinecraftPermission.RUN_DISCORD_COMMAND.getAsString())) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, source.getUniqueId()));
                                        source.spigot().sendMessage(ComponentSerializer.parse(txt));
                                    } else if (source.hasPermission(MinecraftPermission.ADMIN.getAsString()) || source.hasPermission(MinecraftPermission.RUN_DISCORD_COMMAND_ADMIN.getAsString())) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, source.getUniqueId()));
                                        source.spigot().sendMessage(ComponentSerializer.parse(txt));
                                    } else {
                                        source.spigot().sendMessage(TextComponent.fromLegacyText(Localization.instance().commands.noPermission));
                                    }
                                } else {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));
                                    source.spigot().sendMessage(ComponentSerializer.parse(txt));
                                }
                                break;
                        }
                    }
                }
                ev.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(PlayerDeathEvent ev) {
        final Player p = ev.getEntity();
        if (DiscordIntegration.INSTANCE != null) {
            if (LinkManager.isPlayerLinked(p.getUniqueId()) && LinkManager.getLink(null, p.getUniqueId()).settings.hideFromDiscord)
                return;
            final String deathMessage = ev.getDeathMessage();
            if (!Localization.instance().playerDeath.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.deathMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", p.getUniqueId().toString()).replace("%uuid_dashless%", p.getUniqueId().toString().replace("-", "")).replace("%name%", p.getName()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.playerJoinMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbedJson(Configuration.instance().embedMode.playerJoinMessage.customJSON
                                .replace("%uuid%", p.getUniqueId().toString())
                                .replace("%uuid_dashless%", p.getUniqueId().toString().replace("-", ""))
                                .replace("%name%", SpigotMessageUtils.formatPlayerName(p))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%deathMessage%", ChatColor.stripColor(deathMessage).replace(SpigotMessageUtils.formatPlayerName(p) + " ", ""))
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(p.getUniqueId()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.deathMessage.toEmbed();
                        b.setDescription(":skull: " + Localization.instance().playerDeath.replace("%player%", SpigotMessageUtils.formatPlayerName(p)).replace("%msg%", ChatColor.stripColor(deathMessage).replace(SpigotMessageUtils.formatPlayerName(p) + " ", "")));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Localization.instance().playerDeath.replace("%player%", SpigotMessageUtils.formatPlayerName(p)).replace("%msg%", ChatColor.stripColor(deathMessage).replace(SpigotMessageUtils.formatPlayerName(p) + " ", ""))), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent ev) {
        String message = ev.getMessage();
        final Player player = ev.getPlayer();
        if (DiscordIntegration.INSTANCE == null) return;
        if (LinkManager.isPlayerLinked(player.getUniqueId()) && LinkManager.getLink(null, player.getUniqueId()).settings.hideFromDiscord) {
            return;
        }
        if (!DiscordIntegration.INSTANCE.getServerInterface().playerHasPermissions(ev.getPlayer().getUniqueId(), MinecraftPermission.SEMD_MESSAGES, MinecraftPermission.USER))
            return;
        if (DiscordIntegration.INSTANCE.callEvent((e) -> {
            if (e instanceof SpigotDiscordEventHandler) {
                return ((SpigotDiscordEventHandler) e).onMcChatMessage(ev);
            }
            return false;
        })) {
            return;
        }
        final String text = MessageUtils.escapeMarkdown(message);
        if (DiscordIntegration.INSTANCE != null) {
            final GuildMessageChannel channel = DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            if (channel == null) {
                return;
            }
            if (!Localization.instance().discordChatMessage.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.chatMessages.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", player.getUniqueId().toString()).replace("%uuid_dashless%", player.getUniqueId().toString().replace("-", "")).replace("%name%", player.getName()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.chatMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.chatMessages.toEmbedJson(Configuration.instance().embedMode.chatMessages.customJSON
                                .replace("%uuid%", player.getUniqueId().toString())
                                .replace("%uuid_dashless%", player.getUniqueId().toString().replace("-", ""))
                                .replace("%name%", SpigotMessageUtils.formatPlayerName(player))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%msg%", text)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUniqueId()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        EmbedBuilder b = Configuration.instance().embedMode.chatMessages.toEmbed();
                        if (Configuration.instance().embedMode.chatMessages.generateUniqueColors)
                            b = b.setColor(TextColors.generateFromUUID(player.getUniqueId()));
                        b = b.setAuthor(SpigotMessageUtils.formatPlayerName(player), null, avatarURL)
                                .setDescription(text);
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(SpigotMessageUtils.formatPlayerName(player), player.getUniqueId().toString(), new DiscordMessage(null, text, true), channel);
            final Component comp = LegacyComponentSerializer.legacySection().deserialize(message);
            message = LegacyComponentSerializer.legacySection().serialize(MessageUtils.mentionsToNames(comp, channel.getGuild()));
        }
        if (!Configuration.instance().compatibility.disableParsingMentionsIngame)
            ev.setMessage(message);
    }
}
