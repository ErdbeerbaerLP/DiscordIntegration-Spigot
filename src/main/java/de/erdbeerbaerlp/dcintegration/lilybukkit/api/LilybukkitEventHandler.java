package de.erdbeerbaerlp.dcintegration.lilybukkit.api;

import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import org.bukkit.event.player.PlayerChatEvent;

public abstract class LilybukkitEventHandler extends DiscordEventHandler {

    public abstract boolean onMcChatMessage(PlayerChatEvent ev);
}
