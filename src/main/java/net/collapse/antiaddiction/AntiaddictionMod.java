package net.collapse.antiaddiction;

import net.collapse.antiaddiction.cache.RedisCache;
import net.collapse.antiaddiction.commands.AntiAddiction;
import net.collapse.antiaddiction.config.PluginConfig;
import net.collapse.antiaddiction.listener.PlayerSessionListener;
import net.collapse.antiaddiction.scoreboard.ScoreboardManager;
import net.collapse.antiaddiction.storage.MySQLPlayerDataProvider;
import net.collapse.antiaddiction.storage.PlayerData;
import net.collapse.antiaddiction.task.PlaytimeAccumulatorTask;
import net.collapse.antiaddiction.task.PeriodicSaveTask;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiaddictionMod extends JavaPlugin {
    private static AntiaddictionMod instance;
    private PluginConfig config;
    private MySQLPlayerDataProvider mysqlProvider;
    private RedisCache redisCache;
    private final Map<UUID, PlayerData> sessionMap = new ConcurrentHashMap<>();
    private PlaytimeAccumulatorTask accumulatorTask;
    private PeriodicSaveTask saveTask;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;
        config = new PluginConfig(this);
        config.saveDefaults();
        config.load();

        if (config.isMysqlEnabled()) {
            initMysql();
        }

        if (config.isRedisEnabled()) {
            initRedis();
        }

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this, config, mysqlProvider, sessionMap), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(AntiAddiction.register(), "antiaddiction");
        });

        if (mysqlProvider != null) {
            Map<UUID, PlayerData> allData = mysqlProvider.loadAll();
            sessionMap.putAll(allData);
            getLogger().info("Loaded " + allData.size() + " player records from MySQL.");

            if (redisCache != null && redisCache.isConnected()) {
                for (PlayerData data : allData.values()) {
                    redisCache.put(data);
                }
                getLogger().info("Populated Redis cache with " + allData.size() + " players.");
            }
        }

        accumulatorTask = new PlaytimeAccumulatorTask(this, config, sessionMap);
        accumulatorTask.runTaskTimer(this, 0L, 1L);

        saveTask = new PeriodicSaveTask(this, config, mysqlProvider, sessionMap);
        saveTask.runTaskTimer(this, 0L, config.getPersistenceIntervalSeconds() * 20L);

        if (config.isScoreboardEnabled()) {
            scoreboardManager = new ScoreboardManager(this, config);
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                        scoreboardManager.update(player);
                    }
                }
            }.runTaskTimer(this, 0L, config.getScoreboardRefreshInterval());
        }
    }

    public static AntiaddictionMod getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return config;
    }

    @Override
    public void onDisable() {
        if (accumulatorTask != null) accumulatorTask.cancel();
        if (saveTask != null) saveTask.cancel();

        if (mysqlProvider != null) {
            try {
                mysqlProvider.saveAll(sessionMap.values());
            } catch (Exception e) {
                getLogger().warning("Failed to save on shutdown: " + e.getMessage());
            }
        }

        if (redisCache != null) {
            redisCache.close();
        }

        if (scoreboardManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                scoreboardManager.remove(player);
            }
        }

        sessionMap.clear();
    }

    public Map<UUID, PlayerData> getSessionMap() {
        return sessionMap;
    }

    public RedisCache getRedisCache() {
        return redisCache;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public MySQLPlayerDataProvider getMysqlProvider() {
        return mysqlProvider;
    }

    public void initMysql() {
        try {
            mysqlProvider = new MySQLPlayerDataProvider(
                    config.getMysqlHost(),
                    config.getMysqlPort(),
                    config.getMysqlDatabase(),
                    config.getMysqlUsername(),
                    config.getMysqlPassword()
            );
        } catch (Exception e) {
            getLogger().warning("Failed to initialize MySQL: " + e.getMessage());
            mysqlProvider = null;
        }
    }

    public void setMysqlProvider(MySQLPlayerDataProvider provider) {
        this.mysqlProvider = provider;
    }

    public void initRedis() {
        if (redisCache != null) {
            redisCache.close();
        }
        if (config.isRedisEnabled()) {
            redisCache = new RedisCache(config.getRedisHost(), config.getRedisPort());
            getLogger().info("Redis cache initialized.");
        } else {
            redisCache = null;
            getLogger().info("Redis cache disabled.");
        }
    }
}