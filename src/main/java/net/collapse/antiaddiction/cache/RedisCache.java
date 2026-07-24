package net.collapse.antiaddiction.cache;

import net.collapse.antiaddiction.storage.PlayerData;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.UUID;

public class RedisCache {
    private final String host;
    private final int port;
    private Jedis jedis;
    private volatile boolean connected = false;

    public RedisCache(String host, int port) {
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() {
        try {
            jedis = new Jedis(host, port, 2000);
            jedis.ping();
            connected = true;
        } catch (Exception e) {
            connected = false;
        }
    }

    public PlayerData get(UUID uuid) {
        if (!connected) return null;
        try {
            String key = "antiaddiction:" + uuid;
            if (!jedis.exists(key)) return null;
            Map<String, String> fields = jedis.hgetAll(key);
            if (fields.isEmpty()) return null;
            return new PlayerData(
                    uuid,
                    fields.get("name"),
                    Long.parseLong(fields.getOrDefault("playtimeTicks", "0")),
                    Long.parseLong(fields.getOrDefault("cooldownUntil", "0")),
                    Boolean.parseBoolean(fields.getOrDefault("whitelisted", "false"))
            );
        } catch (Exception e) {
            connected = false;
            return null;
        }
    }

    public void put(PlayerData data) {
        if (!connected) return;
        try {
            String key = "antiaddiction:" + data.getUuid();
            jedis.hset(key, "name", data.getName());
            jedis.hset(key, "playtimeTicks", String.valueOf(data.getPlaytimeTicks()));
            jedis.hset(key, "cooldownUntil", String.valueOf(data.getCooldownUntil()));
            jedis.hset(key, "whitelisted", String.valueOf(data.isWhitelisted()));
        } catch (Exception e) {
            connected = false;
        }
    }

    public void remove(UUID uuid) {
        if (!connected) return;
        try {
            jedis.del("antiaddiction:" + uuid);
        } catch (Exception e) {
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        if (jedis != null) {
            try { jedis.close(); } catch (Exception ignored) {
            }
        }
        connected = false;
    }
}
