package github.kakarot.Phasing.Listeners;

import com.spacey.kakarotmod.api.KakarotModAPI;
import github.kakarot.Main;
import github.kakarot.Phasing.Cache.PhasingCache;
import github.kakarot.Phasing.Models.PhasedNPC;
import lombok.AllArgsConstructor;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.IPlayerEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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
    //True if player can interact (has permissions or NPC is not in phasing system)
    //False otherwise
    public boolean checkPhasingNPC(IPlayerEvent.InteractEvent event) {
        ICustomNpc<?> npc = (ICustomNpc<?>) event.getTarget();
        IPlayer<?> iPlayer = event.getPlayer();
        String npcName = npc.getName().trim();
        String npcTitle = (npc.getTitle() != null) ? npc.getTitle().trim() : "";
        String id = npcName + "|" + npcTitle;
        Player player = Bukkit.getPlayer(UUID.fromString(iPlayer.getUniqueID()));
        String permission = this.plugin.getPhasingConfigManager().reverseIndexNpc.get(id);
        if(permission != null) return player.hasPermission(permission);
        return true;
    }
}
