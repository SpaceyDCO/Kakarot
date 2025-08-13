package github.kakarot;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import github.kakarot.Parties.Events.PlayerLeavePartyEvent;
import github.kakarot.Parties.Listeners.PlayerChat;
import github.kakarot.Parties.Managers.IPartyManager;
import github.kakarot.Parties.Managers.PartyManager;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Listeners.GameListener;
import github.kakarot.Raids.Managers.ConfigManager;
import github.kakarot.Raids.Managers.RaidManager;
import github.kakarot.Tools.ClassesRegistration;
import github.kakarot.Tools.Commands.CommandFramework;
import github.kakarot.Tools.MessageManager;
import github.kakarot.Tools.PacketHandler.IPacketHandler;
import github.kakarot.Tools.PacketHandler.PacketHandler;
import github.kakarot.Trivias.TriviaDataHandler;
import github.kakarot.Trivias.TriviasData;
import lombok.Getter;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.handler.ICloneHandler;
import noppes.npcs.scripted.NpcAPI;
import noppes.npcs.scripted.event.NpcEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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

    //Parties
    private IPartyManager partyManager;
    private PlayerChat playerChatEvent;
    //Parties

    //Arenas
    @Getter private ConfigManager configManager;
    @Getter private RaidManager raidManager;
    @Getter private MessageManager messageManager;
    private GameListener gameListener;
    //Arenas

    @Getter private IPacketHandler packetHandler;

    private final CommandFramework commandFramework = new CommandFramework(this);
    private final ClassesRegistration classesRegistration = new ClassesRegistration();

    @Override
    public void onEnable() {
        instance = this;
        this.messageManager = new MessageManager(this);

        //Parties
        this.partyManager = new PartyManager(this, this.messageManager);
        loadPartyEvents();
        //Parties

        //Trivia
        //readTriviaConfig();
        //TriviasRunnable.runnableTrivias.runTaskTimer(this, TriviasRunnable.TriviaCooldown, TriviasRunnable.TriviaCooldown); Disabled trivia runnable
        activeTrivia = false;
        //Trivia

        //Arenas
        this.configManager = new ConfigManager(this);
        this.configManager.loadAllConfig();
        getServer().getScheduler().runTaskLater(this, this::cleanUpNpcs, 40L);
        this.raidManager = new RaidManager(this, this.configManager, this.partyManager, this.messageManager);
        this.gameListener = new GameListener(this, this.raidManager);
        getServer().getPluginManager().registerEvents(this.gameListener, this);
        //Arenas

        this.packetHandler = new PacketHandler();
        classesRegistration.loadCommands("github.kakarot.Commands");
        Bukkit.getConsoleSender().sendMessage("Activated plugin Kakarot");
        Bukkit.getConsoleSender().sendMessage("By: SpaceyDCO");
    }
    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("Deactivating plugin Kakarot");

        //Trivia
        //TriviasRunnable.runnableTrivias.cancel();
        //Trivia

        //Parties
        unloadPartyEvents();
        //Parties

        //Arenas
        PlayerDeathEvent.getHandlerList().unregister(this.gameListener);
        PlayerQuitEvent.getHandlerList().unregister(this.gameListener);
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this.gameListener);
        PlayerLeavePartyEvent.getHandlerList().unregister(this.gameListener);
        this.raidManager.cleanupArenas();
        //Arenas
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

    //PARTIES
    private void loadPartyEvents() {
        this.playerChatEvent = new PlayerChat(this);
        getServer().getPluginManager().registerEvents(this.playerChatEvent, this);
    }
    private void unloadPartyEvents() {
        AsyncPlayerChatEvent.getHandlerList().unregister(this.playerChatEvent);
    }
    //PARTIES

    private void cleanUpNpcs() {
        getLogger().info("Starting npc's clean up...");
        int cleanedCount = 0;
        for(Arena arena : this.configManager.getArenas()) {
            World world = Bukkit.getWorld(arena.getWorldName());
            if(world == null) {
                getLogger().warning("Could not clean up arena " + arena.getArenaName() + " because its world " + arena.getWorldName() + " is not loaded. Skipping...");
                continue;
            }
            Location corner1 = arena.getBoundaryCorner1().toBukkitLocation(arena.getWorldName());
            Location corner2 = arena.getBoundaryCorner2().toBukkitLocation(arena.getWorldName());
            int minX = corner1.getBlockX() >> 4;
            int maxX = corner2.getBlockX() >> 4;
            int minZ = corner1.getBlockZ() >> 4;
            int maxZ = corner2.getBlockZ() >> 4;
            if(minX > maxX) {
                int temp = minX;
                minX = maxX;
                maxX = temp;
            }
            if(minZ > maxZ) {
                int temp = minZ;
                minZ = maxZ;
                maxZ = temp;
            }
            getLogger().info("Scanning arena " + arena.getArenaName() + "...");
            for(int x = minX; x <= maxX; x++) {
                for(int z = minZ; z <= maxZ; z++) {
                    if(!world.isChunkLoaded(x, z)) {
                        world.loadChunk(x, z);
                    }
                    for(IEntity<?> npcEntity : NpcAPI.Instance().getLoadedEntities()) {
                        if(npcEntity instanceof ICustomNpc<?>) {
                            ICustomNpc<?> npc = (ICustomNpc<?>) npcEntity;
                            if(npc.hasStoredData("SPACEY_ARENA_SYSTEM_NPC")) {
                                if(!npc.isAlive()) continue;
                                getLogger().info("Found ghost NPC '" + npc.getName() + "' with stored data belonging to arena system " + npc.getStoredData("SPACEY_ARENA_SYSTEM_NPC") + "." + " Killing this npc...");
                                String nameToSave = "GHOST_NPC_" + npc.getName();
                                ICloneHandler iCloneHandler = NpcAPI.Instance().getClones();
                                if(iCloneHandler.has(10, nameToSave)) {
                                    getLogger().info("Tried to save NPC to cloner tool tab 10 under name " + nameToSave + " but it already exists.");
                                }else {
                                    getLogger().info("Killing (not de-spawning) npc " + npc.getName() + " as it's tagged as a ghost NPC...");
                                    getLogger().info("This npc will also be saved to cloner tool tab 10 under name " + nameToSave + " in case it was tagged accidentally.");
                                    iCloneHandler.set(10, nameToSave, npc);
                                }
                                cleanedCount++;
                                npc.kill();
                            }
                        }
                    }
                }
            }
        }
        if(cleanedCount > 0) getLogger().info("Successfully cleaned up " + cleanedCount + " ghost npc(s).");
        else getLogger().info("There were no ghost npcs to clean up.");
    }
    //RAIDS CNPC EVENTS
    public void onNpcDiedEvent(NpcEvent.DiedEvent event) {
        this.gameListener.onNpcDied(event);
    }
    public void onNpcDied(ICustomNpc<?> npc) {
        this.gameListener.npcDied(npc);
    }
    public void onNpcDamagedEvent(NpcEvent.DamagedEvent event) {
        this.gameListener.onNpcDamaged(event);
    }
    //RAIDS CNPC EVENTS
}