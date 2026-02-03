package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class SettingsManager {
    private final Map<UUID, String> playerLanguage = new HashMap<>();
    private final Main plugin;
    public SettingsManager(Main plugin) {
        this.plugin = plugin;
    }
    public void loadPlayerSettings(UUID playerUUID) {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            String playerLanguage = plugin.getQuestDBManager().getPlayerLanguage(playerUUID);
            this.playerLanguage.put(playerUUID, playerLanguage);
        }, 2L);
    }
    public void setPlayerLanguage(UUID playerUUID, String newLanguage) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
           if(!this.plugin.getQuestDBManager().setPlayerLanguage(playerUUID, newLanguage)) plugin.getLogger().severe("Could not update language...");
        });
    }
}
