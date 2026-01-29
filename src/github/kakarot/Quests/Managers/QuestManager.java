package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import github.kakarot.Quests.Models.*;
import github.kakarot.Quests.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {
    private final Main plugin;
    //Quests cache loaded once on startup
    private final Map<Integer, Quest> quests = new HashMap<>();
    //Key: playerUUID, Value: map of questId -> progress ; useful for checking player progress quickly
    private final Map<String, Map<Integer, PlayerQuestProgress>> playerProgress = new HashMap<>();
    public QuestManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the quest manager, this is called on plugin startup and loads all quests from quests.yml
     */
    public void initialize() {
        plugin.getLogger().info("Initializing quest manager...");
        loadQuests();
        plugin.getLogger().info("Loaded " + quests.size() + " quests");
    }

    /**
     * Reload quests from YAML
     */
    public void reloadQuests() {
        quests.clear();
        loadQuests();
        plugin.getLogger().info("Reloaded quests!");
    }
    /**
     * Load all quests from quests/quests.yml
     */
    private void loadQuests() {
        try {
            File questFolder = new File(plugin.getDataFolder(), "quests");
            if(!questFolder.exists()) if(!questFolder.mkdirs()) plugin.getLogger().severe("Could not create folder \"quests\" in " + questFolder.getAbsolutePath());
            File questsFile = new File(questFolder, "quests.yml");
            if(!questsFile.exists()) createDefaultQuests(questsFile);
            FileConfiguration config = YamlConfiguration.loadConfiguration(questsFile);
            ConfigurationSection questsSection = config.getConfigurationSection("quests");
            if(questsSection == null) {
                plugin.getLogger().warning("No quests section found in quests.yml");
                return;
            }
            for(String questIdStr : questsSection.getKeys(false)) {
                try {
                    int questId = Integer.parseInt(questIdStr);
                    ConfigurationSection questSection = questsSection.getConfigurationSection(questIdStr);
                    if(questSection == null) continue;
                    Quest quest = loadQuestFromYAML(questId, questSection);
                    if(quest != null) quests.put(questId, quest);
                }catch(NumberFormatException e) {
                    plugin.getLogger().warning("Invalid quest ID: " + questIdStr);
                }
            }
        }catch(Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading quests.yml!", e);
        }
    }
    /**
     * Retrieves a single quest from YAML configuration
     */
    private Quest loadQuestFromYAML(int questId, ConfigurationSection questSection) {
        try {
            Map<String, String> names = new HashMap<>();
            ConfigurationSection nameSection = questSection.getConfigurationSection("name");
            if(nameSection != null) {
                for(String locale : nameSection.getKeys(false)) {
                    names.put(locale, nameSection.getString(locale).replace("&", "§"));
                }
            }
            if(names.isEmpty()) names.put("es", "Misión #" + questId);
            Map<String, String> descriptions = new HashMap<>();
            ConfigurationSection descriptionSection = questSection.getConfigurationSection("description");
            if(descriptionSection != null) {
                for(String locale : descriptionSection.getKeys(false)) {
                    descriptions.put(locale, descriptionSection.getString(locale).replace("&", "§"));
                }
            }
            if(descriptions.isEmpty()) descriptions.put("es", "Sin descripción");
            List<QuestObjective> objectives = new ArrayList<>();
            List<Map<?, ?>> objectivesList = questSection.getMapList("objectives");
            for(Map<?, ?> objMap : objectivesList) {
                QuestObjective obj = parseObjective(objMap);
                if(obj != null) objectives.add(obj);
            }
            List<QuestReward> rewards = new ArrayList<>();
            List<Map<?, ?>> rewardsList = questSection.getMapList("rewards");
            for(Map<?, ?> rewMap : rewardsList) {
                QuestReward reward = parseReward(rewMap);
                if(reward != null) rewards.add(reward);
            }
            Map<String, String> completionMessages = new HashMap<>();
            ConfigurationSection completionMessageSection = questSection.getConfigurationSection("completionMessage");
            if(completionMessageSection != null) {
                for(String locale : completionMessageSection.getKeys(false)) {
                    completionMessages.put(locale, completionMessageSection.getString(locale));
                }
            }
            boolean repeatable = questSection.getBoolean("repeatable", false);
            long repeatCooldown = questSection.getLong("repeatCooldown", 86400000);
            int npcId = questSection.getInt("npcId", -1);
            Quest quest = new Quest(
                    questId,
                    names,
                    descriptions,
                    objectives,
                    rewards,
                    completionMessages,
                    repeatable,
                    repeatCooldown,
                    npcId
            );
            plugin.getLogger().info("Loaded quest #" + questId + ": "
            + names.getOrDefault("es", "?"));
            return quest;
        }catch(Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error loading quest #" + questId, e);
            return null;
        }
    }

    /**
     * Parses a single objective
     */
    private QuestObjective parseObjective(Map<?, ?> map) {
        try {
            String typeStr = (String) map.get("type");
            String target = (String) map.get("target");
            int required;
            if(map.get("required") != null) required = ((Number) map.get("required")).intValue();
            else required = 1;
            boolean shareable;
            if(map.get("shareable") != null) shareable = (Boolean) map.get("shareable");
            else shareable = false;
            ObjectiveType type = ObjectiveType.valueOf(typeStr.toUpperCase());
            return new QuestObjective(type, target, required, shareable);
        }catch(Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing objective: " + e);
            return null;
        }
    }

    /**
     * Parses a single reward
     */
    private QuestReward parseReward(Map<?, ?> map) {
        try {
            String typeStr = (String) map.get("type");
            String value = (String) map.get("value");
            Object descObj = map.get("descriptions");
            Map<String, String> localizedDescriptions = new HashMap<>();
            if(descObj instanceof Map<?, ?>) {
                Map<?, ?> descMap = (Map<?, ?>) descObj;
                for(Map.Entry<?, ?> descEntry : descMap.entrySet()) {
                    localizedDescriptions.put(descEntry.getKey().toString(), descEntry.getValue().toString());
                }
            }
            RewardType type = RewardType.valueOf(typeStr.toUpperCase());
            return new QuestReward(type, value, localizedDescriptions);
        }catch(Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing quest reward: ", e);
            return null;
        }
    }
    private void createDefaultQuests(File destination) {
        try {
            if(!destination.exists()) {
                Files.copy(plugin.getResource("quests/quests.yml"), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created default config: " + destination.getName());
            }
        }catch(Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save default resource: " + "quests/quests.yml" + ".", e);
        }
    }
    public Quest getQuest(int questId) {
        return this.quests.get(questId);
    }
    public Map<Integer, Quest> getAllQuests() {
        return new HashMap<>(this.quests);
    }
    public void addQuestToPlayer(UUID playerUUID, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) {
            plugin.getLogger().severe("Could not fetch quest with ID " + questId);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            //QuestDB.addQuest(playerUUID, questId);
            // QuestDB.initializeQuestObjectives(playerUUID, questId, quest);
            // TODO: Refactor database
           if(!playerProgress.containsKey(playerUUID.toString())) {
               this.playerProgress.put(playerUUID.toString(), new HashMap<>());
           }
           PlayerQuestProgress progress = new PlayerQuestProgress(playerUUID, questId, quest);
           progress.setStatus(QuestStatus.IN_PROGRESS);
           playerProgress.get(playerUUID.toString()).put(questId, progress);
           plugin.getServer().getScheduler().runTask(plugin, () -> {
               Player player = Bukkit.getPlayer(playerUUID);
               if(player != null && player.isOnline()) player.sendMessage("§aMisión aceptada: &f" + quest.getName("es")); //Change to support multiple languages
           });
        });
    }
    public PlayerQuestProgress getPlayerQuestProgress(UUID playerUUID, int questId) {
        Map<Integer, PlayerQuestProgress> playerQuests = this.playerProgress.get(playerUUID.toString());
        if(playerQuests == null) return null;
        return playerQuests.getOrDefault(questId, null);
    }
    public Map<Integer, PlayerQuestProgress> getPlayerQuests(UUID playerUUID) {
        Map<Integer, PlayerQuestProgress> quests = playerProgress.get(playerUUID.toString());
        if(quests != null) return new HashMap<>(quests);
        return new HashMap<>();
    }
    public boolean hasPickedUpQuest(UUID playerUUID, int questId) {
        return playerProgress.getOrDefault(playerUUID.toString(), new HashMap<>()).containsKey(questId);
    }
    public boolean hasCompletedQuest(UUID playerUUID, int questId) {
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return false;
        return progress.getStatus() == QuestStatus.COMPLETED;
    }
    //Can be called async
    public void progressObjective(UUID playerUUID, int questId, int objectiveIndex, int amount) {
        Quest quest = getQuest(questId);
        if(quest == null) return;
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return;
        if(objectiveIndex >= quest.getObjectiveCount()) return;
        QuestObjective objective = quest.getObjectives().get(objectiveIndex);
        int currentProgress = progress.getObjectiveProgress()[objectiveIndex];
        int newProgress = Math.min(currentProgress + amount, objective.getRequired());
        progress.getObjectiveProgress()[objectiveIndex] = newProgress;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
           QuestDB.updateObjectiveProgress(playerUUID, questId, objectiveIndex, newProgress);
           if(quest.areAllObjectivesCompletes(progress.getObjectiveProgress())) {
               completeQuestAsync(playerUUID, questId);
           }else {
               plugin.getServer().getScheduler().runTask(plugin, () -> {
                  Player player = Bukkit.getPlayer(playerUUID);
                  if(player != null && player.isOnline()) {
                      int percentage = objective.getProgressCompletedAsPercentage(newProgress);
                      player.sendMessage("§7[Quest] &f" + objective.getTarget() + " §7(" + percentage + "%)"); //TODO: multiple languages support
                  }
               });
           }
        });
    }
    public void completeQuest(UUID playerUUID, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) return;
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null)  return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
           completeQuestAsync(playerUUID, questId);
        });
    }
    public void completeQuestAsync(UUID playerUUID, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) return;
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null)  return;
//        QuestDB.completeQuest(playerUUID, questId, quest.getRepeatCooldown());
        progress.markCompleted(quest);
//        if(QuestDB.getTrackedQuest(playerUUID) == questId) QuestDB.setTrackedQuest(playerUUID, -1);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            executeRewards(playerUUID, quest);
            Player player = Bukkit.getPlayer(playerUUID);
            if(player != null && player.isOnline()) {
                player.sendMessage("§a§l✓ Misión completada! §f" + quest.getName("es"));
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1);
                player.sendMessage("§7§m─────────────────────────────");
//               for(QuestReward reward : quest.getRewards()) {
//                   player.sendMessage(reward.getLocale().); //TODO: Add reading of messages
//
//               }
                player.sendMessage("§7§m─────────────────────────────");
                //TODO: quest message completion here
            }
        });
    }
    //Call on main thread
    private void executeRewards(UUID playerUUID, Quest quest) {
        Player player = Bukkit.getPlayer(playerUUID);
        if(player == null || !player.isOnline()) {
            String playerName = Bukkit.getOfflinePlayer(playerUUID) == null ? "N/A" : Bukkit.getOfflinePlayer(playerUUID).getName();
            plugin.getLogger().info("Player " + playerName + " is offline, skipping rewards...");
            return;
        }
        String playerName = player.getName();
        for(QuestReward reward : quest.getRewards()) {
            try {
                switch(reward.getType()) {
                    case PERMISSION:
                        plugin.getLogger().info("Player " + playerName + " gained permission: " + reward.getValue()); //TODO: add actual permission addition
                        break;
                    case COMMAND:
                        String command = reward.getResolvedValue(playerName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        plugin.getLogger().info("Executed command: " + command);
                        break;
                }
            }catch(Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error while trying to give reward type " + reward.getType() + " value " + reward.getValue() + " to player " + playerName, e);
            }
        }
    }
}
