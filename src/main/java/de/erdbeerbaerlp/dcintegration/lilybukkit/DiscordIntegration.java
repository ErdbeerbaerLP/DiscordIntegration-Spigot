package de.erdbeerbaerlp.dcintegration.lilybukkit;

import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.util.DownloadSourceChecker;
import de.erdbeerbaerlp.dcintegration.common.util.UpdateChecker;
import de.erdbeerbaerlp.dcintegration.lilybukkit.commands.McDiscordCommand;
import de.erdbeerbaerlp.dcintegration.lilybukkit.events.LilypadPlayerListener;
import de.erdbeerbaerlp.dcintegration.lilybukkit.util.LilybukkitServerInterface;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.*;
public class DiscordIntegration extends JavaPlugin {
    /**
     * Plugin instance
     */
    public static DiscordIntegration INSTANCE;
    /**
     * Used to detect plugin reloads in onEnable
     */
    private boolean active = false;
    public Object dynmapListener;

    @Override
    public void onLoad() {
        loadDiscordInstance();
    }
    /**
     * Loads JDA and Config files
     */
    private void loadDiscordInstance() {

        INSTANCE = this;


        //Define config file and load config
        configFile = new File("./plugins/DiscordIntegration/config.toml");
        if (!discordDataDir.exists()) discordDataDir.mkdir();
        try {
            Discord.loadConfigs();
        } catch (IOException e) {
            System.err.println("Config loading failed");
            e.printStackTrace();
        }
        //Load Discord Integration

        discord_instance = new Discord(new LilybukkitServerInterface());
        active = true;

        try {
            //Wait a short time to allow JDA to get initialized
            System.out.println("Waiting for JDA to initialize to send starting message... (max 5 seconds before skipping)");
            for (int i = 0; i <= 5; i++) {
                if (discord_instance.getJDA() == null) Thread.sleep(1000);
                else break;
            }
            if (discord_instance.getJDA() != null && !Localization.instance().serverStarting.isEmpty()) {
                Thread.sleep(5000); //Wait for it to cache the channels (hopefully this fixes channel retrieving issues)
                if (discord_instance.getChannel() != null)
                    startingMsg = discord_instance.sendMessageReturns(Localization.instance().serverStarting, discord_instance.getChannel(Configuration.instance().advanced.serverChannelID));
            }

            CommandRegistry.registerDefaultCommandsFromConfig();

        } catch (InterruptedException | NullPointerException ignored) {
        }
    }
    @Override
    public InputStream getResource(String s) {
        return null;
    }

    @Override
    public void onDisable() {
        active = false;
        if (discord_instance != null) {
            discord_instance.sendMessage(Localization.instance().serverStopped);
            discord_instance.kill(false);
        }
    }

    @Override
    public void onEnable() {
        if (!active && discord_instance == null) loadDiscordInstance(); //In case of /reload or similar
        PluginManager pm = getServer().getPluginManager();

        if (!Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) { //Prevent events when token not set
            //Register events
            final LilypadPlayerListener evh = new LilypadPlayerListener();
            pm.registerEvent(Event.Type.PLAYER_CHAT, evh, Event.Priority.Monitor, this);
            pm.registerEvent(Event.Type.PLAYER_LOGIN, evh, Event.Priority.Normal, this);
            pm.registerEvent(Event.Type.PLAYER_PRELOGIN, evh, Event.Priority.Highest, this);
            pm.registerEvent(Event.Type.PLAYER_JOIN, evh, Event.Priority.Monitor, this);
            pm.registerEvent(Event.Type.PLAYER_QUIT, evh, Event.Priority.Monitor, this);
            final PluginCommand cmd = getServer().getPluginCommand("discord");
            cmd.setExecutor(new McDiscordCommand());



        //Run only after server is started
            System.out.println("Started");
            started = new Date().getTime();
            if (discord_instance != null)
                if (startingMsg != null) {
                    startingMsg.thenAccept((a) -> a.editMessage(Localization.instance().serverStarted).queue());
                } else discord_instance.sendMessage(Localization.instance().serverStarted);
            if (discord_instance != null) {
                discord_instance.startThreads();
            }
            UpdateChecker.runUpdateCheck("https://raw.githubusercontent.com/ErdbeerbaerLP/DiscordIntegration-Spigot/master/update_checker.json");

            if (!DownloadSourceChecker.checkDownloadSource(new File(DiscordIntegration.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("%")[0]))) {
                System.out.println("You likely got this mod from a third party website.");
                System.out.println("Some of such websites are distributing malware or old versions.");
                System.out.println("Download this mod from an official source (https://www.curseforge.com/minecraft/mc-mods/dcintegration) to hide this message");
                System.out.println("This warning can also be suppressed in the config file");
            }
        };
    }
}
