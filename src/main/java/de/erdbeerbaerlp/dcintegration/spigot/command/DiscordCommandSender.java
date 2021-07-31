package de.erdbeerbaerlp.dcintegration.spigot.command;

import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;

public class DiscordCommandSender implements ConsoleCommandSender {
    private final ConsoleCommandSender wrappedSender;
    private final Spigot spigotWrapper;
    private final User user;
    private final TextChannel channel;

    private class Spigot extends CommandSender.Spigot {
        /**
         * Sends this sender a chat component.
         *
         * @param component the components to send
         */
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent component) {
            Variables.discord_instance.sendMessageFuture(BaseComponent.toLegacyText(component), channel.getId());
            wrappedSender.spigot().sendMessage();
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         */
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent... components) {
            Variables.discord_instance.sendMessageFuture(BaseComponent.toPlainText(components), channel.getId());
            wrappedSender.spigot().sendMessage(components);
        }
    }


    public DiscordCommandSender(ConsoleCommandSender wrappedSender, User user, TextChannel channel) {
        this.wrappedSender = wrappedSender;
        spigotWrapper = new Spigot();
        this.user = user;
        this.channel = channel;
    }

    @Override
    public void sendMessage(String message) {
        wrappedSender.sendMessage(message);
        Variables.discord_instance.sendMessageFuture(message, channel.getId());
    }

    @Override
    public void sendMessage(String[] messages) {
        wrappedSender.sendMessage(messages);
        for (String message : messages) {
            Variables.discord_instance.sendMessageFuture(message, channel.getId());
        }
    }

    @Override
    public void sendMessage(UUID sender, String message) {
        wrappedSender.sendMessage(sender, message);
        Variables.discord_instance.sendMessageFuture(message, channel.getId());
    }

    @Override
    public void sendMessage(UUID sender, String[] messages) {
        wrappedSender.sendMessage(sender, messages);
        for (String message : messages) {
            Variables.discord_instance.sendMessageFuture(message, channel.getId());
        }
    }

    @Override
    public void sendRawMessage(String message) {
        Variables.discord_instance.sendMessageFuture(message, channel.getId());
        wrappedSender.sendRawMessage(message);
    }

    @Override
    public void sendRawMessage(UUID sender, String message) {
        wrappedSender.sendRawMessage(sender, message);
    }

    @Override
    public Server getServer() {
        return wrappedSender.getServer();
    }

    @Override
    public String getName() {
        return user.getAsTag();
    }

    @Override
    public DiscordCommandSender.Spigot spigot() {
        return spigotWrapper;
    }

    @Override
    public boolean isConversing() {
        return wrappedSender.isConversing();
    }

    @Override
    public void acceptConversationInput(String input) {
        wrappedSender.acceptConversationInput(input);
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return wrappedSender.beginConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        wrappedSender.abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        wrappedSender.abandonConversation(conversation, details);
    }


    @Override
    public boolean isPermissionSet(String name) {
        return wrappedSender.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return wrappedSender.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        return wrappedSender.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return wrappedSender.hasPermission(perm);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return wrappedSender.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return wrappedSender.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return wrappedSender.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return wrappedSender.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        wrappedSender.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        wrappedSender.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return wrappedSender.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {
    }
}