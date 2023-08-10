package de.erdbeerbaerlp.dcintegration.spigot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.addon.DiscordAddonMeta;
import de.erdbeerbaerlp.dcintegration.common.compat.DynmapListener;
import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerSettings;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.DownloadSourceChecker;
import de.erdbeerbaerlp.dcintegration.common.util.UpdateChecker;
import de.erdbeerbaerlp.dcintegration.spigot.bstats.Metrics;
import de.erdbeerbaerlp.dcintegration.spigot.command.McDiscordCommand;
import de.erdbeerbaerlp.dcintegration.spigot.compat.DynmapWorkaroundListener;
import de.erdbeerbaerlp.dcintegration.spigot.compat.VotifierEventListener;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotServerInterface;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
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

public class DiscordIntegrationPlugin extends JavaPlugin {

    /**
     * Plugin instance
     */
    public static DiscordIntegrationPlugin INSTANCE;
    final Metrics bstats = new Metrics(this, 9765);
    /**
     * Used to detect plugin reloads in onEnable
     */
    public static boolean active = false;
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
        DiscordIntegration.configFile = new File("./plugins/DiscordIntegration/config.toml");
        if (!DiscordIntegration.discordDataDir.exists()) DiscordIntegration.discordDataDir.mkdir();
        try {
            DiscordIntegration.loadConfigs();
            if (Configuration.instance().general.allowConfigMigration) {
                //Migrate configs from DiscordSRV, if available
                final File discordSrvDir = new File("./plugins/DiscordSRV/");
                if (discordSrvDir.exists()) {
                    final File dsrvConfig = new File(discordSrvDir, "config.yml");
                    if (dsrvConfig.exists()) {
                        DiscordIntegration.LOGGER.info("Found DiscordSRV Config, attempting to migrate!");
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
                        DiscordIntegration.LOGGER.info("Migrated " + dsrvConfig.getPath());
                        final File linkedPlayers = new File(discordSrvDir, "linkedaccounts.json");
                        if (linkedPlayers.exists()) {
                            try {
                                final JsonReader r = new JsonReader(new FileReader(linkedPlayers));
                                final JsonObject object = gson.fromJson(r, JsonObject.class);
                                object.entrySet().forEach((e) -> LinkManager.addLink(new PlayerLink(e.getKey(), e.getValue().getAsString(), null, new PlayerSettings())));
                                r.close();
                                DiscordIntegration.LOGGER.info("Migrated " + linkedPlayers.getPath());
                            } catch (IOException e) {
                                DiscordIntegration.LOGGER.error("Failed to migrate " + linkedPlayers.getPath());
                                e.printStackTrace();
                            }
                        }
                        DiscordIntegration.LOGGER.info("Migration done! Renaming DiscordSRV's config directory...");
                        File backupDir = new File("./plugins/DiscordSRV_" + System.nanoTime() + "/");

                        try {
                            Files.move(discordSrvDir.toPath(), backupDir.toPath());
                            DiscordIntegration.LOGGER.info("DONE");
                        } catch (IOException e) {
                            DiscordIntegration.LOGGER.error("Failed. Plugin might migrate again at next startup");
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

        DiscordIntegration.INSTANCE = new DiscordIntegration(new SpigotServerInterface());
        active = true;

        try {
            //Wait a short time to allow JDA to get initialized
            DiscordIntegration.LOGGER.info("Waiting for JDA to initialize to send starting message... (max 5 seconds before skipping)");
            for (int i = 0; i <= 5; i++) {
                if (DiscordIntegration.INSTANCE.getJDA() == null) Thread.sleep(1000);
                else break;
            }
            if (DiscordIntegration.INSTANCE.getJDA() != null) {
                Thread.sleep(2000); //Wait for it to cache the channels
                CommandRegistry.registerDefaultCommands();
                if (!Localization.instance().serverStarting.isEmpty()) {

                    if (!Localization.instance().serverStarting.isBlank())
                        if (DiscordIntegration.INSTANCE.getChannel() != null) {
                            final MessageCreateData m;
                            if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed)
                                m = new MessageCreateBuilder().setEmbeds(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarting).build()).build();
                            else
                                m = new MessageCreateBuilder().addContent(Localization.instance().serverStarting).build();
                            DiscordIntegration.startingMsg = DiscordIntegration.INSTANCE.sendMessageReturns(m, DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                        }
                }
            }

        } catch (InterruptedException | NullPointerException ignored) {
        }
    }

    @Override
    public void onEnable() {
        if (!active && DiscordIntegration.INSTANCE == null) loadDiscordInstance(); //In case of /reload or similar
        PluginManager pm = getServer().getPluginManager();

        if (!Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) { //Prevent events when token not set
            pm.registerEvents(new SpigotEventListener(), this);
            if (pm.getPlugin("Votifier") != null) {
                pm.registerEvents(new VotifierEventListener(), this);
            }
            if (pm.getPlugin("dynmap") != null) {
                DynmapCommonAPIListener.register((DynmapCommonAPIListener) (dynmapListener = new DynmapListener(true)));
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
            DiscordIntegration.LOGGER.info("Started");
            DiscordIntegration.started = new Date().getTime();
            if (DiscordIntegration.INSTANCE != null)
                if (!Localization.instance().serverStarted.isBlank())
                    if (DiscordIntegration.startingMsg != null) {
                        if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed) {
                            if (!Configuration.instance().embedMode.startMessages.customJSON.isBlank()) {
                                final EmbedBuilder b = Configuration.instance().embedMode.startMessages.toEmbedJson(Configuration.instance().embedMode.startMessages.customJSON);
                                DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessageEmbeds(b.build()).queue());
                            } else
                                DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessageEmbeds(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarted).build()).queue());
                        } else
                            DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessage(Localization.instance().serverStarted).queue());
                    } else {
                        if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed) {
                            if (!Configuration.instance().embedMode.startMessages.customJSON.isBlank()) {
                                final EmbedBuilder b = Configuration.instance().embedMode.startMessages.toEmbedJson(Configuration.instance().embedMode.startMessages.customJSON);
                                DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                            } else
                                DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarted).build()));
                        } else
                            DiscordIntegration.INSTANCE.sendMessage(Localization.instance().serverStarted);
                    }
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
            if (DiscordIntegration.INSTANCE != null) {
                DiscordIntegration.INSTANCE.startThreads();
            }
            UpdateChecker.runUpdateCheck("https://raw.githubusercontent.com/ErdbeerbaerLP/DiscordIntegration-Spigot/1.20/update_checker.json");

            if (!DownloadSourceChecker.checkDownloadSource(new File(DiscordIntegrationPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("%")[0]))) {
                DiscordIntegration.LOGGER.warn("You likely got this mod from a third party website.");
                DiscordIntegration.LOGGER.warn("Some of such websites are distributing malware or old versions.");
                DiscordIntegration.LOGGER.warn("Download this mod from an official source (https://www.curseforge.com/minecraft/mc-mods/dcintegration) to hide this message");
                DiscordIntegration.LOGGER.warn("This warning can also be suppressed in the config file");
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
        if (DiscordIntegration.INSTANCE != null) {
            if (!Localization.instance().serverStopped.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.stopMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.stopMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.stopMessages.toEmbedJson(Configuration.instance().embedMode.stopMessages.customJSON);
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Configuration.instance().embedMode.stopMessages.toEmbed().setDescription(Localization.instance().serverStopped).build()));
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().serverStopped);
            DiscordIntegration.INSTANCE.stopThreads();
            DiscordIntegration.INSTANCE.kill(true);
            if (getServer().getPluginManager().getPlugin("dynmap") != null && dynmapListener != null) {
                DynmapCommonAPIListener.unregister((DynmapCommonAPIListener) dynmapListener);
            }
        }
        HandlerList.unregisterAll(this);
    }
}
