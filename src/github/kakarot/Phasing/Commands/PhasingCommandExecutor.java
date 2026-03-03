package github.kakarot.Phasing.Commands;

import com.spacey.kakarotmod.api.KakarotModAPI;
import github.kakarot.Main;
import github.kakarot.Phasing.Cache.PhasingCache;
import github.kakarot.Phasing.Models.PhasedNPC;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class PhasingCommandExecutor implements CommandExecutor {
    private final Main plugin;
    public PhasingCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("kakarot.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "check":
                return handleCheck(sender, args);
            case "hideclient":
                return handleHideClient(sender, args);
            case "showclient":
                return handleShowClient(sender, args);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Phasing System Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/phasing add <id> <permission> <name> [title...] " + ChatColor.WHITE + "- Add NPC to YML");
        sender.sendMessage(ChatColor.YELLOW + "/phasing remove <id> " + ChatColor.WHITE + "- Remove NPC from YML");
        sender.sendMessage(ChatColor.YELLOW + "/phasing check <name> " + ChatColor.WHITE + "- Check if NPC is in phasing system");
        sender.sendMessage(ChatColor.AQUA + "/phasing hideclient <player> <name> [title...] " + ChatColor.WHITE + "- Force hide via API");
        sender.sendMessage(ChatColor.AQUA + "/phasing showclient <player> <name> [title...] " + ChatColor.WHITE + "- Force show via API");
    }
    //Usage: /phasing add quest_veg_1 kakarot.quest.goku Vegeta Prince of all Saiyans
    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /phasing add <id> <permission> <name> [title...]");
            return true;
        }
        String id = args[1];
        String permission = args[2];
        String name = args[3];
        name = name.replace("_", " ");
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 4; i < args.length; i++) {
            titleBuilder.append(args[i]).append(" ");
        }
        String title = titleBuilder.toString().trim();
        PhasingCache.add(new PhasedNPC(id, name, title, permission));
        plugin.getPhasingConfigManager().savePhasedNPC(id, name, title, permission);
        sender.sendMessage(ChatColor.GREEN + "Successfully added phased NPC '" + name + "' to the system.");
        return true;
    }
    //Usage: /phasing remove quest_veg_1
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /phasing remove <id>");
            return true;
        }
        String id = args[1];
        if (PhasingCache.getAll().remove(id) != null) {
            plugin.getPhasingConfigManager().removePhasedNPC(id);
            sender.sendMessage(ChatColor.GREEN + "Removed phased NPC with ID: " + id);
        } else {
            sender.sendMessage(ChatColor.RED + "No phased NPC found with ID: " + id);
            return false;
        }
        return true;
    }
    //Usage: /phasing check Vegeta
    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /phasing check <name>");
            return true;
        }
        String targetName = args[1];
        targetName = targetName.replace("_", " ");
        int count = 0;
        sender.sendMessage(ChatColor.GOLD + "Searching cache for NPCs named '" + targetName + "':");
        for (Map.Entry<String, PhasedNPC> entry : PhasingCache.getAll().entrySet()) {
            PhasedNPC npc = entry.getValue();
            if (npc.getName().equalsIgnoreCase(targetName)) {
                count++;
                String titleStr = npc.getTitle().isEmpty() ? "No Title" : npc.getTitle();
                sender.sendMessage(ChatColor.YELLOW + "- ID: " + entry.getKey() +
                        " | Title: " + titleStr +
                        " | Perm: " + npc.getPermission());
            }
        }
        if (count == 0) {
            sender.sendMessage(ChatColor.RED + "No NPCs found in the phasing system with that name.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Total found: " + count);
        }
        return true;
    }
    //Usage: /phasing hideclient PlayerName Vegeta Prince of all Saiyans
    private boolean handleHideClient(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /phasing hideclient <player> <npc_name> [title...]");
            return true;
        }
        String targetPlayer = args[1];
        String npcName = args[2];
        npcName = npcName.replace("_", " ");
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            titleBuilder.append(args[i]).append(" ");
        }
        String npcTitle = titleBuilder.toString().trim();
        boolean success = KakarotModAPI.hideNPC(targetPlayer, npcName, npcTitle);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Sent API request to hide '" + npcName + "' for player " + targetPlayer);
        } else {
            sender.sendMessage(ChatColor.RED + "Player not found or packet failed.");
        }
        return true;
    }
    //Usage: /phasing showclient PlayerName Vegeta Prince of all Saiyans
    private boolean handleShowClient(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /phasing showclient <player> <npc_name> [title...]");
            return true;
        }
        String targetPlayer = args[1];
        String npcName = args[2];
        npcName = npcName.replace("_", " ");
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            titleBuilder.append(args[i]).append(" ");
        }
        String npcTitle = titleBuilder.toString().trim();
        boolean success = KakarotModAPI.showNPC(targetPlayer, npcName, npcTitle);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Sent API request to show '" + npcName + "' for player " + targetPlayer);
        } else {
            sender.sendMessage(ChatColor.RED + "Player not found or packet failed.");
        }
        return true;
    }
}
