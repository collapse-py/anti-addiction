package net.collapse.antiaddiction.task;

import net.collapse.antiaddiction.AntiaddictionMod;
import net.collapse.antiaddiction.cache.RedisCache;
import net.collapse.antiaddiction.config.PluginConfig;
import net.collapse.antiaddiction.storage.MySQLPlayerDataProvider;
import net.collapse.antiaddiction.storage.PlayerData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class PlaytimeAccumulatorTask extends BukkitRunnable {
    private final AntiaddictionMod plugin;
    private final PluginConfig config;
    private final Map<UUID, PlayerData> sessionMap;

    public PlaytimeAccumulatorTask(AntiaddictionMod plugin, PluginConfig config, Map<UUID, PlayerData> sessionMap) {
        this.plugin = plugin;
        this.config = config;
        this.sessionMap = sessionMap;
    }

    @Override
    public void run() {
        if (!config.isAntiaddictionEnabled()) return;
        long now = System.currentTimeMillis();

        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerData data = sessionMap.get(uuid);
            if (data == null || data.isWhitelisted()) continue;

            if (data.getCooldownUntil() > 0 && data.getCooldownUntil() > now) {
                continue;
            }

            data.addPlaytimeTicks(1);

            if (data.getPlaytimeTicks() >= config.getPlaytimeLimitTicks()) {
                data.setCooldownUntil(now + config.getCooldownDurationTicks() * 50);
                player.kickPlayer("§c要休息");
                persist(data);
            }
        }
    }

    private void persist(PlayerData data) {
        MySQLPlayerDataProvider provider = plugin.getMysqlProvider();
        if (provider != null) {
            try {
                provider.save(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to persist player data for " + data.getUuid() + ": " + e.getMessage());
            }
        }
        RedisCache redisCache = plugin.getRedisCache();
        if (redisCache != null && redisCache.isConnected()) {
            redisCache.put(data);
        }
    }
}
