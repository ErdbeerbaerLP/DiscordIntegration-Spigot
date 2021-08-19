package de.erdbeerbaerlp.dcintegration.spigot.compat;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.format.Style;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;


public class FloodgateUtils {

    public static boolean linkCommand(Player p) {
        if (isBedrockPlayer(p)) {
            if (Configuration.instance().linking.enableLinking && Variables.discord_instance.srv.isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                if (PlayerLinkController.isBedrockPlayerLinked(p.getUniqueId())) {
                    p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Configuration.instance().localization.linking.alreadyLinked.replace("%player%", Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromBedrockPlayer(p.getUniqueId())).getAsTag())).style(Style.style(TextColors.of(Color.RED)))));
                    return true;
                }
                final int r = Variables.discord_instance.genBedrockLinkNumber(p.getUniqueId());
                p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Configuration.instance().localization.linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", "/")).style(Style.style(TextColors.of(Color.ORANGE)))));
                return true;
            }
        }
        return false;
    }
    public static boolean isBedrockPlayer(Player p ){
        try {
            final Class<?> aClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            final Object getInstance = aClass.getDeclaredMethod("getInstance").invoke(null);
            final Method isBedrockPlayer = getInstance.getClass().getDeclaredMethod("isFloodgateId", UUID.class);
            final Object invoke = isBedrockPlayer.invoke(getInstance, p.getUniqueId());
            return (boolean) invoke;
        }catch (RuntimeException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }
}
