package net.collapse.antiaddiction.task;

import net.collapse.antiaddiction.AntiaddictionMod;
import net.collapse.antiaddiction.cache.RedisCache;
import net.collapse.antiaddiction.config.PluginConfig;
import net.collapse.antiaddiction.storage.MySQLPlayerDataProvider;
import net.collapse.antiaddiction.storage.PlayerData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class PeriodicSaveTask extends BukkitRunnable {
    private final AntiaddictionMod plugin;
    private final PluginConfig config;
    private final MySQLPlayerDataProvider mysqlProvider;
    private final Map<UUID, PlayerData> sessionMap;

    public PeriodicSaveTask(AntiaddictionMod plugin, PluginConfig config, MySQLPlayerDataProvider mysqlProvider, Map<UUID, PlayerData> sessionMap) {
        this.plugin = plugin;
        this.config = config;
        this.mysqlProvider = mysqlProvider;
        this.sessionMap = sessionMap;
    }

    @Override
    public void run() {
        if (!config.isMysqlEnabled() || mysqlProvider == null) return;

        Collection<PlayerData> allData = sessionMap.values();
        if (allData.isEmpty()) return;

        try {
            mysqlProvider.saveAll(allData);
        } catch (Exception e) {
            plugin.getLogger().warning("Periodic save failed: " + e.getMessage());
            return;
        }

        RedisCache redisCache = plugin.getRedisCache();
        if (config.isRedisEnabled() && redisCache != null && redisCache.isConnected()) {
            for (PlayerData data : allData) {
                redisCache.put(data);
            }
        }
    }
}
