package github.kakarot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import github.kakarot.Parties.Managers.IPartyManager;
import github.kakarot.Tools.ClassesRegistration;
import github.kakarot.Tools.Commands.CommandFramework;
import github.kakarot.Parties.Managers.PartyManager;
import github.kakarot.Trivias.TriviaDataHandler;
import github.kakarot.Trivias.TriviasData;
import github.kakarot.Trivias.TriviasRunnable;
import lombok.Getter;
import net.minecraft.util.com.google.common.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


@Getter
public class Main extends JavaPlugin {
    public static boolean activeTrivia;
    public static Main instance;

    @Getter
    private IPartyManager partyManager;
    private final CommandFramework commandFramework = new CommandFramework(this);
    private final ClassesRegistration classesRegistration = new ClassesRegistration();

    @Override
    public void onEnable() {
        instance = this;
        classesRegistration.loadCommands("github.kakarot.Commands");
        classesRegistration.loadListeners("github.kakarot.Events");
        Bukkit.getConsoleSender().sendMessage("Activated plugin Kakarot");
        Bukkit.getConsoleSender().sendMessage("By: SpaceyDCO");
        //Trivia
        readTriviaConfig();
        TriviasRunnable.runnableTrivias.runTaskTimer(this, TriviasRunnable.TriviaCooldown, TriviasRunnable.TriviaCooldown);
        activeTrivia = false;
        //Parties
        this.partyManager = new PartyManager(this);
    }
    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("Deactivating plugin Kakarot");
        TriviasRunnable.runnableTrivias.cancel();
    }

    //TRIVIA
    private void readTriviaConfig() {
        File dataFolder = getDataFolder();
        File saveFile = new File(dataFolder, "trivias_data.json");
        Path savePath = saveFile.toPath();
        if(!Files.exists(savePath)) {
            Bukkit.getLogger().warning(saveFile.getName() + " not found. No trivia data to load. Will set an empty map...");
            TriviaDataHandler.triviasDataMap = new ConcurrentHashMap<>();
        }
        Gson gson = new GsonBuilder().create();
        Type typeOfMap = new TypeToken<ConcurrentHashMap<String, TriviasData>>(){}.getType();
        try(Reader reader = new InputStreamReader(Files.newInputStream(savePath), StandardCharsets.UTF_8)) {
            Map<String, TriviasData> loadedMap = gson.fromJson(reader, typeOfMap);
            if(loadedMap != null) {
                Bukkit.getLogger().info("Successfully read " + loadedMap.size() + " trivia entries");
                TriviaDataHandler.triviasDataMap.clear();
                TriviaDataHandler.triviasDataMap.putAll(loadedMap);
            }else {
                Bukkit.getLogger().log(Level.SEVERE, saveFile.getName() + " is empty or contains null, an empty map will be set instead...");
                TriviaDataHandler.triviasDataMap = new ConcurrentHashMap<>();
            }
        }catch(IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not read trivias data, send log to SpaceyDCO!", e);
            TriviaDataHandler.triviasDataMap = new ConcurrentHashMap<>();
        }catch(JsonSyntaxException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error parsing JSON from " + saveFile.getName() + ", .json has the wrong syntax!", e);
            TriviaDataHandler.triviasDataMap = new ConcurrentHashMap<>();
        }
    }
    //TRIVIA

}