package github.kakarot.Raids.Listeners;

import github.kakarot.Main;
import github.kakarot.Parties.Events.PlayerLeavePartyEvent;
import github.kakarot.Raids.Game.GameSession;
import github.kakarot.Raids.Managers.RaidManager;
import github.kakarot.Tools.CC;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.scripted.event.NpcEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GameListener implements Listener {
    private final Main plugin;
    private final RaidManager raidManager;
    private final List<String> allowedCommands = Arrays.asList("/msg", "/r", "/party", "/recargar", "/login");
    public GameListener(Main plugin, RaidManager raidManager) {
        this.plugin = plugin;
        this.raidManager = raidManager;
    }

    public void onNpcDamaged(NpcEvent.DamagedEvent event) {
        if(event == null) {
            plugin.getLogger().severe("===== CUSTOMNPC EVENT ERROR =====");
            plugin.getLogger().severe("'event' parameter is null, there might be an error with an npc's script.");
            return;
        }
        if(event.getNpc().hasStoredData("SPACEY_ARENA_SYSTEM_NPC")) {
            String arenaName = (String) event.getNpc().getStoredData("SPACEY_ARENA_SYSTEM_NPC");
            Optional<GameSession> session = raidManager.getSessionByArena(arenaName);
            session.ifPresent(gameSession -> gameSession.onNpcDamaged(event));
        }
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
    public void npcDied(ICustomNpc<?> npc) {
        if(npc.hasStoredData("SPACEY_ARENA_SYSTEM_NPC")) {
            int npcId = npc.getEntityId();
            String arenaName = (String) npc.getStoredData("SPACEY_ARENA_SYSTEM_NPC");
            Optional<GameSession> session = raidManager.getSessionByArena(arenaName);
            session.ifPresent(session1 -> session1.onNpcDied(npcId));
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
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Optional<GameSession> sessionOptional = raidManager.getSessionByPlayer(player.getUniqueId());
        if(sessionOptional.isPresent()) {
            String command = event.getMessage().toLowerCase().split(" ")[0];
            if(player.isOp()) return;
            if(!allowedCommands.contains(command)) {
                event.setCancelled(true);
                player.sendMessage(CC.translate(RaidManager.RAID_PREFIX + " &4You can't use commands while in-game."));
            }
        }
    }
    @EventHandler
    public void onPartyLeave(PlayerLeavePartyEvent event) {
        Player player = event.getPlayer();
        Optional<GameSession> sessionOptional = raidManager.getSessionByPlayer(player.getUniqueId());
        sessionOptional.ifPresent(session -> session.onPlayerLeftParty(player));
    }
}
