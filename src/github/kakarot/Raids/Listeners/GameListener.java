package github.kakarot.Raids.Listeners;

import github.kakarot.Main;
import github.kakarot.Raids.Game.GameSession;
import github.kakarot.Raids.Managers.RaidManager;
import noppes.npcs.scripted.NpcAPI;
import noppes.npcs.scripted.event.NpcEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class GameListener implements Listener {
    private final Main plugin;
    private final RaidManager raidManager;
    public GameListener(Main plugin, RaidManager raidManager) {
        this.plugin = plugin;
        this.raidManager = raidManager;
    }

    public void onNpcDied(NpcEvent.DiedEvent event) {
        if(event == null) {
            plugin.getLogger().severe("===== CUSTOMNPC EVENT ERROR =====");
            plugin.getLogger().severe("'event' parameter is null, there might be an error with an npc's script.");
            return;
        }
        if(event.getNpc().hasStoredData("SPACEY_ARENA_SYSTEM_NPC")) {
            int npcId = event.getNpc().getEntityId();
            String arenaName = (String) event.getNpc().getStoredData("SPACEY_ARENA_SYSTEM_NPC");
            Optional<GameSession> session = raidManager.getSessionByArena(arenaName);
            session.ifPresent(gameSession -> gameSession.onNpcDied(npcId));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Optional<GameSession> sessionOptional = raidManager.getSessionByPlayer(player.getUniqueId());
        sessionOptional.ifPresent(session -> session.onPlayerDied(player));
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Optional<GameSession> sessionOptional = raidManager.getSessionByPlayer(player.getUniqueId());
        sessionOptional.ifPresent(session -> session.onPlayerQuit(player));
    }
}
