package net.collapse.antiaddiction.config;

import net.collapse.antiaddiction.util.TimeParser;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    private long playtimeLimitTicks;
    private long cooldownDurationTicks;
    private int persistenceIntervalSeconds;
    private boolean scoreboardEnabled;
    private String scoreboardTitle;
    private int scoreboardRefreshInterval;
    private boolean mysqlEnabled;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private boolean redisEnabled;
    private String redisHost;
    private int redisPort;
    private boolean antiaddictionEnabled;
    private List<String> whitelist;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        playtimeLimitTicks = parseTicks(config.get("playtime-limit"), 144000);
        cooldownDurationTicks = parseTicks(config.get("cooldown-duration"), 36000);
        persistenceIntervalSeconds = config.getInt("persistence-interval", 60);
        scoreboardEnabled = config.getBoolean("scoreboard.enabled", true);
        scoreboardTitle = config.getString("scoreboard.title", "&6Anti-Addiction");
        scoreboardRefreshInterval = config.getInt("scoreboard.refresh-interval", 20);
        mysqlEnabled = config.getBoolean("mysql.enabled", false);
        mysqlHost = config.getString("mysql.host", "localhost");
        mysqlPort = config.getInt("mysql.port", 3306);
        mysqlDatabase = config.getString("mysql.database", "antiaddiction");
        mysqlUsername = config.getString("mysql.username", "root");
        mysqlPassword = config.getString("mysql.password", "");
        redisEnabled = config.getBoolean("redis.enabled", false);
        redisHost = config.getString("redis.host", "localhost");
        redisPort = config.getInt("redis.port", 6379);
        antiaddictionEnabled = config.getBoolean("antiaddiction-enabled", true);
        whitelist = new ArrayList<>(config.getStringList("whitelist"));
    }

    private long parseTicks(Object obj, long defaultTicks) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        if (obj instanceof String) {
            return TimeParser.parseToTicks((String) obj);
        }
        return defaultTicks;
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    public void saveDefaults() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    public long getPlaytimeLimitTicks() {
        return playtimeLimitTicks;
    }

    public long getCooldownDurationTicks() {
        return cooldownDurationTicks;
    }

    public int getPersistenceIntervalSeconds() {
        return persistenceIntervalSeconds;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public int getScoreboardRefreshInterval() {
        return scoreboardRefreshInterval;
    }

    public boolean isMysqlEnabled() {
        return mysqlEnabled;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public boolean isAntiaddictionEnabled() {
        return antiaddictionEnabled;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setAntiaddictionEnabled(boolean enabled) {
        this.antiaddictionEnabled = enabled;
        config.set("antiaddiction-enabled", enabled);
        save();
    }

    public void setPlaytimeLimitTicks(long ticks) {
        this.playtimeLimitTicks = ticks;
        config.set("playtime-limit", ticks);
        save();
    }

    public void setCooldownDurationTicks(long ticks) {
        this.cooldownDurationTicks = ticks;
        config.set("cooldown-duration", ticks);
        save();
    }

    public void setMysqlEnabled(boolean enabled) {
        this.mysqlEnabled = enabled;
        config.set("mysql.enabled", enabled);
        save();
    }

    public void setRedisEnabled(boolean enabled) {
        this.redisEnabled = enabled;
        config.set("redis.enabled", enabled);
        save();
    }

    public void setWhitelist(List<String> list) {
        this.whitelist = new ArrayList<>(list);
        config.set("whitelist", list);
        save();
    }
}
