package net.collapse.antiaddiction.listener;

import net.collapse.antiaddiction.AntiaddictionMod;
import net.collapse.antiaddiction.cache.RedisCache;
import net.collapse.antiaddiction.config.PluginConfig;
import net.collapse.antiaddiction.storage.MySQLPlayerDataProvider;
import net.collapse.antiaddiction.storage.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

public class PlayerSessionListener implements Listener {
    private final AntiaddictionMod plugin;
    private final PluginConfig config;
    private final MySQLPlayerDataProvider mysqlProvider;
    private final Map<UUID, PlayerData> sessionMap;

    public PlayerSessionListener(AntiaddictionMod plugin, PluginConfig config, MySQLPlayerDataProvider mysqlProvider, Map<UUID, PlayerData> sessionMap) {
        this.plugin = plugin;
        this.config = config;
        this.mysqlProvider = mysqlProvider;
        this.sessionMap = sessionMap;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        PlayerData data = sessionMap.get(uuid);
        if (data == null) {
            data = loadData(uuid);
        }
        if (data == null) {
            data = new PlayerData(uuid, name);
        } else {
            data.setName(name);
        }

        if (config.getWhitelist().contains(name)) {
            data.setWhitelisted(true);
        }

        if (!data.isWhitelisted()) {
            long now = System.currentTimeMillis();

            if (data.getCooldownUntil() > 0) {
                if (data.getCooldownUntil() > now) {
                    long remaining = data.getCooldownUntil() - now;
                    player.kickPlayer("§cYou are in cooldown! Please wait " + formatTime(remaining) + " before playing again.");
                    sessionMap.put(uuid, data);
                    return;
                } else {
                    data.setPlaytimeTicks(0);
                    data.setCooldownUntil(0);
                    plugin.getLogger().info("Cooldown expired for " + name + ", playtime reset.");
                }
            }

            if (data.getPlaytimeTicks() >= config.getPlaytimeLimitTicks()) {
                long cooldownEnd = System.currentTimeMillis() + config.getCooldownDurationTicks() * 50;
                data.setCooldownUntil(cooldownEnd);
                player.kickPlayer("§c要休息");
                plugin.getLogger().info("Player " + name + " reached playtime limit. Cooldown until: " + cooldownEnd);
                persist(data);
                sessionMap.put(uuid, data);
                return;
            }
        }

        sessionMap.put(uuid, data);

        RedisCache redisCache = plugin.getRedisCache();
        if (config.isRedisEnabled() && redisCache != null && redisCache.isConnected()) {
            redisCache.put(data);
        }

        net.collapse.antiaddiction.scoreboard.ScoreboardManager sb = plugin.getScoreboardManager();
        if (sb != null) {
            sb.update(player);
        }
    }

    private PlayerData loadData(UUID uuid) {
        RedisCache redisCache = plugin.getRedisCache();
        if (config.isRedisEnabled() && redisCache != null && redisCache.isConnected()) {
            PlayerData data = redisCache.get(uuid);
            if (data != null) return data;
        }

        if (mysqlProvider != null) {
            return mysqlProvider.load(uuid);
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = sessionMap.get(uuid);
        if (data != null) {
            persist(data);
        }

        net.collapse.antiaddiction.scoreboard.ScoreboardManager sb = plugin.getScoreboardManager();
        if (sb != null) {
            sb.remove(event.getPlayer());
        }
    }

    private void persist(PlayerData data) {
        if (mysqlProvider != null) {
            try {
                mysqlProvider.save(data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to persist player data for " + data.getUuid() + ": " + e.getMessage());
            }
        }
        RedisCache redisCache = plugin.getRedisCache();
        if (config.isRedisEnabled() && redisCache != null && redisCache.isConnected()) {
            redisCache.put(data);
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
