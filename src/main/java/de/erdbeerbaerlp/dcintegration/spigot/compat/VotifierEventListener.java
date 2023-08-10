package de.erdbeerbaerlp.dcintegration.spigot.compat;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class VotifierEventListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVotifierEvent(VotifierEvent event) {
        final Vote vote = event.getVote();
        DiscordIntegration.INSTANCE.sendMessage(DiscordIntegration.INSTANCE.getChannel(Configuration.instance().votifier.votifierChannelID), Configuration.instance().votifier.message.replace("%site%", vote.getServiceName()).replace("%player%", vote.getUsername()).replace("%addr%", vote.getAddress()), Configuration.instance().votifier.avatarURL, Configuration.instance().votifier.name);
    }
}
