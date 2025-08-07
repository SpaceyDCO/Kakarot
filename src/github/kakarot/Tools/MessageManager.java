package github.kakarot.Tools;

import github.kakarot.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {
    private final Main plugin;
    private FileConfiguration partyMessages;
    private FileConfiguration raidMessages;
    public MessageManager(Main plugin) {
        this.plugin = plugin;
        reloadMessages();
    }

    public void reloadMessages() {
        reloadMessages(true, true);
    }
    public void reloadMessages(boolean party, boolean raid) {
        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if(!messagesDir.exists()) {
            if(messagesDir.mkdirs()) plugin.getLogger().info("Successfully created messages folder.");
            else {
                plugin.getLogger().severe("ERROR: Could not create messages folder. Maybe a permissions error?");
                return;
            }
        }
        if(party) partyMessages = loadConfig("messages/party_messages.yml");
        if(raid) raidMessages = loadConfig("messages/raid_messages.yml");
    }

    public String getMessage(String key, String... replacements) {
        return getMessage(key, true, replacements);
    }
    public String getMessage(String key, boolean prefix, String... replacements) {
        String message;
        if(partyMessages.isSet(key)) {
            message = partyMessages.getString(key);
            if(!key.equalsIgnoreCase("prefix") && prefix) {
                message = partyMessages.getString("prefix") + message;
            }
        }else if(raidMessages.isSet(key)) {
            message = raidMessages.getString(key);
            if(!key.equalsIgnoreCase("prefix") && prefix) {
                message = raidMessages.getString("prefix") + message;
            }
        }else {
            return CC.translate("&cMissing message key: " + key);
        }
        for(int i = 0; i < replacements.length; i += 2) {
            if((i+1) < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i+1]);
            }
        }
        return CC.translate(message);
    }

    private FileConfiguration loadConfig(String filePath) {
        File file = new File(plugin.getDataFolder(), filePath);
        if(!file.exists()) {
            plugin.saveResource(filePath, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

}
