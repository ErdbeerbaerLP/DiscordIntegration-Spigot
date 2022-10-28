package de.erdbeerbaerlp.dcintegration.spigot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.addon.DiscordAddonMeta;
import de.erdbeerbaerlp.dcintegration.common.compat.DynmapListener;
import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.DownloadSourceChecker;
import de.erdbeerbaerlp.dcintegration.common.util.UpdateChecker;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.bstats.Metrics;
import de.erdbeerbaerlp.dcintegration.spigot.command.McDiscordCommand;
import de.erdbeerbaerlp.dcintegration.spigot.compat.DynmapWorkaroundListener;
import de.erdbeerbaerlp.dcintegration.spigot.compat.VotifierEventListener;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotServerInterface;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapCommonAPIListener;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.*;

public class DiscordIntegration extends JavaPlugin {

    /**
     * Plugin instance
     */
    public static DiscordIntegration INSTANCE;
    final Metrics bstats = new Metrics(this, 9765);
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
            if (Configuration.instance().general.allowConfigMigration) {
                //Migrate configs from DiscordSRV, if available
                final File discordSrvDir = new File("./plugins/DiscordSRV/");
                if (discordSrvDir.exists()) {
                    final File dsrvConfig = new File(discordSrvDir, "config.yml");
                    if (dsrvConfig.exists()) {
                        Variables.LOGGER.info("Found DiscordSRV Config, attempting to migrate!");
                        final Gson gson = new Gson();
                        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dsrvConfig);
                        final Configuration conf = Configuration.instance();
                        conf.general.botToken = cfg.getString("BotToken", conf.general.botToken);
                        ConfigurationSection channels = cfg.getConfigurationSection("Channels");
                        conf.general.botChannel = channels.get("global") == null ? conf.advanced.deathsChannelID : channels.getString("global");
                        conf.advanced.deathsChannelID = channels.get("deaths") == null ? conf.advanced.deathsChannelID : channels.getString("deaths");
                        conf.commandLog.channelID = cfg.getString("DiscordConsoleChannelId", conf.commandLog.channelID);
                        if (conf.commandLog.channelID.equals("000000000000000000")) conf.commandLog.channelID = "0";
                        conf.webhook.enable = cfg.getBoolean("Experiment_WebhookChatMessageDelivery", conf.webhook.enable);
                        if (!cfg.getStringList("DiscordGameStatus").isEmpty())
                            conf.general.botStatusName = cfg.getStringList("DiscordGameStatus").get(0);
                        else if (cfg.getString("DiscordGameStatus") != null)
                            conf.general.botStatusName = cfg.getString("DiscordGameStatus");
                        conf.saveConfig();
                        Variables.LOGGER.info("Migrated " + dsrvConfig.getPath());
                        final File linkedPlayers = new File(discordSrvDir, "linkedaccounts.json");
                        if (linkedPlayers.exists()) {
                            try {
                                final JsonReader r = new JsonReader(new FileReader(linkedPlayers));
                                final JsonObject object = gson.fromJson(r, JsonObject.class);
                                object.entrySet().forEach((e) -> PlayerLinkController.migrateLinkPlayer(e.getKey(), UUID.fromString(e.getValue().getAsString())));
                                r.close();
                                Variables.LOGGER.info("Migrated " + linkedPlayers.getPath());
                            } catch (IOException e) {
                                Variables.LOGGER.error("Failed to migrate " + linkedPlayers.getPath());
                                e.printStackTrace();
                            }
                        }
                        Variables.LOGGER.info("Migration done! Renaming DiscordSRV's config directory...");
                        File backupDir = new File("./plugins/DiscordSRV_" + System.nanoTime() + "/");

                        try {
                            Files.move(discordSrvDir.toPath(), backupDir.toPath());
                            Variables.LOGGER.info("DONE");
                        } catch (IOException e) {
                            Variables.LOGGER.error("Failed. Plugin might migrate again at next startup");
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Config loading failed");
            e.printStackTrace();
        }
        //Load Discord Integration

        discord_instance = new Discord(new SpigotServerInterface());
        active = true;

        try {
            //Wait a short time to allow JDA to get initialized
            Variables.LOGGER.info("Waiting for JDA to initialize to send starting message... (max 5 seconds before skipping)");
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
    public void onEnable() {
        if (!active && discord_instance == null) loadDiscordInstance(); //In case of /reload or similar
        PluginManager pm = getServer().getPluginManager();

        if (!Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) { //Prevent events when token not set
            pm.registerEvents(new SpigotEventListener(), this);
            if (pm.getPlugin("Votifier") != null) {
                pm.registerEvents(new VotifierEventListener(), this);
            }
            if (pm.getPlugin("dynmap") != null) {
                org.dynmap.DynmapCommonAPIListener.register((DynmapCommonAPIListener) (dynmapListener = new DynmapListener(true)));
                pm.registerEvents(new DynmapWorkaroundListener(), this);
            }
            final PluginCommand cmd = getServer().getPluginCommand("discord");
            cmd.setExecutor(new McDiscordCommand());
            cmd.setTabCompleter(new McDiscordCommand.TabCompleter());

            bstats.addCustomChart(new Metrics.SimplePie("webhook_mode", () -> Configuration.instance().webhook.enable ? "Enabled" : "Disabled"));
            bstats.addCustomChart(new Metrics.SimplePie("command_log", () -> !Configuration.instance().commandLog.channelID.equals("0") ? "Enabled" : "Disabled"));
        }

        //Run only after server is started
        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            Variables.LOGGER.info("Started");
            started = new Date().getTime();
            if (discord_instance != null)
                if (startingMsg != null) {
                    startingMsg.thenAccept((a) -> a.editMessage(Localization.instance().serverStarted).queue());
                } else discord_instance.sendMessage(Localization.instance().serverStarted);

            //Add addon stats
            bstats.addCustomChart(new Metrics.DrilldownPie("addons", () -> {
                final Map<String, Map<String, Integer>> map = new HashMap<>();
                if (Configuration.instance().bstats.sendAddonStats) {  //Only send if enabled, else send empty map
                    for (DiscordAddonMeta m : AddonLoader.getAddonMetas()) {
                        final Map<String, Integer> entry = new HashMap<>();
                        entry.put(m.getVersion(), 1);
                        map.put(m.getName(), entry);
                    }
                }
                return map;
            }));
            if (discord_instance != null) {
                discord_instance.startThreads();
            }
            UpdateChecker.runUpdateCheck("https://raw.githubusercontent.com/ErdbeerbaerLP/DiscordIntegration-Spigot/master/update_checker.json");

            if (!DownloadSourceChecker.checkDownloadSource(new File(DiscordIntegration.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("%")[0]))) {
                Variables.LOGGER.warn("You likely got this mod from a third party website.");
                Variables.LOGGER.warn("Some of such websites are distributing malware or old versions.");
                Variables.LOGGER.warn("Download this mod from an official source (https://www.curseforge.com/minecraft/mc-mods/dcintegration) to hide this message");
                Variables.LOGGER.warn("This warning can also be suppressed in the config file");
            }
        }, 30);
    }

    @Override
    public void reloadConfig() {
        try {
            Configuration.instance().loadConfig();
        } catch (IOException e) {
            System.err.println("Config loading failed");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        active = false;
        if (discord_instance != null) {
            discord_instance.sendMessage(Localization.instance().serverStopped);
            discord_instance.kill(false);
            if (getServer().getPluginManager().getPlugin("dynmap") != null && dynmapListener != null) {
                org.dynmap.DynmapCommonAPIListener.unregister((DynmapCommonAPIListener) dynmapListener);
            }
        }
        HandlerList.unregisterAll(this);
    }
}
