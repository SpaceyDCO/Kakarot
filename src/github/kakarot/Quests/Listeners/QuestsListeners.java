package github.kakarot.Quests.Listeners;

import github.kakarot.Main;
import github.kakarot.Quests.Managers.PlayerProgressManager;
import github.kakarot.Quests.Managers.QuestManager;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestsListeners implements Listener {
    private final PlayerProgressManager progressManager;
    private final Main plugin;
    public QuestsListeners(Main plugin) {
        this.plugin = plugin;
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
    public void onNpcDied(ICustomNpc<?> npc, IEntity<?> killer) {
        if(npc == null || killer == null) {
            plugin.getLogger().warning("Critical ERROR: NPC or Player killer with null value");
            return;
        }
        if(!(killer instanceof IPlayer<?>)) return;
        IPlayer<?> iPlayer = (IPlayer<?>) killer;
        Player player = Bukkit.getPlayer(UUID.fromString(iPlayer.getUniqueID()));
        List<QuestManager.QuestObjectiveReference> references = plugin.getQuestManager().npcKillObjectives.getOrDefault(npc.getName(), new ArrayList<>());
        if(references.isEmpty()) return;
        for(QuestManager.QuestObjectiveReference reference : references) {
            if(plugin.getQuestManager().hasPickedUpQuest(player.getUniqueId(), reference.getQuestId())) {
                if(!reference.getTitle().isEmpty() && npc.getTitle().equals(reference.getTitle())) {
                    if(!plugin.getQuestManager().hasCompletedQuest(player.getUniqueId(), reference.getQuestId())) plugin.getQuestManager().progressObjective(player.getUniqueId(), reference.getQuestId(), reference.getObjectiveIndex(), 1);
                }
                else if(reference.getTitle().isEmpty()) {
                    if(!plugin.getQuestManager().hasCompletedQuest(player.getUniqueId(), reference.getQuestId())) plugin.getQuestManager().progressObjective(player.getUniqueId(), reference.getQuestId(), reference.getObjectiveIndex(), 1);
                }
            }
        }
    }
}
