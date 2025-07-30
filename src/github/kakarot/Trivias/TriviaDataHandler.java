package github.kakarot.Trivias;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.kakarot.Main;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
public class TriviaDataHandler {
    public static Map<String, TriviasData> triviasDataMap = new ConcurrentHashMap<>();
    private final TriviasData data;
    private final String triviaID;
    public TriviaDataHandler(String triviaID) {
        this.triviaID = triviaID;
        this.data = triviasDataMap.getOrDefault(triviaID, new TriviasData());
    }
    public void save() {
        triviasDataMap.put(this.triviaID, this.data);
        saveToConfig();
    }
    public boolean isConfigured() {
        return this.data.getAnswers() != null && this.data.getRewardsCommands() != null;
    }
    public void setEnabled(boolean enabled) {
        this.data.setEnabled(enabled);
    }
    public void addReward(String reward) {
        if(this.data.getRewardsCommands() != null) this.data.getRewardsCommands().add(reward);
        else {
            Set<String> set = new HashSet<>();
            set.add(reward);
            this.data.setRewardsCommands(set);
        }
    }
    public void removeRewards() {
        if(this.data.getRewardsCommands() != null) this.data.setRewardsCommands(null);
    }
    public void addAnswer(String answer) {
        if(this.data.getAnswers() != null) this.data.getAnswers().add(answer);
        else {
            Set<String> set = new HashSet<>();
            set.add(answer);
            this.data.setAnswers(set);
        }
    }
    public void removeAnswers() {
        if(this.data.getAnswers() != null) this.data.setAnswers(null);
    }

    //STATIC
    public static Map<String, TriviasData> getAllTriviasMap() {
        try {
            return triviasDataMap;
        }catch(NullPointerException e) {
            return null;
        }
    }
    public static Set<String> getAllTriviasIds() {
        try {
            return triviasDataMap.keySet();
        }catch(NullPointerException e) {
            return null;
        }
    }
    public static TriviasData getTrivia(String id) {
        try {
            return triviasDataMap.getOrDefault(id, null);
        }catch(NullPointerException e) {
            return null;
        }
    }
    public static void addTrivia(String triviaID, TriviasData data) {
        if(triviasDataMap.containsKey(triviaID)) return; //Don't save if it already exists
        triviasDataMap.put(triviaID, data);
    }
    public static void addTrivia(String triviaID, String category, String question, int timeLimit, Set<String> answers, Set<String> rewardsCommands, boolean enabled) {
        if(triviasDataMap.containsKey(triviaID)) return; //Don't save if it already exists
        TriviasData triviaToAdd = new TriviasData(category, question, timeLimit, answers, rewardsCommands, enabled);
        triviasDataMap.put(triviaID, triviaToAdd);
    }
    //STATIC
    private void saveToConfig() {
        if(triviasDataMap == null) {
            Bukkit.getLogger().warning("TriviasDataMap is null, cannot save!");
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File dataFolder = Main.instance.getDataFolder();
        if(!dataFolder.exists()) {
            if(!dataFolder.mkdirs()) {
                Bukkit.getLogger().log(Level.SEVERE, "Could not create folder for saving Trivia data... " + dataFolder.getAbsolutePath());
                return;
            }
        }
        File saveFile = new File(dataFolder, "trivias_data.json");
        Type typeOfMap = new TypeToken<ConcurrentHashMap<String, TriviasData>>(){}.getType();
        try(Writer writer = new OutputStreamWriter(Files.newOutputStream(saveFile.toPath()), StandardCharsets.UTF_8)) {
            gson.toJson(triviasDataMap, typeOfMap, writer);
            Bukkit.getLogger().info("Successfully saved " + triviasDataMap.size() + " trivia entries to " + saveFile.getName());
        }catch(IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save TRIVIAS DATA!, send log to SpaceyDCO...", e);
        }
    }
}
