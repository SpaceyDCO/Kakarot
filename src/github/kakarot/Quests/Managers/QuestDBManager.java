package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import github.kakarot.Quests.Models.PlayerQuestProgress;
import github.kakarot.Quests.Models.QuestStatus;
import github.kakarot.Quests.Quest;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

@Slf4j
public class QuestDBManager {
    private final Main plugin;
    private final QuestDBConfig dbConfig;
    public QuestDBManager(Main plugin) {
        this.plugin = plugin;
        this.dbConfig = plugin.getDatabaseConfig();
    }
    public boolean pickupQuest(UUID playerUUID, int questId, int objectiveCount) {
        Connection conn = this.dbConfig.getConnection();
        if(conn == null) {
            plugin.getLogger().severe("Cannot pickup quest - database not connected!");
            return false;
        }
        try {
            conn.setAutoCommit(false);
            String insertProgress = "INSERT INTO player_progress " + "(player_uuid, quest_id, quest_status, picked_up_at) " + "VALUES (?, ?, 'IN_PROGRESS', ?)";
            try(PreparedStatement statement = conn.prepareStatement(insertProgress)) {
                statement.setString(1, playerUUID.toString());
                statement.setInt(2, questId);
                statement.setLong(3, System.currentTimeMillis());
                int rowsAffected = statement.executeUpdate();
                if(rowsAffected == 0) {
                    plugin.getLogger().warning("Failed to insert quest progress for " + playerUUID);
                    conn.rollback();
                    return false;
                }
            }
            String insertObjectives = "INSERT INTO player_objective_progress " + "(player_uuid, quest_id, objective_index, objective_progress) " + "VALUES (?, ?, ?, 0)";
            try(PreparedStatement statement = conn.prepareStatement(insertObjectives)) {
                for(int i = 0; i < objectiveCount; i++) {
                    statement.setString(1, playerUUID.toString());
                    statement.setInt(2, questId);
                    statement.setInt(3, i);
                    statement.addBatch();
                }
                int[] results = statement.executeBatch();
                if(results.length != objectiveCount) {
                    plugin.getLogger().warning("Failed to insert all objectives for quest " + questId);
                    conn.rollback();
                    return false;
                }
            }
            conn.commit();
            plugin.getLogger().info(String.format("Player %s picked up quest %d with %d objectives", playerUUID, questId, objectiveCount));
            return true;
        }catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error recording quest pickup for " + playerUUID, e);
            try {
                conn.rollback();
            }catch(SQLException rollbackEx) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction!", rollbackEx);
            }
            return false;
        }finally {
            try {
              conn.setAutoCommit(true);
            }catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to restore auto-commit!", e);
            }
        }
    }
    public boolean pickupRepeatableQuest(UUID playerUUID, int questId, int objectiveCount) {
        Connection conn = this.dbConfig.getConnection();
        if(conn == null) {
            plugin.getLogger().severe("Cannot pickup repeatable quest! - database not connected");
            return false;
        }
        try {
            conn.setAutoCommit(false);
            String updateProgress = "UPDATE player_progress " + "SET quest_status = 'IN_PROGRESS', " + "picked_up_at = ?, " + "WHERE player_uuid = ? AND quest_id = ?";
            try(PreparedStatement statement = conn.prepareStatement(updateProgress)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, playerUUID.toString());
                statement.setInt(3, questId);
                int rowsAffected = statement.executeUpdate();
                if(rowsAffected == 0) {
                    plugin.getLogger().severe(String.format("Failed to update repeatable quest progress for %s (quest %d not found)", playerUUID, questId));
                    conn.rollback();
                    return false;
                }
            }
            String resetObjectives = "UPDATE player_objective_progress SET objective_progress = 0 WHERE player_uuid = ? AND quest_id = ?";
            try(PreparedStatement statement = conn.prepareStatement(resetObjectives)) {
                statement.setString(1, playerUUID.toString());
                statement.setInt(2, questId);
                int rowsAffected = statement.executeUpdate();
                if(rowsAffected != objectiveCount) {
                    plugin.getLogger().severe(String.format("Expected to reset %d objectives but afected %d rows for quest %d", objectiveCount, rowsAffected, questId));
                }
            }
            conn.commit();
            plugin.getLogger().info(String.format("Player %s picked up repeatable quest %d (objectives reset to 0)", playerUUID.toString(), questId));
            return true;
        }catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error picking up quest for player " + playerUUID, e);
            try {
                conn.rollback();
            }catch(SQLException rollbackEx) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction!", rollbackEx);
            }
            return false;
        }finally {
            try {
                conn.setAutoCommit(true);
            }catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to restore auto-commit!", e);
            }
        }
    }
    public boolean updateObjectiveProgress(UUID playerUUID, int questId, int objectiveIndex, int newProgress) {
        Connection connection = this.dbConfig.getConnection();
        if(connection == null) {
            plugin.getLogger().severe("Cannot update objective - database not connected!");
            return false;
        }
        String sql = "UPDATE player_objective_progress " + "SET objective_progress = ? " + "WHERE player_uuid = ? AND quest_id = ? AND objective_index = ?";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, newProgress);
            statement.setString(2, playerUUID.toString());
            statement.setInt(3, questId);
            statement.setInt(4, objectiveIndex);
            int rowsAffected = statement.executeUpdate();
            if(rowsAffected == 0) {
                plugin.getLogger().warning(String.format("No objective found to update: player=%s, quest=%d, objective=%d", playerUUID, questId, objectiveIndex));
                return false;
            }
            plugin.getLogger().info(String.format("Updated objective progress: player=%s, quest=%d, objective=%d, progress=%d", playerUUID, questId, objectiveIndex, newProgress));
            return true;
        }catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating objective progress for " + playerUUID, e);
            return false;
        }
    }
    public boolean incrementObjectiveProgress(UUID playerUUID, int questId, int objectiveIndex, int increment) {
        Connection conn = this.dbConfig.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Cannot increment objective - database not connected!");
            return false;
        }
        String sql = "UPDATE player_objective_progress " +
                "SET objective_progress = objective_progress + ? " +
                "WHERE player_uuid = ? AND quest_id = ? AND objective_index = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, increment);
            stmt.setString(2, playerUUID.toString());
            stmt.setInt(3, questId);
            stmt.setInt(4, objectiveIndex);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                plugin.getLogger().warning(String.format("No objective found to increment: player=%s, quest=%d, objective=%d", playerUUID, questId, objectiveIndex));
                return false;
            }
            plugin.getLogger().fine(String.format(
                    "Incremented objective progress: player=%s, quest=%d, objective=%d, +%d",
                    playerUUID, questId, objectiveIndex, increment
            ));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error incrementing objective progress for " + playerUUID, e);
            return false;
        }
    }
    public boolean completeQuest(UUID playerUUID, int questId, long nextAvailable) {
        Connection conn = this.dbConfig.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Cannot complete quest - database not connected!");
            return false;
        }
        long now = System.currentTimeMillis();
        String sql = "UPDATE player_progress " + "SET quest_status = 'COMPLETED', " + "completed_at = COALESCE(completed_at, ?), " + //Only set if NULL (first completion)
                "last_completed = ?, " + //Always update last completion
                "next_available = ? " + //Next available timestamp
                "WHERE player_uuid = ? AND quest_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, now); //completed_at (only if first time)
            stmt.setLong(2, now); //last_completed (always)
            stmt.setLong(3, nextAvailable);
            stmt.setString(4, playerUUID.toString());
            stmt.setInt(5, questId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                plugin.getLogger().warning(String.format("No quest found to complete: player=%s, quest=%d", playerUUID, questId));
                return false;
            }
            plugin.getLogger().info(String.format(
                    "Player %s completed quest %d",
                    playerUUID, questId
            ));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error completing quest for " + playerUUID, e);
            return false;
        }
    }
    public boolean setNextAvailable(UUID playerUUID, int questId, long nextAvailableTimestamp) {
        Connection conn = this.dbConfig.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Cannot set next available - database not connected!");
            return false;
        }
        String sql = "UPDATE player_progress " +
                "SET next_available = ?, " +
                "quest_status = 'NOT_PICKED_UP' " + // Reset to not picked up
                "WHERE player_uuid = ? AND quest_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, nextAvailableTimestamp);
            stmt.setString(2, playerUUID.toString());
            stmt.setInt(3, questId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                plugin.getLogger().warning(String.format("No quest found to set next available: player=%s, quest=%d", playerUUID, questId));
                return false;
            }
            plugin.getLogger().info(String.format("Set next available for player %s, quest %d: %d", playerUUID, questId, nextAvailableTimestamp));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting next available for " + playerUUID, e);
            return false;
        }
    }
    public boolean abandonQuest(UUID playerUUID, int questId) {
        Connection conn = this.dbConfig.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Cannot abandon quest - database not connected!");
            return false;
        }
        // Only allow abandoning IN_PROGRESS quests (can't abandon completed ones)
        String sql = "DELETE FROM player_progress " + "WHERE player_uuid = ? AND quest_id = ? AND quest_status = 'IN_PROGRESS'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setInt(2, questId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                plugin.getLogger().warning(String.format("Cannot abandon quest (not IN_PROGRESS or doesn't exist): player=%s, quest=%d", playerUUID, questId));
                return false;
            }
            plugin.getLogger().info(String.format("Player %s abandoned quest %d", playerUUID, questId));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error abandoning quest for " + playerUUID, e);
            return false;
        }
    }
    public Map<Integer, PlayerQuestProgress> loadAllPlayerQuests(UUID playerUUID) {
        Connection conn = this.dbConfig.getConnection();
        Map<Integer, PlayerQuestProgress> questMap = new HashMap<>();
        if (conn == null) {
            plugin.getLogger().severe("Cannot load player quests - database not connected!");
            return questMap;
        }
        try {
            String sqlProgress = "SELECT quest_id, quest_status, picked_up_at, completed_at, last_completed, next_available " +
                    "FROM player_progress " +
                    "WHERE player_uuid = ?";
            Map<Integer, QuestProgressData> progressDataMap = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(sqlProgress)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int questId = rs.getInt("quest_id");
                        QuestProgressData data = new QuestProgressData();
                        data.questId = questId;
                        data.status = rs.getString("quest_status");
                        data.pickedUpAt = rs.getLong("picked_up_at");
                        data.completedAt = rs.getLong("completed_at");
                        data.lastCompleted = rs.getLong("last_completed");
                        data.nextAvailable = rs.getLong("next_available");
                        progressDataMap.put(questId, data);
                    }
                }
            }
            if (progressDataMap.isEmpty()) {
                return questMap;
            }
            String sqlObjectives = "SELECT quest_id, objective_index, objective_progress " +
                    "FROM player_objective_progress " +
                    "WHERE player_uuid = ? " +
                    "ORDER BY quest_id ASC, objective_index ASC";
            Map<Integer, Map<Integer, Integer>> objectivesMap = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(sqlObjectives)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int questId = rs.getInt("quest_id");
                        int objectiveIndex = rs.getInt("objective_index");
                        int progress = rs.getInt("objective_progress");
                        objectivesMap
                                .computeIfAbsent(questId, k -> new HashMap<>())
                                .put(objectiveIndex, progress);
                    }
                }
            }
            QuestManager questManager = plugin.getQuestManager();
            for (Map.Entry<Integer, QuestProgressData> entry : progressDataMap.entrySet()) {
                int questId = entry.getKey();
                QuestProgressData data = entry.getValue();
                Quest quest = questManager.getQuest(questId);
                if (quest == null) {
                    plugin.getLogger().warning(String.format("Quest %d found in database but not in QuestManager! Skipping...", questId));
                    continue;
                }
                PlayerQuestProgress progress = new PlayerQuestProgress(playerUUID, questId, quest);
                progress.setStatus(QuestStatus.valueOf(data.status));
                progress.setPickedUpAt(data.pickedUpAt);
                progress.setCompletedAt(data.completedAt);
                progress.setLastCompleted(data.lastCompleted);
                progress.setNextAvailable(data.nextAvailable);
                Map<Integer, Integer> objectives = objectivesMap.get(questId);
                if (objectives != null) {
                    for (Map.Entry<Integer, Integer> objEntry : objectives.entrySet()) {
                        int index = objEntry.getKey();
                        int objProgress = objEntry.getValue();
                        if (index >= 0 && index < progress.getObjectiveProgress().length) {
                            progress.getObjectiveProgress()[index] = objProgress;
                        } else {
                            plugin.getLogger().warning(String.format(
                                    "Invalid objective index %d for quest %d (max: %d)",
                                    index, questId, progress.getObjectiveProgress().length - 1
                            ));
                        }
                    }
                }
                questMap.put(questId, progress);
            }
            plugin.getLogger().info(String.format("Loaded %d quests for player %s", questMap.size(), playerUUID));
            return questMap;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading quests for player " + playerUUID, e);
            return questMap;
        }
    }
    protected String getPlayerLanguage(UUID playerUUID) {
        Connection conn = this.dbConfig.getConnection();
        if(conn == null) {
            plugin.getLogger().severe("Cannot get nor create player settings - database not connected!");
            return "es";
        }
        try {
            String selectSql = "SELECT language FROM player_settings WHERE player_uuid = ?";
            try(PreparedStatement statement = conn.prepareStatement(selectSql)) {
                statement.setString(1, playerUUID.toString());
                try(ResultSet rs = statement.executeQuery()) {
                    if(rs.next()) {
                        updateLastLogin(playerUUID);
                        return rs.getString("language");
                    }
                }
            }
            String insertSql = "INSERT INTO player_settings (player_uuid, language, created_at, last_login) VALUES (?, 'es', ?, ?)";
            long now = System.currentTimeMillis();
            try(PreparedStatement statement = conn.prepareStatement(insertSql)) {
                statement.setString(1, playerUUID.toString());
                statement.setLong(2, now);
                statement.setLong(3, now);
                statement.executeUpdate();
            }
            plugin.getLogger().info("Created default settings for player: " + playerUUID);
            return "es";
        }catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting or creating player settings for player " + playerUUID.toString(), e);
            return "es";
        }
    }
    protected boolean setPlayerLanguage(UUID playerUUID, String newLanguage) {
        Connection conn = this.dbConfig.getConnection();
        if(conn == null) {
            plugin.getLogger().severe("Cannot get nor create player settings - database not connected!");
            return false;
        }
        String sql = "UPDATE player_settings SET language = ? WHERE player_uuid = ?";
        try(PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, newLanguage);
            statement.setString(2, playerUUID.toString());
            int rowsAffected = statement.executeUpdate();
            if(rowsAffected == 0) {
                plugin.getLogger().severe("Failed to update language for " + playerUUID.toString() + ". Player setting not found");
                return false;
            }
            plugin.getLogger().info(String.format("Update language for %s: %s", playerUUID, newLanguage));
            return true;
        }catch(SQLException e) {
            plugin.getLogger().info("Error setting new language for player " + playerUUID);
            return false;
        }
    }
    private void updateLastLogin(UUID playerUUID) {
        Connection conn = this.dbConfig.getConnection();
        if(conn == null) {
            plugin.getLogger().severe("Can not update last player login - database not connected!");
            return;
        }
            String sql = "UPDATE player_settings SET last_login = ? WHERE player_uuid = ?";
        try(PreparedStatement statement = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            statement.setLong(1, now);
            statement.setString(2, playerUUID.toString());
            statement.executeUpdate();
        }catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update last login for player " + playerUUID, e);
        }
    }
    private static class QuestProgressData {
        int questId;
        String status;
        long pickedUpAt;
        long completedAt;
        long lastCompleted;
        long nextAvailable;
    }
}
