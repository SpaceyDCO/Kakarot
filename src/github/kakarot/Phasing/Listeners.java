package github.kakarot.Phasing;

import com.spacey.kakarotmod.api.KakarotModAPI;
import github.kakarot.Main;
import lombok.AllArgsConstructor;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@AllArgsConstructor
public class Listeners implements Listener {
    private final Main plugin;
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        KakarotModAPI.syncHiddenNPCs(event.getPlayer().getName(), new String[]{"Vegeta|Prince"});
        this.plugin.getLogger().info("Hiding Vegeta NPC for player " + event.getPlayer().getName());
    }
    public void onNPCInteract(ICustomNpc<?> npc, IPlayer<?> iPlayer) {
        //TODO: Add event cancel if npc is invisible to player
    }
}
