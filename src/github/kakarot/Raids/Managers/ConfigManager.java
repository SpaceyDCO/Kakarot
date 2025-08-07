package github.kakarot.Raids.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.kakarot.Main;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Scenario.Scenario;
import github.kakarot.Raids.Scenario.Wave;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {
    private final Main plugin;
    private final Gson gson;
    private final Map<String, Arena> loadedArenas = new HashMap<>();
    private final Map<String, Scenario> loadedScenarios = new HashMap<>();
    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    //DATA
    /**
     * Gets a specified arena by its ID
     * @param arenaID Arena's ID
     * @return An optional containing the arena, empty optional otherwise
     */
    public Optional<Arena> getArena(String arenaID) {
        return Optional.ofNullable(this.loadedArenas.get(arenaID));
    }

    /**
     * Gets a specified scenario by its ID
     * @param scenarioID Scenario's ID
     * @return An optional containing the scenario, empty optional otherwise
     */
    public Optional<Scenario> getScenario(String scenarioID) {
        return Optional.ofNullable(this.loadedScenarios.get(scenarioID));
    }

    /**
     * Gets all the arenas currently loaded to RAM
     * @return A collection of arenas
     */
    public Collection<Arena> getArenas() {
        return this.loadedArenas.values();
    }

    /**
     * Gets all scenarios currently loaded to RAM
     * @return A collection of scenarios
     */
    public Collection<Scenario> getScenarios() {
        return this.loadedScenarios.values();
    }
    //DATA

    /**
     * Main method to call on plugin startup
     * This loads all config from json files
     */
    public void loadAllConfig() {
        plugin.getLogger().info("---------------------------");
        plugin.getLogger().info("\nLoading arena and scenarios config...");
        plugin.getLogger().info("\n---------------------------");

        loadScenarios();
        loadArenas();

        plugin.getLogger().info("Successfully loaded arena and scenarios config");
    }

    private void loadArenas() {
        loadedArenas.clear(); //Just in case
        File arenasDirectory = new File(plugin.getDataFolder(), "arenas");
        loadFromDirectory(arenasDirectory, "arenas/default_arena.json", Arena.class, loadedArenas);
    }
    private void loadScenarios() {
        loadedScenarios.clear(); //Just in case
        File scenariosDirectory = new File(plugin.getDataFolder(), "scenarios");
        loadFromDirectory(scenariosDirectory, "scenarios/default_scenario.json", Scenario.class, loadedScenarios);
    }

    private <T> void loadFromDirectory(File directory, String defaultResourcePath, Class<T> classOfT, Map<String, T> destinationMap) {
        final File destination = new File(directory, new File(defaultResourcePath).getName());
        if(!directory.exists()) {
            if(directory.mkdirs()) plugin.getLogger().info("Successfully created directory " + directory.getAbsolutePath());
            else {
                plugin.getLogger().log(Level.SEVERE, "Couldn't create directory " + directory.getAbsolutePath() + " with its corresponding parent folders");
                return;
            }
            saveDefaultResources(defaultResourcePath, destination);
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if(files == null || files.length == 0) {
            saveDefaultResources(defaultResourcePath, destination);
            files = directory.listFiles((dir, name) -> name.endsWith(".json"));
            if(files == null) {
                plugin.getLogger().log(Level.SEVERE, "Can't read files from " + directory + " might be a permissions issue\nARENAS AND SCENARIOS WEREN'T LOADED PROPERLY");
                return;
            }
        }
        for(File file : files) {
            try(Reader reader = new FileReader(file)) {
                T loadedObject = gson.fromJson(reader, classOfT);
                if(loadedObject != null) {
                    String key = file.getName().replace(".json", "");
                    destinationMap.put(key.toLowerCase(), loadedObject);
                    //Sort waves after loading
                    if(loadedObject instanceof Scenario) {
                        Scenario scenario = (Scenario) loadedObject;
                        scenario.getWaves().sort(Comparator.comparingInt(Wave::getWaveNumber));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't load file " + file.getName() + " in " + file.getAbsolutePath(), e);
            }
        }
    }

    private void saveDefaultResources(String resourcePath, File destination) {
        try {
            if(!destination.exists()) {
                Files.copy(plugin.getResource(resourcePath), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created default config: " + destination.getName());
            }
        }catch(IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save default resource " + resourcePath + ".", e.getMessage());
        }
    }
}
