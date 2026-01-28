package github.kakarot.Quests.Managers;

import github.kakarot.Quests.Models.PlayerQuestProgress;
import github.kakarot.Quests.Models.QuestObjective;
import github.kakarot.Quests.Models.QuestStatus;
import github.kakarot.Quests.Quest;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

//Call ONLY from async tasks
public class QuestDB {
    public static void addQuest(UUID playerUUID, int questId) {
        String sql = "INSERT OR IGNORE INTO player_quests " +
                "(player_uuid, quest_id, status, picked_up_at) VALUES (?, ?, ?, ?)";
        try(Connection connection = QuestDBConfig.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            preparedStatement.setInt(2, questId);
            preparedStatement.setString(3, QuestStatus.IN_PROGRESS.toString());
            preparedStatement.setLong(4, System.currentTimeMillis());
            preparedStatement.execute();
            Bukkit.getLogger().info("[Kakarot Quests] Added quest " + questId + " to " + playerUUID);
        }catch(SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Could not add quest.", e);
        }
    }
    public static Map<Integer, PlayerQuestProgress> getPlayerQuests(UUID playerUUID) {
        Map<Integer, PlayerQuestProgress> quests = new HashMap<>();
        String sql = "SELECT quest_id, status, picked_up_at, completed_at, last_completed, next_available FROM player_quests WHERE player_uuid = ?";
        try(Connection connection = QuestDBConfig.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int questId = resultSet.getInt("quest_id");
                Quest questDef = QuestManager.getQuest(questId);
                if(questDef == null) {
                    Bukkit.getLogger().warning("[Kakarot Quests] Quest " + questId + " not found in database!");
                    continue;
                }
                PlayerQuestProgress progress = new PlayerQuestProgress(playerUUID, questId, questDef);
                progress.setStatus(QuestStatus.valueOf(resultSet.getString("status")));
                progress.setPickedUpAt(resultSet.getLong("picked_up_at"));
                progress.setCompletedAt(resultSet.getLong("completed_at"));
                progress.setLastCompleted(resultSet.getLong("last_completed"));
                progress.setNextAvailable(resultSet.getLong("next_available"));
                loadObjectiveProgress(playerUUID, questId, progress.getObjectiveProgress());
                quests.put(questId, progress);
            }
        }catch(SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not fetch player quests", e);
        }
        return quests;
    }
    public static PlayerQuestProgress getQuestProgress(UUID playerUUID, int questId) {
        String sql = "SELECT status, picked_up_at, completed_at, last_completed, next_available " +
                "FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
        try(Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            preparedStatement.setInt(2, questId);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                Quest questDef = QuestManager.getQuest(questId);
                if (questDef == null) return null;
                PlayerQuestProgress progress = new PlayerQuestProgress(playerUUID, questId, questDef);
                progress.setStatus(QuestStatus.valueOf(rs.getString("status")));
                progress.setPickedUpAt(rs.getLong("picked_up_at"));
                progress.setCompletedAt(rs.getLong("completed_at"));
                progress.setLastCompleted(rs.getLong("last_completed"));
                progress.setNextAvailable(rs.getLong("next_available"));
                loadObjectiveProgress(playerUUID, questId, progress.getObjectiveProgress());
                return progress;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error getting quest progress" + e);
        }
        return null;
    }
    public static void completeQuest(UUID playerUUID, int questId, long repeatCooldown) {
        long now = System.currentTimeMillis();
        long nextAvailable = now + repeatCooldown;
        String sql = "UPDATE player_quests SET status = ?, completed_at = ?, last_completed = ?, next_available = ? " +
                "WHERE player_uuid = ? AND quest_id = ?";
        try(Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, QuestStatus.COMPLETED.toString());
            preparedStatement.setLong(2, now);
            preparedStatement.setLong(3, now);
            preparedStatement.setLong(4, nextAvailable);
            preparedStatement.setString(5, playerUUID.toString());
            preparedStatement.setInt(6, questId);
            preparedStatement.executeUpdate();
            Bukkit.getLogger().info("[Kakarot Quests] Completed quest " + questId + " for " + playerUUID);
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error completing quest" + e);
        }
    }
    public static void initializeQuestObjectives(UUID playerUUID, int questId, Quest quest) {
        String sql = "INSERT OR IGNORE INTO player_quest_objectives " +
                "(player_uuid, quest_id, objective_index, progress, max_progress) VALUES (?, ?, ?, 0, ?)";
        try(Connection connection = QuestDBConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                preparedStatement.setString(1, playerUUID.toString());
                preparedStatement.setInt(2, questId);
                preparedStatement.setInt(3, i);
                preparedStatement.setInt(4, obj.getRequired());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error initializing quest objectives" + e);
        }
    }
    public static void updateObjectiveProgress(UUID playerUUID, int questId, int objectiveIndex, int newProgress) {
        String sql = "UPDATE player_quest_objectives SET progress = ? " +
                "WHERE player_uuid = ? AND quest_id = ? AND objective_index = ?";
        try(Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, newProgress);
            preparedStatement.setString(2, playerUUID.toString());
            preparedStatement.setInt(3, questId);
            preparedStatement.setInt(4, objectiveIndex);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error updating objective progress" + e);
        }
    }
    public static void resetQuestObjectives(UUID playerUUID, int questId) {
        String sql = "UPDATE player_quest_objectives SET progress = 0 " +
                "WHERE player_uuid = ? AND quest_id = ?";
        try (Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            preparedStatement.setInt(2, questId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error resetting quest objectives" + e);
        }
    }
    public static void setTrackedQuest(UUID playerUUID, int questId) {
        String sql = "INSERT OR REPLACE INTO tracked_quests (player_uuid, quest_id) VALUES (?, ?)";
        try (Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            preparedStatement.setInt(2, questId == -1 ? 0 : questId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error setting tracked quest" + e);
        }
    }
    public static int getTrackedQuest(UUID playerUUID) {
        String sql = "SELECT quest_id FROM tracked_quests WHERE player_uuid = ?";
        try (Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                int questId = rs.getInt("quest_id");
                return questId == 0 ? -1 : questId;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error getting tracked quest" + e);
        }
        return -1;
    }
    public static boolean hasCompletedQuest(UUID playerUUID, int questId) {
        String sql = "SELECT status FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
        try (Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            preparedStatement.setInt(2, questId);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                QuestStatus status = QuestStatus.valueOf(rs.getString("status"));
                return status == QuestStatus.COMPLETED;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error checking quest completion: " + e);
        }
        return false;
    }
    public static boolean hasPickedUpQuest(UUID playerUUID, int questId) {
        String sql = "SELECT id FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
        try (Connection connection = QuestDBConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            preparedStatement.setInt(2, questId);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Error checking quest pickup" + e);
        }
        return false;
    }
    private static void loadObjectiveProgress(UUID playerUUID, int questId, int[] progressArray) {
        String sql = "SELECT objective_index, progress FROM player_quest_objectives WHERE player_uuid = ? AND quest_id = ? ORDER BY objective_index ASC";
        try(Connection connection = QuestDBConfig.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID.toString());
            preparedStatement.setString(2, String.valueOf(questId));
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int index = resultSet.getInt("objective_index");
                int progress = resultSet.getInt("progress");
                if(index < progressArray.length) progressArray[index] = progress;
            }
        }catch(SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Kakarot Quests] Could not load objectives for quest " + questId);
        }
    }
}
