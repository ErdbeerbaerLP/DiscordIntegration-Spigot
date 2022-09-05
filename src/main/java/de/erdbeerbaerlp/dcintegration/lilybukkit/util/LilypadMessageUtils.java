package de.erdbeerbaerlp.dcintegration.lilybukkit.util;

import dcshadow.org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class LilypadMessageUtils extends MessageUtils {
    public static String formatPlayerName(Map.Entry<UUID, String> p) {
        return ChatColor.stripColor(p.getValue());
    }

    public static String formatPlayerName(Player player) {
        return formatPlayerName(new DefaultMapEntry<>(UUIDUtils.getUUIDFromPlayer(player), player.getName()));
    }

}
