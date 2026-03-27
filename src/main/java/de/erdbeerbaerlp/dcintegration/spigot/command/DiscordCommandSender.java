package de.erdbeerbaerlp.dcintegration.spigot.command;

import dcshadow.dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dcshadow.dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import dcshadow.org.jetbrains.annotations.NotNull;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
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
    private CompletableFuture<Message> cmdMessage;
    private final CompletableFuture<InteractionHook> cmdMsg;
    public final StringBuilder message = new StringBuilder();
    private final String name;

    public DiscordCommandSender(CompletableFuture<InteractionHook> cmdMsg, User sender) {
        this.cmdMsg = cmdMsg;
        this.name = !sender.getDiscriminator().equals("0000") ? sender.getAsTag():sender.getName();
    }
    public DiscordCommandSender() {
        this.cmdMsg = null;
        this.name = "Discord Integration";
    }

    public void appendMessage(String msg) {
        message.append(DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(msg), DiscordSerializerOptions.defaults())).append("\n");
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
            if (cmdMsg != null)
                if (cmdMessage == null)
                    cmdMsg.thenAccept((msg) -> {
                        cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                    });
                else
                    cmdMessage.thenAccept((msg) -> {
                        cmdMessage = msg.editMessage(message.toString().trim()).submit();
                    });
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         */
        public void sendMessage(@NotNull net.md_5.bungee.api.chat.BaseComponent... components) {
            appendMessage(DiscordSerializer.INSTANCE.serialize(SpigotMessageUtils.spigotToAdventure(components)));
            if (cmdMsg != null)
                if (cmdMessage == null)
                    cmdMsg.thenAccept((msg) -> {
                        cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                    });
                else
                    cmdMessage.thenAccept((msg) -> {
                        cmdMessage = msg.editMessage(message.toString().trim()).submit();
                    });
        }

        /**
         * Sends this sender a chat component.
         *
         * @param component the components to send
         * @param sender    the sender of the message
         */
        public void sendMessage(@Nullable UUID sender, @NotNull net.md_5.bungee.api.chat.BaseComponent component) {
            appendMessage(DiscordSerializer.INSTANCE.serialize(SpigotMessageUtils.spigotToAdventure(component)));
            if (cmdMsg != null)
                if (cmdMessage == null)
                    cmdMsg.thenAccept((msg) -> {
                        cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                    });
                else
                    cmdMessage.thenAccept((msg) -> {
                        cmdMessage = msg.editMessage(message.toString().trim()).submit();
                    });
        }

        /**
         * Sends an array of components as a single message to the sender.
         *
         * @param components the components to send
         * @param sender     the sender of the message
         */
        public void sendMessage(@Nullable UUID sender, @NotNull net.md_5.bungee.api.chat.BaseComponent... components) {
            appendMessage(DiscordSerializer.INSTANCE.serialize(SpigotMessageUtils.spigotToAdventure(components), DiscordSerializerOptions.defaults()));
            if (cmdMsg != null)
                if (cmdMessage == null)
                    cmdMsg.thenAccept((msg) -> {
                        cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                    });
                else
                    cmdMessage.thenAccept((msg) -> {
                        cmdMessage = msg.editMessage(message.toString().trim()).submit();
                    });
        }
    }

    @Override
    public void sendMessage(String message) {
        appendMessage(message);
        if (cmdMsg != null)
            if (cmdMessage == null)
                cmdMsg.thenAccept((msg) -> {
                    cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                });
            else
                cmdMessage.thenAccept((msg) -> {
                    cmdMessage = msg.editMessage(message.toString().trim()).submit();
                });
    }

    @Override
    public void sendMessage(String... messages) {
        for (String tosend : messages)
            appendMessage(tosend);

        if (cmdMsg != null)
            if (cmdMessage == null)
                cmdMsg.thenAccept((msg) -> {
                    cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                });
            else
                cmdMessage.thenAccept((msg) -> {
                    cmdMessage = msg.editMessage(message.toString().trim()).submit();
                });
    }

    @Override
    public void sendMessage(UUID sender, String message) {
        appendMessage(message);
        if (cmdMsg != null)
            if (cmdMessage == null)
                cmdMsg.thenAccept((msg) -> {
                    cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                });
            else
                cmdMessage.thenAccept((msg) -> {
                    cmdMessage = msg.editMessage(message.toString().trim()).submit();
                });
    }

    @Override
    public void sendMessage(UUID sender, String... messages) {
        for (String tosend : messages)
            appendMessage(tosend);
        if (cmdMsg != null)
            if (cmdMessage == null)
                cmdMsg.thenAccept((msg) -> {
                    cmdMessage = msg.editOriginal(message.toString().trim()).submit();
                });
            else
                cmdMessage.thenAccept((msg) -> {
                    cmdMessage = msg.editMessage(message.toString().trim()).submit();
                });
    }

    @Override
    public Server getServer() {
        return Bukkit.getConsoleSender().getServer();
    }

    @Override
    public String getName() {
        return name;
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

    @Override
    public boolean isConversing() {
        return Bukkit.getConsoleSender().isConversing();
    }

    @Override
    public void acceptConversationInput(@NotNull String input) {
        Bukkit.getConsoleSender().acceptConversationInput(input);
    }

    @Override
    public boolean beginConversation(@NotNull Conversation conversation) {
        return Bukkit.getConsoleSender().beginConversation(conversation);
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation) {
        Bukkit.getConsoleSender().abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) {
        Bukkit.getConsoleSender().abandonConversation(conversation, details);
    }

    @Override
    public void sendRawMessage(@NotNull String message) {
        Bukkit.getConsoleSender().sendRawMessage(message);
    }

    @Override
    public void sendRawMessage(@Nullable UUID sender, @NotNull String message) {
        Bukkit.getConsoleSender().sendRawMessage(sender, message);
    }
}
