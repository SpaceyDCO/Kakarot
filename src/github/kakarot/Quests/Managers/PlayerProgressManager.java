package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import github.kakarot.Quests.Models.PlayerQuestProgress;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProgressManager {
    private final Map<UUID, Map<Integer, PlayerQuestProgress>> playerProgressMap = new HashMap<>();
    private final Main plugin;
    public PlayerProgressManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads a player's progress from database to cache
     * Safe to call on main thread
     * @param playerUUID The player's uuid whose progress will be loaded to cache
     */
    public void loadPlayerProgress(UUID playerUUID) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Integer, PlayerQuestProgress> progress = QuestDB.getPlayerQuests(this.plugin, playerUUID);
            this.playerProgressMap.put(playerUUID, progress);
            plugin.getLogger().info("Loaded quest progress for player " + Bukkit.getPlayer(playerUUID).getName() + ".\n" + progress.size() + " progressed quests loaded.");
        });
    }

    /**
     * Saves a player's progress to database and removes it from cache
     * Currently only removes from cache as database is updated on the go
     * Meant to be called when a player quits the server
     * Safe to call on main thread
     * @param playerUUID The player's uuid whose progress will be "moved" to database
     */
    public void savePlayerProgress(UUID playerUUID) {
        this.playerProgressMap.remove(playerUUID);
    }
}
