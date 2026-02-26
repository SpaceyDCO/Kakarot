package github.kakarot.Quests.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spacey.kakarotmod.api.KakarotModAPI;
import github.kakarot.Main;
import github.kakarot.Quests.Models.*;
import github.kakarot.Quests.Quest;
import github.kakarot.Tools.HexcodeUtils;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {
    private final Main plugin;
    //Quests cache loaded once on startup
    private final Map<Integer, Quest> quests = new HashMap<>();
    //Key: playerUUID, Value: map of questId -> progress ; useful for checking player progress quickly
    public final Map<String, Map<Integer, PlayerQuestProgress>> playerProgress = new HashMap<>();
    public final Map<String, List<QuestObjectiveReference>> npcObjectives = new HashMap<>();
    public final Map<String, List<QuestTurnInReference>> npcsTurnIn = new HashMap<>();
    //Key: language code like "en", "es", Value: map with key message_id (like command.error)
    public final Map<String, Map<String, String>> localizedMessages = new HashMap<>();
    public QuestManager(Main plugin) {
        this.plugin = plugin;
    }
    //TODO: FIX Collect item bug (update progress on player item pickup properly)

    /**
     * Initializes the quest manager, this is called on plugin startup and loads all quests from quests.yml
     */
    public void initialize() {
        plugin.getLogger().info("Initializing quest manager...");
        loadQuests();
        loadMessages();
        plugin.getLogger().info("Loaded " + quests.size() + " quests");
    }

    /**
     * Reload quests from YAML
     */
    public void reloadQuests() {
        quests.clear();
        loadQuests();
        loadMessages();
        plugin.getLogger().info("Reloaded quests!");
    }
    private void loadQuests() {
        File dataFolder = new File(plugin.getDataFolder(), "quests/data");
        if(!dataFolder.exists()) {
            if(dataFolder.mkdirs()) createDefaultQuests(new File(dataFolder, "quest-example_1.json"));
            else {
                plugin.getLogger().severe("Could not create quest data folder in " + dataFolder.getAbsolutePath());
                return;
            }
        }
        File[] questFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if(questFiles == null || questFiles.length == 0) {
            plugin.getLogger().warning("No quest files found in quests/data");
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for(File file : questFiles) {
            try {
                int questId = extractQuestIdFromFilename(file.getName());
                Quest quest;
                try(InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    quest = gson.fromJson(reader, Quest.class);
                }
                if(quest.getId() != questId) {
                    plugin.getLogger().severe("ID mismatch in " + file.getName());
                    continue;
                }
                quests.put(questId, quest);
                plugin.getLogger().info("Loaded quest #" + quest.getId());
            }catch(IllegalArgumentException e) {
                plugin.getLogger().log(Level.SEVERE, "Invalid filename: " + file.getName() + " in " + file.getAbsolutePath(), e.getMessage());
            }catch(FileNotFoundException e) {
                plugin.getLogger().log(Level.SEVERE, "File not found OR failed to load: " + file.getName(), e);
            }catch(Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not load quest " + file.getName() + "...", e);
            }
        }
        if(!quests.isEmpty()) {
            buildReverseIndex();
        }
    }
    private void loadMessages() {
        File dataFolder = new File(plugin.getDataFolder(), "quests/messages");
        if(!dataFolder.exists()) {
            if(dataFolder.mkdirs()) {
                createDefaultLanguages(new File(dataFolder, "es.yml"));
                //TODO: add english messages too
            }
            else {
                plugin.getLogger().severe("Could not create quest message data folder in " + dataFolder.getAbsolutePath());
                return;
            }
        }
        File[] messageFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if(messageFiles == null || messageFiles.length == 0) {
            plugin.getLogger().warning("No messages found in quests/messages");
            return;
        }
        for(File messages : messageFiles) {
            try {
                String langCode = messages.getName().replace(".yml", "").toLowerCase();
                FileConfiguration config = loadYamlUtf8(messages);
                Map<String, String> translations = new HashMap<>();
                for(String key : config.getKeys(true)) {
                    if(!config.isConfigurationSection(key)) {
                        String translatedText = config.getString(key);
                        if(translatedText != null) {
                            translatedText = ChatColor.translateAlternateColorCodes('&', translatedText);
                            translations.put(key, translatedText);
                        }
                    }
                }
                localizedMessages.put(langCode, translations);
                plugin.getLogger().info("Loaded " + translations.size() + " messages for language: " + langCode);
            }catch(Exception e) {
                plugin.getLogger().log(Level.SEVERE, "There was an error trying to load messages from file " + messages.getName(), e);
            }
        }
    }
    private YamlConfiguration loadYamlUtf8(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        try(InputStream in = Files.newInputStream(file.toPath());
            Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }
    private void createDefaultLanguages(File destination) {
        try {
            if(!destination.exists()) {
                Files.copy(plugin.getResource("quests/messages/es.yml"), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created default config: " + destination.getName());
            }
        }catch(Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save default resource: " + "quests/messages/es.yml", e);
        }
    }
    public String getLangMessage(UUID playerUUID, String messageKey, String... placeholders) {
        String lang = plugin.getSettingsManager().getPlayerLanguage().getOrDefault(playerUUID, "es");
        Map<String, String> langMap = localizedMessages.getOrDefault(lang, localizedMessages.get("es"));
        if(langMap == null) return "§cArchivo de lenguaje no encontrado: " + lang;
        String message = langMap.get(messageKey);
        if(message == null) {
            Map<String, String> esMap = localizedMessages.get("es");
            message = (esMap != null) ? esMap.getOrDefault(messageKey, "§cID de mensaje no encontrada: " + messageKey) : "§cID de mensaje no encontrada: " + messageKey;
        }
        if(placeholders != null && placeholders.length > 0) {
            if(placeholders.length % 2 != 0) {
                plugin.getLogger().warning("Uneven placeholder arguments for key: " + messageKey);
            }else {
                for(int i = 0; i < placeholders.length; i += 2) {
                    message = message.replace(placeholders[i], placeholders[i+1]);
                }
            }
        }
        return message;
    }
//    /**
//     * Load all quests from quests/quests.yml
//     * Reads from YAML, old format, moved to JSON
//     */
//    @Deprecated
//    private void loadQuestsFromYaml() {
//        try {
//            File questFolder = new File(plugin.getDataFolder(), "quests");
//            if(!questFolder.exists()) if(!questFolder.mkdirs()) plugin.getLogger().severe("Could not create folder \"quests\" in " + questFolder.getAbsolutePath());
//            File questsFile = new File(questFolder, "quests.yml");
//            if(!questsFile.exists()) createDefaultQuests(questsFile);
//            FileConfiguration config = YamlConfiguration.loadConfiguration(questsFile);
//            ConfigurationSection questsSection = config.getConfigurationSection("quests");
//            if(questsSection == null) {
//                plugin.getLogger().warning("No quests section found in quests.yml");
//                return;
//            }
//            for(String questIdStr : questsSection.getKeys(false)) {
//                try {
//                    int questId = Integer.parseInt(questIdStr);
//                    ConfigurationSection questSection = questsSection.getConfigurationSection(questIdStr);
//                    if(questSection == null) continue;
//                    Quest quest = loadQuestFromYAML(questId, questSection);
//                    if(quest != null) {
//                        quests.put(questId, quest);
//                        buildReverseIndex();
//                    }
//                }catch(NumberFormatException e) {
//                    plugin.getLogger().warning("Invalid quest ID: " + questIdStr);
//                }
//            }
//        }catch(Exception e) {
//            plugin.getLogger().log(Level.SEVERE, "Error loading quests.yml!", e);
//        }
//    }
//    /**
//     * Retrieves a single quest from YAML configuration
//     */
//    private Quest loadQuestFromYAML(int questId, ConfigurationSection questSection) {
//        try {
//            Map<String, String> names = new HashMap<>();
//            ConfigurationSection nameSection = questSection.getConfigurationSection("name");
//            if(nameSection != null) {
//                for(String locale : nameSection.getKeys(false)) {
//                    names.put(locale, nameSection.getString(locale).replace("&", "§"));
//                }
//            }
//            if(names.isEmpty()) names.put("es", "Misión #" + questId);
//            Map<String, String> descriptions = new HashMap<>();
//            ConfigurationSection descriptionSection = questSection.getConfigurationSection("description");
//            if(descriptionSection != null) {
//                for(String locale : descriptionSection.getKeys(false)) {
//                    descriptions.put(locale, descriptionSection.getString(locale).replace("&", "§"));
//                }
//            }
//            if(descriptions.isEmpty()) descriptions.put("es", "Sin descripción");
//            List<QuestObjective> objectives = new ArrayList<>();
//            List<Map<?, ?>> objectivesList = questSection.getMapList("objectives");
//            for(Map<?, ?> objMap : objectivesList) {
//                QuestObjective obj = parseObjective(objMap, questId);
//                if(obj != null) objectives.add(obj);
//            }
//            List<QuestReward> rewards = new ArrayList<>();
//            List<Map<?, ?>> rewardsList = questSection.getMapList("rewards");
//            for(Map<?, ?> rewMap : rewardsList) {
//                QuestReward reward = parseReward(rewMap);
//                if(reward != null) rewards.add(reward);
//            }
//            Map<String, String> completionMessages = new HashMap<>();
//            ConfigurationSection completionMessageSection = questSection.getConfigurationSection("completionMessage");
//            if(completionMessageSection != null) {
//                for(String locale : completionMessageSection.getKeys(false)) {
//                    completionMessages.put(locale, completionMessageSection.getString(locale));
//                }
//            }
//            boolean repeatable = questSection.getBoolean("repeatable", false);
//            long repeatCooldown = questSection.getLong("repeatCooldown", 86400000);
//            Quest quest = new Quest(
//                    questId,
//                    names,
//                    descriptions,
//                    objectives,
//                    rewards,
//                    completionMessages,
//                    repeatable,
//                    repeatCooldown
//            );
//            plugin.getLogger().info("Loaded quest #" + questId + ": "
//            + names.getOrDefault("es", "?"));
//            return quest;
//        }catch(Exception e) {
//            Bukkit.getLogger().log(Level.SEVERE, "Error loading quest #" + questId, e);
//            return null;
//        }
//    }
//
//    /**
//     * Parses a single objective
//     */
//    private QuestObjective parseObjective(Map<?, ?> map, int questId) {
//        try {
//            String typeStr = (String) map.get("type");
//            String target = "";
//            String title = "";
//            Object objInfo = map.get("objectiveInfo");
//            if(objInfo instanceof Map<?, ?>) {
//                Map<?, ?> objMap = (Map<?, ?>) objInfo;
//                if(objMap.get("target") == null) {
//                    plugin.getLogger().warning("No objective target set for quest #" + questId);
//                    return null;
//                }
//                target = (String) objMap.get("target");
//                title = objMap.get("title") != null && !((String) objMap.get("title")).isEmpty() ? (String) objMap.get("title") : "";
//            }
//            int required;
//            if(map.get("required") != null) required = ((Number) map.get("required")).intValue();
//            else required = 1;
//            boolean shareable;
//            if(map.get("shareable") != null) shareable = (Boolean) map.get("shareable");
//            else shareable = false;
//            ObjectiveType type = ObjectiveType.valueOf(typeStr.toUpperCase());
//            ObjectiveInfo info = new ObjectiveInfo(target, title);
//            return new QuestObjective(type, info, required, shareable);
//        }catch(Exception e) {
//            plugin.getLogger().log(Level.WARNING, "Error parsing objective: " + e);
//            return null;
//        }
//    }
//
//    /**
//     * Parses a single reward
//     */
//    private QuestReward parseReward(Map<?, ?> map) {
//        try {
//            String typeStr = (String) map.get("type");
//            String value = (String) map.get("value");
//            Object descObj = map.get("descriptions");
//            Map<String, String> localizedDescriptions = new HashMap<>();
//            if(descObj instanceof Map<?, ?>) {
//                Map<?, ?> descMap = (Map<?, ?>) descObj;
//                for(Map.Entry<?, ?> descEntry : descMap.entrySet()) {
//                    localizedDescriptions.put(descEntry.getKey().toString(), descEntry.getValue().toString());
//                }
//            }
//            RewardType type = RewardType.valueOf(typeStr.toUpperCase());
//            return new QuestReward(type, value, localizedDescriptions);
//        }catch(Exception e) {
//            plugin.getLogger().log(Level.WARNING, "Error parsing quest reward: ", e);
//            return null;
//        }
//    }
    private void createDefaultQuests(File destination) {
        try {
            if(!destination.exists()) {
                Files.copy(plugin.getResource("quests/quest-example_1.json"), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created default config: " + destination.getName());
            }
        }catch(Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save default resource: " + "quests/data/quest-example_1.json", e);
        }
    }
    public Quest getQuest(int questId) {
        return this.quests.get(questId);
    }
    public Map<Integer, Quest> getAllQuests() {
        return new HashMap<>(this.quests);
    }
    private void buildReverseIndex() {
        npcObjectives.clear();
        for(Quest quest : quests.values()) {
            for(int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if(obj.getType() == ObjectiveType.KILL_MOBS || obj.getType() == ObjectiveType.TALK_TO_NPC) {
                    npcObjectives.computeIfAbsent(obj.getObjectiveInfo().getTarget(), k -> new ArrayList<>()).add(new QuestObjectiveReference(quest.getId(), i, obj.getObjectiveInfo().getTitle(), obj.getType()));
                }
            }
            if(quest.isTurnIn()) {
                npcsTurnIn.computeIfAbsent(quest.getNpcTurnInDetails().getName(), k -> new ArrayList<>()).add(new QuestTurnInReference(quest.getId(), quest.getNpcTurnInDetails().getTitle()));
            }
        }
    }
    private int extractQuestIdFromFilename(String fileName) throws IllegalArgumentException {
        String name = fileName.replace(".json", "");
        if(!name.contains("_")) throw new IllegalArgumentException("Filename must contain underscore: quest-name_id.json");
        String[] parts = name.split("_");
        if(parts.length != 2) throw new IllegalArgumentException("Filename must comply format quest-name_id.json (only 1 underscore)");
        String questName = parts[0];
        String questId = parts[1];
        if(!questName.matches("[a-z0-9-]+")) throw new IllegalArgumentException("Quest name must be lowercase with hyphens: " + questName);
        try {
            return Integer.parseInt(questId);
        }catch(NumberFormatException e) {
            throw new IllegalArgumentException("Quest ID must be a number: " + questId);
        }
    }
    public void addQuestToPlayer(UUID playerUUID, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) {
            plugin.getLogger().severe("Could not fetch quest with ID " + questId);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if(hasPickedUpQuest(playerUUID, questId) && hasCompletedQuest(playerUUID, questId)) {
                if(!plugin.getQuestDBManager().pickupRepeatableQuest(playerUUID, questId, quest.getObjectiveCount())) {
                    Bukkit.getPlayer(playerUUID).sendMessage(getLangMessage(playerUUID, "manager.db_error"));
                    return;
                }
            }else if(!plugin.getQuestDBManager().pickupQuest(playerUUID, questId, quest.getObjectiveCount())) {
                Bukkit.getPlayer(playerUUID).sendMessage(getLangMessage(playerUUID, "manager.db_error"));
                return;
            }
           if(!playerProgress.containsKey(playerUUID.toString())) {
               this.playerProgress.put(playerUUID.toString(), new HashMap<>());
           }
           plugin.getServer().getScheduler().runTask(plugin, () -> {
               PlayerQuestProgress progress = new PlayerQuestProgress(playerUUID, questId, quest);
               progress.setStatus(QuestStatus.IN_PROGRESS);
               playerProgress.get(playerUUID.toString()).put(questId, progress);
               Player player = Bukkit.getPlayer(playerUUID);
               String playerLocale = this.plugin.getSettingsManager().getPlayerLanguage().getOrDefault(playerUUID, "es");
               String questAcceptedMsg = getLangMessage(playerUUID, "manager.quest_accepted", "%quest_name%", quest.getName(playerLocale));
               if(player != null && player.isOnline()) player.sendMessage(questAcceptedMsg);
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
    public boolean hasCompletedObjective(UUID playerUUID, int questId, int objectiveIndex) {
        Quest quest = getQuest(questId);
        if(quest == null) return false;
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return false;
        QuestObjective objective = quest.getObjectives().get(objectiveIndex);
        return progress.hasCompletedObjective(objectiveIndex, objective.getRequired());
    }
    public void progressObjective(UUID playerUUID, int questId, int objectiveIndex, Location completionLoc, int amount) {
        Quest quest = getQuest(questId);
        if(quest == null) return;
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return;
        if(objectiveIndex >= quest.getObjectiveCount()) return;
        QuestObjective objective = quest.getObjectives().get(objectiveIndex);
        if(objective.isShareable()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if(plugin.getPartyManager().getParty(player).isPresent()) {
                for(UUID member : plugin.getPartyManager().getParty(player).get().getMembers()) {
                    Player partyPlayer = Bukkit.getPlayer(member);
                    if(partyPlayer == null || !partyPlayer.isOnline()) continue;
                    //Progress objective for player IF he is within 75 blocks from the objective completion location
                    if(hasPickedUpQuest(member, questId) && isPlayerWithinObjectiveRange(partyPlayer.getLocation(), completionLoc, 75)) {
                        PlayerQuestProgress partyPlayerProgress = getPlayerQuestProgress(member, questId);
                        if(partyPlayerProgress == null) continue;
                        progressObjectiveForPlayer(partyPlayerProgress, objective, quest, member, questId, objectiveIndex, amount);
                    }
                }
                return;
            }
        }
        progressObjectiveForPlayer(progress, objective, quest, playerUUID, questId, objectiveIndex, amount);
    }

    /**
     * Checks whether a repeatable quest can be repeated already or not
     * @param playerUUID The player's UUID
     * @param questId The quest ID to perform the check on
     * @return true if cooldown has passed, false otherwise (or if no progress for this player)
     */
    public boolean canRepeat(UUID playerUUID, int questId) {
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return false;
        return progress.canRepeat();
    }
    public String getRemainingCooldown(UUID playerUUID, int questId) {
        Quest quest = getQuest(questId);
        if(!quest.isRepeatable()) return "XX:YY:ZZ";
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return "XX:YY:ZZ";
        long remainingTime = progress.getNextAvailable() - System.currentTimeMillis(); //TODO: format into something cleaner
        return String.valueOf(remainingTime);
    }
    //Can be called on main thread
    public void progressObjectiveForPlayer(PlayerQuestProgress progress, QuestObjective objective, Quest quest, UUID playerUUID, int questId, int objectiveIndex, int amount) {
        if(hasCompletedQuest(playerUUID, questId) || hasCompletedObjective(playerUUID, questId, objectiveIndex)) return; //Quest or objective already completed
        int currentProgress = progress.getObjectiveProgress()[objectiveIndex];
        int newProgress = Math.min(currentProgress + amount, objective.getRequired());
        //Run DB update before anything else
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if(!this.plugin.getQuestDBManager().incrementObjectiveProgress(playerUUID, questId, objectiveIndex, amount)) {
                plugin.getLogger().severe("Could not progress objective in database for player " + playerUUID);
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                progress.getObjectiveProgress()[objectiveIndex] = newProgress;
                if(quest.areAllObjectivesCompletes(progress.getObjectiveProgress())) {
                    if(!quest.isTurnIn()) {
                        //Clear arrow guide instantly when completing the quest
                        KakarotModAPI.clearQuestTarget(Bukkit.getPlayer(playerUUID).getName());
                        completeQuest(playerUUID, questId);
                    }
                    else {
                        Player player = Bukkit.getPlayer(playerUUID);
                        String turnInMessage = getLangMessage(playerUUID, "manager.turn_in_ready", "%npc_name%", quest.getNpcTurnInDetails().getName());
                        player.sendMessage(turnInMessage);
                        NpcTurnInDetails details = quest.getNpcTurnInDetails();
                        KakarotModAPI.setQuestTarget(player.getName(), details.getX(), details.getY(), details.getZ(), "Quest Completed", HexcodeUtils.parseColor(details.getArrowColor()));
                    }
                }else {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if(player != null && player.isOnline()) {
                        int percentage = objective.getProgressCompletedAsPercentage(newProgress);
                        player.sendMessage("§7[Quest] §f" + objective.getObjectiveInfo().getTarget() + " §7(" + percentage + "%)");
                        if(progress.getObjectiveProgress()[objectiveIndex] >= objective.getRequired()) {
                            //Objective completed, update tracking to next objective...
                            updateTrackingToNextObj(progress, quest, player);
                            return;
                        }
                        //Objective not completed, just update tracking...
                        TrackingInfo info = objective.getTrackingInfo();
                        if(info == null) return;
                        String playerLocale = plugin.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
                        String label = info.getLabel().getOrDefault(playerLocale, "");
                        label = label.replace("%current_progress%", String.valueOf(progress.getObjectiveProgress()[objectiveIndex])).replace("%required%", String.valueOf(objective.getRequired()));
                        KakarotModAPI.setQuestTarget(player.getName(), info.getX(), info.getY(), info.getZ(), label, HexcodeUtils.parseColor(info.getArrowColor()), HexcodeUtils.parseColor(info.getLabelColor()));
                    }
                }
            });
        });
    }
    private void updateTrackingToNextObj(PlayerQuestProgress progress, Quest quest, Player player) {
        int nonCompletedObjIndex = 0;
        int objectiveCount = Math.min(quest.getObjectiveCount(), progress.getObjectiveProgress().length);
        for(int i = 0; i < objectiveCount; i++) {
            int objProgress = progress.getObjectiveProgress()[i];
            if(objProgress < quest.getObjectives().get(i).getRequired()) {
                nonCompletedObjIndex = i;
                break;
            }
        }
        QuestObjective obj2 = quest.getObjectives().get(nonCompletedObjIndex);
        TrackingInfo nextObjInfo = obj2.getTrackingInfo();
        if(nextObjInfo == null) return;
        String playerLocale = plugin.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
        String label = nextObjInfo.getLabel().getOrDefault(playerLocale, "");
        label = label.replace("%current_progress%", String.valueOf(progress.getObjectiveProgress()[nonCompletedObjIndex])).replace("%required%", String.valueOf(obj2.getRequired()));
        KakarotModAPI.setQuestTarget(player.getName(), nextObjInfo.getX(), nextObjInfo.getY(), nextObjInfo.getZ(), label, HexcodeUtils.parseColor(nextObjInfo.getArrowColor()), HexcodeUtils.parseColor(nextObjInfo.getLabelColor()));
    }
    public void completeQuest(UUID playerUUID, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) return;
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return;
        if(hasCompletedQuest(playerUUID, questId)) return;
        progress.markCompleted(getQuest(questId));
        removeQuestItems(Bukkit.getPlayer(playerUUID), questId);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
           completeQuestAsync(playerUUID, questId);
        });
    }
    public boolean hasRequiredItems(Player player, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) return false;
        int totalCollectItemsObjectives = 0;
        int completionCount = 0;
        for(QuestObjective obj : quest.getObjectives()) {
            if(obj.getType() != ObjectiveType.COLLECT_ITEMS) continue;
            totalCollectItemsObjectives++;
            if(plugin.getProgressManager().playerHasRequiredItem(player, obj)) completionCount++;
        }
        return completionCount >= totalCollectItemsObjectives;
    }
    public void removeQuestItems(Player player, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) return;
        for(QuestObjective obj : quest.getObjectives()) {
            if(obj.getType() != ObjectiveType.COLLECT_ITEMS) continue;
            ObjectiveInfo info = obj.getObjectiveInfo();
            int itemId = info.getItemId();
            byte dataValue = info.getDataValue();
            NBTTagCompound requiredNBT = info.getParsedNbt();
            int amountToRemove = obj.getRequired();
            for(int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getContents()[i];
                if(amountToRemove <= 0) break;
                if(item == null || item.getTypeId() != itemId) continue;
                if(item.getData().getData() != dataValue) continue;
                if(requiredNBT != null) {
                    if(!plugin.getProgressManager().itemMatchesNbt(item, requiredNBT)) continue;
                }
                int removeCount = Math.min(item.getAmount(), amountToRemove);
                item.setAmount(item.getAmount() - removeCount);
                amountToRemove -= removeCount;
            }
        }
    }
    public void completeQuestAsync(UUID playerUUID, int questId) {
        Quest quest = getQuest(questId);
        if(quest == null) return;
        PlayerQuestProgress progress = getPlayerQuestProgress(playerUUID, questId);
        if(progress == null) return;
        //Update database, error if unsuccessful
        if(!this.plugin.getQuestDBManager().completeQuest(playerUUID, questId, (quest.getRepeatCooldown()+System.currentTimeMillis()))) {
            this.plugin.getLogger().severe("Could not update database to complete quest " + questId + " for player " + playerUUID);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            executeRewards(playerUUID, quest);
            Player player = Bukkit.getPlayer(playerUUID);
            if(player != null && player.isOnline()) {
                KakarotModAPI.clearQuestTarget(player.getName());
                String playerLocale = this.plugin.getSettingsManager().getPlayerLanguage().getOrDefault(playerUUID, "es");
                String title = quest.getTitleInfo().getTitle().getOrDefault(playerLocale, "");
                title = title.replace("%quest_name%", quest.getName(playerLocale));
                String subtitle = quest.getTitleInfo().getSubtitle().getOrDefault(playerLocale, "");
                subtitle = subtitle.replace("%quest_name%", quest.getName(playerLocale));
                KakarotModAPI.displayTitle(
                        player.getName(),
                        title,
                        subtitle,
                        quest.getTitleInfo().getDisplayTime(),
                        quest.getTitleInfo().getFadeoutTime(),
                        HexcodeUtils.parseColor(quest.getTitleInfo().getTitleColor()),
                        HexcodeUtils.parseColor(quest.getTitleInfo().getSubtitleColor()));
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1);
                player.sendMessage("§7§m─────────────────────────────");
               for(QuestReward reward : quest.getRewards()) {
                   player.sendMessage(reward.getDescription().getOrDefault(playerLocale, "").replace("&", "§"));
               }
                player.sendMessage("§7§m─────────────────────────────");
                player.sendMessage(quest.getCompletionMessage().getOrDefault(playerLocale, "").replace("&", "§"));
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
    private boolean isPlayerWithinObjectiveRange(Location playerLocation, Location completionLocation, int range) {
        if(playerLocation.getWorld() != completionLocation.getWorld()) return false;
        return playerLocation.distance(completionLocation) < range;
    }
    @Getter
    public static class QuestObjectiveReference {
        final int questId;
        final int objectiveIndex;
        final String title;
        final ObjectiveType type;
        QuestObjectiveReference(int questId, int objectiveIndex, String title, ObjectiveType type) {
            this.questId = questId;
            this.objectiveIndex = objectiveIndex;
            this.title = title;
            this.type = type;
        }
    }
    @Getter
    public static class QuestTurnInReference {
        final int questId;
        final String title;
        QuestTurnInReference(int questId, String title) {
            this.questId = questId;
            this.title = title;
        }
    }
}
