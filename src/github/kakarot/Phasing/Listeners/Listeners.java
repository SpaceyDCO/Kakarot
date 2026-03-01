package github.kakarot.Phasing.Listeners;

import com.spacey.kakarotmod.api.KakarotModAPI;
import github.kakarot.Main;
import github.kakarot.Phasing.Cache.PhasingCache;
import github.kakarot.Phasing.Models.PhasedNPC;
import lombok.AllArgsConstructor;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Map;

@AllArgsConstructor
public class Listeners implements Listener {
    private final Main plugin;
    @EventHandler
    //TODO: Optimize using LuckPerms hasPermission IF there will be hundreds of NPCs
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ArrayList<String> hiddenNPCs = new ArrayList<>();
            for(Map.Entry<String, PhasedNPC> entry : PhasingCache.getAll().entrySet()) {
                PhasedNPC phasedNPC = entry.getValue();
                String identifier = phasedNPC.getIdentifier();
                String permission = phasedNPC.getPermission();
                if(!player.hasPermission(permission)) hiddenNPCs.add(identifier);
            }
            KakarotModAPI.syncHiddenNPCs(player.getName(), hiddenNPCs.toArray(new String[0]));
            plugin.getLogger().info("Hiding " + hiddenNPCs.size() + " npcs for player: " + player.getName());
        }, 5L);
    }
    public void onNPCInteract(ICustomNpc<?> npc, IPlayer<?> iPlayer) {
        //TODO: Add event cancel if npc is invisible to player
    }
}
