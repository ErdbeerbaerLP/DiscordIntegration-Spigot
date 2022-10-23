package de.erdbeerbaerlp.dcintegration.spigot.command;

import dcshadow.dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dcshadow.dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import dcshadow.org.jetbrains.annotations.NotNull;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("NullableProblems")
public class DiscordCommandSender implements ConsoleCommandSender {
    private CompletableFuture<Message> editedMessage;
    final StringBuilder tmpMessage = new StringBuilder();
    private final User sender;

    public DiscordCommandSender(CompletableFuture<Message> cmdMsg, User sender) {
        this.editedMessage = cmdMsg;
        this.sender = sender;
    }

    public void appendMessage(String msg) {
        tmpMessage.append(DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(msg), DiscordSerializerOptions.defaults())).append("\n");
    }


    @Override
    public boolean isConversing() {
        return false;
    }

    @Override
    public void acceptConversationInput(String input) {

    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return false;
    }

    @Override
    public void abandonConversation(Conversation conversation) {

    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {

    }

    @Override
    public void sendRawMessage(String message) {
        sendMessage(message);
    }

    @Override
    public void sendRawMessage(UUID sender, String message) {
        sendMessage(message);
    }


    // Spigot start
    class SpigotProxy extends Spigot {

        /**
         * Sends this sender a chat component.
         *
         * @param component the components to send
         */
        public void sendMessage(@NotNull net.md_5.bungee.api.chat.BaseComponent component) {
            appendMessage(DiscordSerializer.INSTANCE.serialize(SpigotMessageUtils.spigotToAdventure(component)));
            editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(tmpMessage.toString()).submit());
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         */
        public void sendMessage(@NotNull net.md_5.bungee.api.chat.BaseComponent... components) {
            appendMessage(DiscordSerializer.INSTANCE.serialize(SpigotMessageUtils.spigotToAdventure(components)));
            editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(tmpMessage.toString()).submit());
        }

        /**
         * Sends this sender a chat component.
         *
         * @param component the components to send
         * @param sender    the sender of the message
         */
        public void sendMessage(@Nullable UUID sender, @NotNull net.md_5.bungee.api.chat.BaseComponent component) {
            appendMessage(DiscordSerializer.INSTANCE.serialize(SpigotMessageUtils.spigotToAdventure(component)));
            editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(tmpMessage.toString()).submit());
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         * @param sender     the sender of the message
         */
        public void sendMessage(@Nullable UUID sender, @NotNull net.md_5.bungee.api.chat.BaseComponent... components) {
            appendMessage(DiscordSerializer.INSTANCE.serialize(SpigotMessageUtils.spigotToAdventure(components), DiscordSerializerOptions.defaults()));
            editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(tmpMessage.toString()).submit());
        }
    }

    @Override
    public void sendMessage(String message) {
        appendMessage(message);
        editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(tmpMessage.toString()))).submit());
    }

    @Override
    public void sendMessage(String... messages) {
        for (String tosend : messages)
            appendMessage(tosend);
        editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(tmpMessage.toString()))).submit());
    }

    @Override
    public void sendMessage(UUID sender, String message) {
        appendMessage(message);
        editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(tmpMessage.toString()))).submit());
    }

    @Override
    public void sendMessage(UUID sender, String... messages) {
        for (String tosend : messages)
            appendMessage(tosend);
        editedMessage.thenAccept((msg) -> editedMessage = msg.editMessage(DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(tmpMessage.toString()))).submit());
    }

    @Override
    public Server getServer() {
        return Bukkit.getConsoleSender().getServer();
    }

    @Override
    public String getName() {
        return sender.getAsTag();
    }

    @Override
    public Spigot spigot() {
        return new SpigotProxy();
    }

    @Override
    public boolean isPermissionSet(String name) {
        return Bukkit.getConsoleSender().isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return Bukkit.getConsoleSender().isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        return Bukkit.getConsoleSender().hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return Bukkit.getConsoleSender().hasPermission(perm);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return Bukkit.getConsoleSender().addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return Bukkit.getConsoleSender().addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return Bukkit.getConsoleSender().addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return Bukkit.getConsoleSender().addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        Bukkit.getConsoleSender().removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        Bukkit.getConsoleSender().recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return Bukkit.getConsoleSender().getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return Bukkit.getConsoleSender().isOp();
    }

    @Override
    public void setOp(boolean value) {
        Bukkit.getConsoleSender().setOp(value);
    }
}
