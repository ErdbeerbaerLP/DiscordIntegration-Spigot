package de.erdbeerbaerlp.dcintegration.lilybukkit.util;

import org.apache.commons.io.Charsets;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UUIDUtils {

    public static UUID getUUIDFromName(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
    }
    public static UUID getUUIDFromPlayer(Player p) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + p.getName()).getBytes(Charsets.UTF_8));
    }

}
