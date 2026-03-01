package github.kakarot.Phasing;

import github.kakarot.Main;
import github.kakarot.Phasing.Cache.PhasingCache;
import github.kakarot.Phasing.Models.PhasedNPC;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

@AllArgsConstructor
public class PhasingConfigManager {
    private final Main plugin;
    public void loadPhasedNPCsToCache() {
        PhasingCache.clear();
        File phasingDataFolder = new File(plugin.getDataFolder(), "phasing");
        File phasingDataFile = new File(phasingDataFolder, "phasingData.yml");
        if(!phasingDataFolder.exists()) {
            if(!phasingDataFolder.mkdirs()) {
                this.plugin.getLogger().severe("Couldn't create phasing datafolder in " + phasingDataFolder.getAbsolutePath());
                return;
            }
        }
        if(!phasingDataFile.exists()) {
            try {
                Files.copy(plugin.getResource("phasing/phasingData.yml"), phasingDataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created default resource: " + phasingDataFile.getAbsolutePath());
            }catch(IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save default resource: " + "phasing/phasingData.yml", e);
                return;
            }
        }
        try {
            YamlConfiguration phasingFileConfig = loadYamlUtf8(phasingDataFile);
            if(!phasingFileConfig.contains("npcs")) return;
            ConfigurationSection npcSection = phasingFileConfig.getConfigurationSection("npcs");
            for(String key : npcSection.getKeys(false)) {
                try {
                    String name = npcSection.getString(key + ".name");
                    String title = npcSection.getString(key + ".title");
                    String permission = npcSection.getString(key + ".permission");
                    if(name == null || permission == null) {
                        plugin.getLogger().warning("Skipping invalid NPC within yml phasingData: " + key + ". Missing name or permission.");
                        continue;
                    }
                    PhasingCache.add(new PhasedNPC(key, name, title, permission));
                }catch(Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load NPC: " + key + ". Formatting error.", e);
                }
            }
            plugin.getLogger().info("Successfully loaded " + PhasingCache.getAll().size() + " NPCs for Phasing System...");
        }catch(IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Invalid configuration for file phasingData.yml inside " + phasingDataFolder.getAbsolutePath(), e);
        }
    }
    public void savePhasedNPC(String id, String name, String title, String permission) {
        File phasingFile = new File(plugin.getDataFolder(), "phasing/phasingData.yml");
        try {
            YamlConfiguration config = loadYamlUtf8(phasingFile);
            config.set("npcs." + id + ".name", name);
            config.set("npcs." + id + ".title", title);
            config.set("npcs." + id + ".permission", permission);
            config.save(phasingFile);
        }catch(Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save NPC to phasingData.yml: " + id, e);
        }
    }
    public void removePhasedNPC(String id) {
        File phasingFile = new File(plugin.getDataFolder(), "phasing/phasingData.yml");
        try {
            YamlConfiguration config = loadYamlUtf8(phasingFile);
            config.set("npcs." + id, null);
            config.save(phasingFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove NPC from phasingData.yml: " + id, e);
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
}
