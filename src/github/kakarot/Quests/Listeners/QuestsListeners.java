package github.kakarot.Quests.Listeners;

import github.kakarot.Main;
import github.kakarot.Quests.Managers.PlayerProgressManager;
import github.kakarot.Quests.Managers.QuestManager;
import github.kakarot.Quests.Models.ObjectiveType;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
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
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId());
        if(player == null) return;
        progressManager.savePlayerProgress(player.getUniqueId());
    }

    public void onNpcDied(ICustomNpc<?> npc, IEntity<?> killer) {
        if(npc == null || killer == null) {
            plugin.getLogger().warning("Critical ERROR: NPC or Player killer with null value");
            return;
        }
        if(!(killer instanceof IPlayer<?>)) return;
        IPlayer<?> iPlayer = (IPlayer<?>) killer;
        Player player = Bukkit.getPlayer(UUID.fromString(iPlayer.getUniqueID()));
        List<QuestManager.QuestObjectiveReference> references = plugin.getQuestManager().npcObjectives.getOrDefault(npc.getName(), new ArrayList<>());
        checkReferenceAndProgressObjective(references, player, npc, ObjectiveType.KILL_MOBS, 1);
    }
    public void onPlayerNpcInteract(ICustomNpc<?> npc, IPlayer<?> iPlayer) {
        if(npc == null || iPlayer == null) {
            plugin.getLogger().warning("Critical ERROR: Npc or Player interact with null value");
            return;
        }
        Player player = Bukkit.getPlayer(UUID.fromString(iPlayer.getUniqueID()));
        List<QuestManager.QuestObjectiveReference> references = plugin.getQuestManager().npcObjectives.getOrDefault(npc.getName(), new ArrayList<>());
        checkReferenceAndProgressObjective(references, player, npc, ObjectiveType.TALK_TO_NPC, 1);
    }

    /**
     * Using reverse index, checks if the given NPC is linked to a quest, then progresses the player by amount
     * @param references The list of QuestObjectiveReference to do the reverse index search in
     * @param player The player doing the action (killing or interacting with an npc)
     * @param npc The npc being actioned on
     * @param amount The amount of objective value to progress
     */
    private void checkReferenceAndProgressObjective(List<QuestManager.QuestObjectiveReference> references, Player player, ICustomNpc<?> npc, ObjectiveType ACTION, int amount) {
        if(references.isEmpty()) return;
        UUID playerUUID = player.getUniqueId();
        for(QuestManager.QuestObjectiveReference reference : references) {
            if(reference.getType() != ACTION) continue;
            int questId = reference.getQuestId();
            if(plugin.getQuestManager().hasPickedUpQuest(playerUUID, questId)) {
                if(plugin.getQuestManager().hasCompletedQuest(playerUUID, reference.getQuestId()) || plugin.getQuestManager().hasCompletedObjective(playerUUID, reference.getQuestId(), reference.getObjectiveIndex())) continue;
                Location completionLoc = new Location(player.getWorld(), npc.getX(), npc.getY(), npc.getZ());
                if(!reference.getTitle().isEmpty() && npc.getTitle().equals(reference.getTitle())) {
                    plugin.getQuestManager().progressObjective(playerUUID, reference.getQuestId(), reference.getObjectiveIndex(), completionLoc, amount);
                }else if(reference.getTitle().isEmpty()) {
                    plugin.getQuestManager().progressObjective(playerUUID, reference.getQuestId(), reference.getObjectiveIndex(), completionLoc, amount);
                }
            }
        }
    }
}
