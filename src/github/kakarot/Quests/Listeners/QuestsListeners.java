package github.kakarot.Quests.Listeners;

import github.kakarot.Main;
import github.kakarot.Quests.Managers.PlayerProgressManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuestsListeners implements Listener {
    private final PlayerProgressManager progressManager;
    public QuestsListeners(Main plugin) {
        this.progressManager = plugin.getProgressManager();
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        //questManager.loadPlayerProgress(event.getPlayer().getUniqueId());
        //TODO: Load player progress on join
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        progressManager.savePlayerProgress(event.getPlayer().getUniqueId());
    }
}
