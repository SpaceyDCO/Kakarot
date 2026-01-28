package github.kakarot.Quests.Managers;

import github.kakarot.Main;
import github.kakarot.Quests.Models.*;
import github.kakarot.Quests.Quest;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class QuestManager {
    @Getter
    private static QuestManager instance;
    private final Main plugin;
    //Quests cache loaded once on startup
    private final Map<Integer, Quest> quests = new HashMap<>();
    //Key: playerUUID, Value: map of questId -> progress ; useful for checking player progress quickly
    private final Map<String, Map<Integer, PlayerQuestProgress>> playerProgress = new HashMap<>();
    public QuestManager(Main plugin) {
        this.plugin = plugin;
        instance = this;
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
                    names.put(locale, nameSection.getString(locale));
                }
            }
            if(names.isEmpty()) names.put("es", "Misión #" + questId);
            Map<String, String> descriptions = new HashMap<>();
            ConfigurationSection descriptionSection = questSection.getConfigurationSection("description");
            if(descriptionSection != null) {
                for(String locale : descriptionSection.getKeys(false)) {
                    descriptions.put(locale, descriptionSection.getString(locale));
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
            boolean repeatable = questSection.getBoolean("repeatable", false);
            long repeatCooldown = questSection.getLong("repeatCooldown", 86400000);
            int npcId = questSection.getInt("npcId", -1);
            Quest quest = new Quest(
                    questId,
                    names,
                    descriptions,
                    objectives,
                    rewards,
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
            RewardType type = RewardType.valueOf(typeStr.toUpperCase());
            return new QuestReward(type, value);
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
    public static Quest getQuest(int questId) {
        if(instance == null) return null;
        return instance.quests.get(questId);
    }
    public static Map<Integer, Quest> getAllQuests() {
        if(instance == null) return null;
        return new HashMap<>(instance.quests);
    }
}
