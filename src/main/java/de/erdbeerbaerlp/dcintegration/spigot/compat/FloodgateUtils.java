package de.erdbeerbaerlp.dcintegration.spigot.compat;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.format.Style;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import org.bukkit.entity.Player;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;


public class FloodgateUtils {

    public static boolean linkCommand(Player p) {
        if (isBedrockPlayer(p)) {
            if (Configuration.instance().linking.enableLinking && DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                if (LinkManager.isBedrockPlayerLinked(p.getUniqueId())) {
                    p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().linking.alreadyLinked.replace("%player%", DiscordIntegration.INSTANCE.getJDA().getUserById(LinkManager.getLink(null, p.getUniqueId()).discordID).getAsTag())).style(Style.style(TextColors.of(Color.RED)))));
                    return true;
                }
                final int r = LinkManager.genBedrockLinkNumber(p.getUniqueId());
                p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Localization.instance().linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", "/")).style(Style.style(TextColors.of(Color.ORANGE)))));
                return true;
            }
        }
        return false;
    }
    public static boolean isBedrockPlayer(Player p){
        try {
            final Class<?> aClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            final Object getInstance = aClass.getDeclaredMethod("getInstance").invoke(null);
            final Method isBedrockPlayer = getInstance.getClass().getDeclaredMethod("isFloodgateId", UUID.class);
            final Object invoke = isBedrockPlayer.invoke(getInstance, p.getUniqueId());
            return (boolean) invoke;
        }catch (RuntimeException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            return false;
        }
    }
}
