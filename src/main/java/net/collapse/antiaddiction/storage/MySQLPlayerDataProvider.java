package net.collapse.antiaddiction.storage;

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySQLPlayerDataProvider implements AutoCloseable {
    private final String url;
    private final String user;
    private final String password;
    private final Logger logger;

    /**
     * 5 參數建構子（保留舊有呼叫相容性）
     */
    public MySQLPlayerDataProvider(String host, int port, String database, String user, String password) {
        this(host, port, database, user, password, Logger.getLogger("Minecraft"));
    }

    /**
     * 6 參數建構子（推薦，可自訂 Logger）
     */
    public MySQLPlayerDataProvider(String host, int port, String database, String user, String password, Logger logger) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database 
            + "?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true&characterEncoding=UTF-8&serverTimezone=UTC";
        this.user = user;
        this.password = password;
        this.logger = logger;
    
        ensureTableExists();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS `anti_addiction_players` (" +
                "`uuid` VARCHAR(36) PRIMARY KEY," +
                "`name` VARCHAR(64) NOT NULL," +
                "`playtime_ticks` BIGINT NOT NULL DEFAULT 0," +
                "`cooldown_until` BIGINT NOT NULL DEFAULT 0," +
                "`whitelisted` BOOLEAN NOT NULL DEFAULT FALSE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[anti-addiction] 創建數據表失敗！請檢查數據庫權限與配置。", e);
            throw new RuntimeException("Failed to create anti-addiction table", e);
        }
    }

    public PlayerData load(UUID uuid) {
        String sql = "SELECT `name`, `playtime_ticks`, `cooldown_until`, `whitelisted` FROM `anti_addiction_players` WHERE `uuid` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            uuid,
                            rs.getString("name"),
                            rs.getLong("playtime_ticks"),
                            rs.getLong("cooldown_until"),
                            rs.getBoolean("whitelisted")
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[anti-addiction] 讀取玩家資料失敗: " + uuid, e);
        }
        return null;
    }

    public Map<UUID, PlayerData> loadAll() {
        Map<UUID, PlayerData> map = new HashMap<>();
        String sql = "SELECT `uuid`, `name`, `playtime_ticks`, `cooldown_until`, `whitelisted` FROM `anti_addiction_players`";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                map.put(uuid, new PlayerData(
                        uuid,
                        rs.getString("name"),
                        rs.getLong("playtime_ticks"),
                        rs.getLong("cooldown_until"),
                        rs.getBoolean("whitelisted")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[anti-addiction] 批次載入所有玩家資料失敗！", e);
        }
        return map;
    }

    public void save(PlayerData data) {
        String sql = "INSERT INTO `anti_addiction_players` (`uuid`, `name`, `playtime_ticks`, `cooldown_until`, `whitelisted`) " +
                "VALUES (?, ?, ?, ?, ?) AS new " +
                "ON DUPLICATE KEY UPDATE " +
                "`name` = new.`name`, " +
                "`playtime_ticks` = new.`playtime_ticks`, " +
                "`cooldown_until` = new.`cooldown_until`, " +
                "`whitelisted` = new.`whitelisted`";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getName());
            ps.setLong(3, data.getPlaytimeTicks());
            ps.setLong(4, data.getCooldownUntil());
            ps.setBoolean(5, data.isWhitelisted());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[anti-addiction] 儲存玩家資料失敗: " + data.getName(), e);
            throw new RuntimeException("Failed to save player data", e);
        }
    }

    public void saveAll(Collection<PlayerData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        String sql = "INSERT INTO `anti_addiction_players` (`uuid`, `name`, `playtime_ticks`, `cooldown_until`, `whitelisted`) " +
                "VALUES (?, ?, ?, ?, ?) AS new " +
                "ON DUPLICATE KEY UPDATE " +
                "`name` = new.`name`, " +
                "`playtime_ticks` = new.`playtime_ticks`, " +
                "`cooldown_until` = new.`cooldown_until`, " +
                "`whitelisted` = new.`whitelisted`";

        try (Connection conn = getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (PlayerData data : dataList) {
                    ps.setString(1, data.getUuid().toString());
                    ps.setString(2, data.getName());
                    ps.setLong(3, data.getPlaytimeTicks());
                    ps.setLong(4, data.getCooldownUntil());
                    ps.setBoolean(5, data.isWhitelisted());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[anti-addiction] 批次儲存玩家資料失敗！", e);
            throw new RuntimeException("Failed to batch save player data", e);
        }
    }

    public void delete(UUID uuid) {
        String sql = "DELETE FROM `anti_addiction_players` WHERE `uuid` = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[anti-addiction] 刪除玩家資料失敗: " + uuid, e);
        }
    }

    @Override
    public void close() {
        // 預留介面：若日後替換為 HikariCP，在此處關閉 DataSource 即可
    }
}