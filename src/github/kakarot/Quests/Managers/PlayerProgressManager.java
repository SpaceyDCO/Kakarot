package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import github.kakarot.Quests.Models.ObjectiveInfo;
import github.kakarot.Quests.Models.PlayerQuestProgress;
import github.kakarot.Quests.Models.QuestObjective;
import github.kakarot.Tools.NbtHandler;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProgressManager {
    private final Map<UUID, Map<Integer, PlayerQuestProgress>> playerProgressMap = new HashMap<>();
    private final Main plugin;
    public PlayerProgressManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads a player's progress from database to cache
     * Safe to call on main thread
     * @param playerUUID The player's uuid whose progress will be loaded to cache
     */
    public void loadPlayerProgress(UUID playerUUID) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Integer, PlayerQuestProgress> progress = QuestDB.getPlayerQuests(this.plugin, playerUUID);
            this.playerProgressMap.put(playerUUID, progress);
            plugin.getLogger().info("Loaded quest progress for player " + Bukkit.getPlayer(playerUUID).getName() + ".\n" + progress.size() + " progressed quests loaded.");
        });
    }

    /**
     * Saves a player's progress to database and removes it from cache
     * Currently only removes from cache as database is updated on the go
     * Meant to be called when a player quits the server
     * Safe to call on main thread
     * @param playerUUID The player's uuid whose progress will be "moved" to database
     */
    public void savePlayerProgress(UUID playerUUID) {
        this.playerProgressMap.remove(playerUUID);
    }
    public boolean playerHasRequiredItem(Player player, QuestObjective objective) {
        ObjectiveInfo info = objective.getObjectiveInfo();
        int itemId = info.getItemId();
        byte dataValue = info.getDataValue();
        NBTTagCompound requiredCompound = info.getParsedNbt() != null ? info.getParsedNbt() : null;
        int count = 0;
        for(ItemStack item : player.getInventory().getContents()) {
            if(item == null || item.getTypeId() != itemId) continue;
            if(item.getData().getData() != dataValue) continue;
            if(requiredCompound != null) {
                if(!itemMatchesNbt(item, requiredCompound)) continue;
            }
            count += item.getAmount();
            if(count >= objective.getRequired()) {
                return true;
            }
        }
        return false;
    }
    private boolean itemMatchesNbt(ItemStack item, NBTTagCompound requiredNbt) {
        NbtHandler handler = new NbtHandler(item);
        if(!handler.hasNBT() && requiredNbt != null) return false; //Item HAS NOT NBT, but the objective requires NBT
        return nbtContainsAllTags(handler.getCompound(), requiredNbt);
    }
    private boolean nbtContainsAllTags(NBTTagCompound actual, NBTTagCompound required) {
        if(actual == null || required == null) return false; //Just in case
        for(Object keyObj : required.func_150296_c()) {
            String key = (String) keyObj;
            if(!actual.hasKey(key)) return false;
            NBTBase requiredTag = required.getTag(key);
            NBTBase actualTag = actual.getTag(key);
            if(requiredTag instanceof NBTTagCompound && actualTag instanceof NBTTagCompound) {
                if(!nbtContainsAllTags((NBTTagCompound) actualTag, (NBTTagCompound) requiredTag)) return false;
            }else if(requiredTag instanceof NBTTagList && actualTag instanceof NBTTagList) {
                if(!nbtListMatches((NBTTagList) actualTag, (NBTTagList) requiredTag)) return false;
            }else {
                if(!requiredTag.equals(actualTag)) return false;
            }
        }
        return true;
    }
    private boolean nbtListMatches(NBTTagList actual, NBTTagList required) {
        if (actual.tagCount() < required.tagCount()) return false;
        int tagType = required.func_150303_d();
        // Handle compound lists (enchantments)
        if (tagType == 10) {
            for (int i = 0; i < required.tagCount(); i++) {
                NBTTagCompound requiredElement = required.getCompoundTagAt(i);

                boolean found = false;
                for (int j = 0; j < actual.tagCount(); j++) {
                    NBTTagCompound actualElement = actual.getCompoundTagAt(j);
                    if (nbtContainsAllTags(actualElement, requiredElement)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
        // Handle string lists (Lore)
        else if (tagType == 8) {
            for (int i = 0; i < required.tagCount(); i++) {
                String requiredStr = required.getStringTagAt(i);
                boolean found = false;
                for (int j = 0; j < actual.tagCount(); j++) {
                    String actualStr = actual.getStringTagAt(j);

                    if (requiredStr.equals(actualStr)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
        else {
            // For safety, do string comparison on list elements
            for (int i = 0; i < required.tagCount(); i++) {
                String reqStr = required.toString();
                String actStr = actual.toString();
                if (!actStr.contains(reqStr)) return false;
            }
            return true;
        }
    }
}
