package de.erdbeerbaerlp.dcintegration.lilybukkit.commands;

import dcshadow.dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dcshadow.dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DiscordCommandSender implements CommandSender {
    private final String cmd;
    private CompletableFuture<Message> editedMessage;
    final StringBuilder tmpMessage = new StringBuilder();
    private final User sender;

    public DiscordCommandSender(String cmd, CompletableFuture<Message> cmdMsg, User sender) {
        this.cmd = cmd;
        this.editedMessage = cmdMsg;
        this.sender = sender;
    }

    public void appendMessage(String msg) {
        tmpMessage.append(DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(msg),DiscordSerializerOptions.defaults())).append("\n");
    }

    @Override
    public void sendMessage(String message) {
        appendMessage(message);
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