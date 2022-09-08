package de.erdbeerbaerlp.dcintegration.lilybukkit.util;

import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UUIDUtils {

    public static UUID getUUIDFromName(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
    public static UUID getUUIDFromPlayer(Player p) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + p.getName()).getBytes(StandardCharsets.UTF_8));
    }

}
