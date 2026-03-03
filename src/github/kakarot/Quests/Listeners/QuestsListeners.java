package github.kakarot.Quests.Listeners;

import github.kakarot.Main;
import github.kakarot.Quests.Managers.PlayerProgressManager;
import github.kakarot.Quests.Managers.QuestManager;
import github.kakarot.Quests.Models.ObjectiveType;
import github.kakarot.Quests.Models.PlayerQuestProgress;
import github.kakarot.Quests.Models.QuestObjective;
import github.kakarot.Quests.Models.QuestStatus;
import github.kakarot.Quests.Quest;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestsListeners implements Listener {
    private final Main plugin;
    public QuestsListeners(Main plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getProgressManager().loadPlayerProgress(event.getPlayer().getUniqueId());
        this.plugin.getSettingsManager().loadPlayerSettings(event.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.getProgressManager().savePlayerProgress(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        this.plugin.getSettingsManager().savePlayerSettings(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        checkCollectItemsObjectives(player);
    }
    @EventHandler
    public void onInventoryChange(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if(event.getCurrentItem() == null && event.getCursor() == null) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkCollectItemsObjectives(player);
        }, 1L);
    }
    public void forceInventoryCheck(IEntity<?> killer) {
        if(killer == null) return;
        if(!(killer instanceof IPlayer<?>)) return;
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            IPlayer<?> iPlayer = (IPlayer<?>) killer;
            checkCollectItemsObjectives(Bukkit.getPlayer(UUID.fromString(iPlayer.getUniqueID())));
        }, 3L);
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
        checkTurnInReferences(plugin.getQuestManager().npcsTurnIn.getOrDefault(npc.getName(), new ArrayList<>()), player, npc);
    }

    /**
     * Using reverse index, checks if the given NPC is linked to a quest, then progresses the player by amount
     * @param references The list of QuestObjectiveReference to do the reverse index search in
     * @param player The player doing the action (killing or interacting with a npc)
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
                if(reference.getTitle() != null) {
                    if(npc.getTitle().equals(reference.getTitle())) plugin.getQuestManager().progressObjective(playerUUID, reference.getQuestId(), reference.getObjectiveIndex(), completionLoc, amount);
                    return;
                }
                plugin.getQuestManager().progressObjective(playerUUID, reference.getQuestId(), reference.getObjectiveIndex(), completionLoc, amount);
            }
        }
    }
    private void checkTurnInReferences(List<QuestManager.QuestTurnInReference> references, Player player, ICustomNpc<?> npc) {
        if(references.isEmpty()) return;
        UUID playerUUID = player.getUniqueId();
        String npcTitle = npc.getTitle();
        for(QuestManager.QuestTurnInReference reference : references) {
            if(reference.getTitle() != null && !reference.getTitle().isEmpty()) {
                if(!npcTitle.equals(reference.getTitle())) continue;
            }
            if(plugin.getQuestManager().hasCompletedQuest(playerUUID, reference.getQuestId())) continue;
            if(!plugin.getQuestManager().hasPickedUpQuest(playerUUID, reference.getQuestId())) continue;
            PlayerQuestProgress progress = plugin.getQuestManager().getPlayerQuestProgress(playerUUID, reference.getQuestId());
            if(progress == null) continue;
            Quest quest = plugin.getQuestManager().getQuest(reference.getQuestId());
            if(quest == null) continue;
            int totalProgress = progress.getTotalProgressPercentage(plugin.getQuestManager().getQuest(reference.getQuestId()));
            if(totalProgress < 100) continue; //Can't turn in, not all objectives completed
            //Player has passed the checks, give reward
            if(plugin.getQuestManager().hasRequiredItems(player, reference.getQuestId())) {
                plugin.getQuestManager().completeQuest(playerUUID, reference.getQuestId());
            }
        }
    }
    private void checkCollectItemsObjectives(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<Integer, PlayerQuestProgress> progress = plugin.getQuestManager().getPlayerQuests(playerUUID);
        if(progress == null || progress.isEmpty()) return;
        for(Map.Entry<Integer, PlayerQuestProgress> progressEntry : progress.entrySet()) {
            int questId = progressEntry.getKey();
            PlayerQuestProgress playerQuestProgress = progressEntry.getValue();
            if(playerQuestProgress.getStatus() != QuestStatus.IN_PROGRESS) continue;
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if(quest == null) continue;
            for(int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective objective = quest.getObjectives().get(i);
                if(objective.getType() != ObjectiveType.COLLECT_ITEMS) continue;
                if(plugin.getQuestManager().hasCompletedObjective(playerUUID, questId, i)) continue;
                int finalI = i;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if(plugin.getProgressManager().playerHasSingleRequiredItem(player, objective)) {
                        int actualCount = plugin.getProgressManager().getRequiredItemInventory(player, objective);
                        int currentProgress = playerQuestProgress.getObjectiveProgress()[finalI];
                        if(actualCount <= currentProgress) return;
                        int progressToAdd = actualCount - currentProgress;
                        plugin.getQuestManager().progressObjective(playerUUID, questId, finalI, player.getLocation(), progressToAdd);
                    }
                }, 1L);
            }
        }
    }
}
