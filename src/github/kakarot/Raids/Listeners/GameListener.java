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
        NpcAPI api = (NpcAPI) NpcAPI.Instance();
        if(api != null) {
            api.events().register(this);
        }else {
            plugin.getLogger().severe("CustomNpcs+ API not found. Raids won't work properly.");
        }
    }

    public void onNpcDied(NpcEvent.DiedEvent event) {
        int npcId = event.getNpc().getEntityId(); //Change to getMCEntity().getEntityID later
        //SELF NOTE: I COULD BE ABLE TO OPTIMIZE THIS FOR BY ADDING A TEMPDATA LINKING EACH SPAWNED NPC TO THE ARENA'S NAME THEY WERE SUPPOSED TO BE SPAWNED IN
        for(GameSession session : raidManager.getAllActiveSessions()) { //Might lag if there are hundreds of sessions
            if(session.getAliveNpcs().contains(npcId)) {
                session.onNpcDied(npcId);
                break;
            }
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
