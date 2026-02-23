package github.kakarot.Quests.Commands;

import com.spacey.kakarotmod.api.KakarotModAPI;
import fr.minuskube.inv.SmartInvsPlugin;
import github.kakarot.Main;
import github.kakarot.Quests.GUI.QuestListGUI;
import github.kakarot.Quests.Managers.QuestManager;
import github.kakarot.Quests.Models.*;
import github.kakarot.Quests.Quest;
import github.kakarot.Tools.HexcodeUtils;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
public class QuestCommands implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) { //TODO: "Requeriments" for objectives (can't be completed before finishing the others)
        String AVAILABLE_COMMANDS = "list, info, track, untrack, add, complete, debug";
        if(args.length > 0 && sender.hasPermission("kakarot.quest.admin")) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "add":
                    if (args.length < 3) {
                        sender.sendMessage("§cUsage: /quest add <player> <quest_id>");
                        return true;
                    }
                    addQuestToPlayer(sender, args[1], args[2]);
                    return true;
                case "complete":
                    if (args.length < 3) {
                        sender.sendMessage("§cUsage: /quest complete <player> <quest_id>");
                        return true;
                    }
                    completeQuestForPlayer(sender, args[1], args[2]);
                    return true;
                case "debug":
                    if(args.length < 2) {
                        sender.sendMessage("§cUsage: /quest debug <reload>");
                        return true;
                    }
                    if(args[1].equalsIgnoreCase("reload")) reloadQuests(sender);
                    return true;
            }
            if(!(sender instanceof Player)) {
                sender.sendMessage("§cUnknown subcommand: " + subcommand);
                sender.sendMessage("§aAvailable: " + AVAILABLE_COMMANDS);
                return true;
            }
        }
        if(!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        if(args.length == 0) {
            showQuestList(player);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch(subCommand) {
            case "language":
                if(args.length < 2) {
                    player.sendMessage("§cUsage: /quest language <new language>\nValues: 'es', 'en'");
                    return true;
                }
                if(!args[1].equalsIgnoreCase("en") && !args[1].equalsIgnoreCase("es")) {
                    player.sendMessage("§cValues: 'es', 'en'");
                    return true;
                }
                String newLanguage = args[1].toLowerCase();
                handleSetLanguageCommand(player, newLanguage);
                break;
            case "list":
                showQuestList(player);
                break;
            case "info":
                if(args.length < 2) {
                    player.sendMessage("§cUsage: /quest info <quest_id>");
                    return true;
                }
                showQuestInfo(player, args[1]);
                break;
            case "track":
                if(args.length < 2) {
                    player.sendMessage("§cUsage: /quest track <quest_id>");
                    return true;
                }
                trackQuest(player, args[1]);
                break;
            case "untrack":
                untrackQuest(player);
                break;
            default:
                player.sendMessage("§Unknown subcommand: " + subCommand);
                player.sendMessage("§aAvailable: " + AVAILABLE_COMMANDS);
        }
        return true;
    }
    private void showQuestList(Player player) {
        QuestListGUI.INVENTORY.open(player);
        player.sendMessage("§aOpening up quests GUI...");
    }
    private void showQuestListText(Player player) {
        QuestManager questManager = Main.instance.getQuestManager();
        Map<Integer, PlayerQuestProgress> playerQuests = questManager.getPlayerQuests(player.getUniqueId());
        if(playerQuests.isEmpty()) {
            player.sendMessage("§cYou don't have any quests yet.");
            return;
        }
        player.sendMessage("§8§m─────────────────────────────");
        for(Map.Entry<Integer, PlayerQuestProgress> entry : playerQuests.entrySet()) {
            int questId = entry.getKey();
            PlayerQuestProgress progress = entry.getValue();
            Quest quest = questManager.getQuest(questId);
            if(quest == null) return;
            String statusColor;
            String statusText;
            switch(progress.getStatus()) {
                case IN_PROGRESS:
                    statusColor = "§e";
                    statusText = "In Progress";
                    break;
                case COMPLETED:
                    if(quest.isRepeatable() && progress.canRepeat()) {
                        statusColor = "§a";
                        statusText = "Can repeat";
                    }else {
                        statusColor = "§7";
                        statusText = "Completed";
                    }
                    break;
                default:
                    statusColor = "§c";
                    statusText = "Unknown";
            }
            int percentage = progress.getTotalProgressPercentage(quest);
            String progressBar = getProgressBar(percentage);
            player.sendMessage("§f#" + questId + " §7- §f" + quest.getName("es"));
            player.sendMessage(statusColor + "● " + statusText + " §7" + progressBar);
        }
        player.sendMessage("§8§m─────────────────────────────");
        player.sendMessage("§7Use §f/quest info <id> §7for details");
    }
    private void showQuestInfo(Player player, String questIdStr) { //TODO: better quest info (show skull if kill, book if interact and so on)
        int questId;
        try {
            questId = Integer.parseInt(questIdStr);
        }catch(NumberFormatException e) {
            player.sendMessage("§cInvalid quest ID");
            return;
        }
        QuestManager questManager = Main.instance.getQuestManager();
        Quest quest = questManager.getQuest(questId);
        if(quest == null) {
            player.sendMessage("§cQuest #" + questId + " not found!");
            return;
        }
        if(!questManager.hasPickedUpQuest(player.getUniqueId(), questId) && !player.hasPermission("kakarot.quest.admin")) {
            player.sendMessage("§cYou can't see information for Quest #" + questIdStr + ". §cAccept it first!");
            return;
        }
        PlayerQuestProgress progress = questManager.getPlayerQuestProgress(player.getUniqueId(), questId);
        String playerLocale = Main.instance.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
        player.sendMessage("§8§m─────────────────────────────");
        player.sendMessage("§6§l " + quest.getName().getOrDefault(playerLocale, "Misión") + " §7(#" + questId + ")");
        player.sendMessage("§f" + quest.getDescription().getOrDefault(playerLocale, ""));
        player.sendMessage("");
        player.sendMessage("§e§lObjectives:");
        for(int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective obj = quest.getObjectives().get(i);
            int currentProgress = progress != null ? progress.getObjectiveProgress()[i] : 0;
            boolean complete = obj.isComplete(currentProgress);
            String checkmark = complete ? "§a✓" : "§7○";
            String objMark;
            switch(obj.getType()) {
                case KILL_MOBS:
                    objMark = "§c[KILL]";
                    break;
                case TALK_TO_NPC:
                    objMark = "§a[TALK]";
                    break;
                case COLLECT_ITEMS:
                    objMark = "§e[COLLECT]";
                    break;
                default:
                    objMark = "§9[CUSTOM]";
                    break;
            }
            String progressText = "§7(" + currentProgress + "/" + obj.getRequired() + ")";
            player.sendMessage(checkmark + " §f" + obj.getObjectiveInfo().getTarget() + " " + progressText + " " + objMark);
        }
        player.sendMessage("");
        player.sendMessage("§a§lRewards:");
        for(QuestReward questReward : quest.getRewards()) {
            player.sendMessage("§a+ §f" + getRewardDescription(playerLocale, questReward));
        }
        if(quest.isRepeatable()) {
            player.sendMessage("");
            player.sendMessage("§7Repeatable every §f" + formatCooldown(quest.getRepeatCooldown()));
        }
        player.sendMessage("§8§m─────────────────────────────");
        if(progress != null && progress.getStatus() == QuestStatus.IN_PROGRESS) {
            player.sendMessage("§7Use §f/quest track " + questIdStr + " §7to track this quest");
        }
    }
    private void trackQuest(Player player, String questIdStr) {
        int questId;
        try {
            questId = Integer.parseInt(questIdStr);
        }catch(NumberFormatException e) {
            player.sendMessage("§cInvalid quest ID");
            return;
        }
        QuestManager questManager = Main.instance.getQuestManager();
        Quest quest = questManager.getQuest(questId);
        if(quest == null) {
            player.sendMessage("§cThis quest doesn't exist");
            return;
        }
        if(!questManager.hasPickedUpQuest(player.getUniqueId(), questId)) {
            player.sendMessage("§cYou don't have this quest");
            return;
        }
        if(questManager.hasCompletedQuest(player.getUniqueId(), questId)) {
            player.sendMessage("§cYou can't track this quest. Already completed");
            return;
        }
        PlayerQuestProgress progress = questManager.getPlayerQuestProgress(player.getUniqueId(), questId);
        int nonCompletedObjIndex = -1;
        int objectiveCount = Math.min(quest.getObjectiveCount(), progress.getObjectiveProgress().length);
        for(int i = 0; i < objectiveCount; i++) {
            int objProgress = progress.getObjectiveProgress()[i];
            if(objProgress < quest.getObjectives().get(i).getRequired()) {
                nonCompletedObjIndex = i;
                break;
            }
        }
        if(nonCompletedObjIndex == -1) {
            KakarotModAPI.clearQuestTarget(player.getName());
            return;
        }
        QuestObjective obj = quest.getObjectives().get(nonCompletedObjIndex);
        TrackingInfo info = obj.getTrackingInfo();
        if(info == null) return;
        String playerLocale = Main.instance.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
        String label = info.getLabel().getOrDefault(playerLocale, "");
        label = label.replace("%current_progress%", String.valueOf(progress.getObjectiveProgress()[nonCompletedObjIndex])).replace("%required%", String.valueOf(obj.getRequired()));
        KakarotModAPI.setQuestTarget(player.getName(), info.getX(), info.getY(), info.getZ(), label, HexcodeUtils.parseColor(info.getArrowColor()), HexcodeUtils.parseColor(info.getLabelColor()));
        player.sendMessage("§a✓ Now tracking quest #" + questIdStr);
    }
    private void untrackQuest(Player player) {
        player.sendMessage("§a✓ Stopped tracking quest");
    }
    private void addQuestToPlayer(CommandSender sender, String playerName, String questIdStr) {
        Player target = Bukkit.getPlayer(playerName);
        if(target == null || !target.isOnline()) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return;
        }
        int questId;
        try {
            questId = Integer.parseInt(questIdStr);
        }catch(NumberFormatException e) {
            sender.sendMessage("§cInvalid quest ID");
            return;
        }
        QuestManager questManager = Main.instance.getQuestManager();
        Quest quest = questManager.getQuest(questId);
        if(quest == null) {
            sender.sendMessage("§cQuest #" + questIdStr + " not found");
            return;
        }
        if(questManager.hasPickedUpQuest(target.getUniqueId(), questId)) {
            //Check if quest is repeatable and player can do it again
            if(quest.isRepeatable() && questManager.hasCompletedQuest(target.getUniqueId(), questId)) {
                if(questManager.canRepeat(target.getUniqueId(), questId)) questManager.addQuestToPlayer(target.getUniqueId(), questId);
                else target.sendMessage("You can't pick this quest up yet!, you have to wait " + questManager.getRemainingCooldown(target.getUniqueId(), questId));
                return;
            }
            if(!quest.isRepeatable()) target.sendMessage("This quest is not repeatable.");
            else target.sendMessage("You can't repeat this quest yet.");
            sender.sendMessage("Player already has this quest");
            return;
        }
        questManager.addQuestToPlayer(target.getUniqueId(), questId);
        sender.sendMessage("§aAdded quest #" + questIdStr + " to " + playerName);
    }
    private void completeQuestForPlayer(CommandSender sender, String playerName, String questIdStr) {
        Player target = Bukkit.getPlayer(playerName);
        if(target == null) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return;
        }
        int questId;
        try {
            questId = Integer.parseInt(questIdStr);
        }catch(NumberFormatException e) {
            sender.sendMessage("§cInvalid quest ID");
            return;
        }
        QuestManager questManager = Main.instance.getQuestManager();
        Quest quest = questManager.getQuest(questId);
        if(quest == null) {
            sender.sendMessage("§cQuest #" + questIdStr + " not found");
            return;
        }
        questManager.completeQuest(target.getUniqueId(), questId);
        sender.sendMessage("§aCompleted quest #" + questIdStr + " for player " + playerName);
    }
    private void reloadQuests(CommandSender sender) {
        QuestManager questManager = Main.instance.getQuestManager();
        questManager.reloadQuests();
        sender.sendMessage("§aReloaded quests from YAML");
    }
    private String getRewardDescription(String locale, QuestReward reward) {

        return reward.getDescription().getOrDefault(locale, "");
    }
    private String getProgressBar(int percentage) {
        int bars = 10;
        double filled = ((double) percentage / 100) * bars;
        StringBuilder bar = new StringBuilder("§a");
        for(int i = 0; i < bars; i++) {
            if(i < filled) {
                bar.append("█");
            }else bar.append("§7░");
        }
        bar.append(" §f(").append(percentage).append("%)");
        return bar.toString();
    }
    private String formatCooldown(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if(days >= 1) {
            return days + " day" + (days > 1 ? "s" : "");
        }else if(hours >= 1) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        }else if(minutes >= 1) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        }else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("list");
            completions.add("info");
            completions.add("track");
            completions.add("untrack");
            completions.add("language");
            if (sender.hasPermission("kakarot.quest.admin")) {
                completions.add("add");
                completions.add("complete");
                completions.add("debug");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        QuestManager questManager = Main.instance.getQuestManager();
        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("info") || subCmd.equals("track")) {
                for (int id : questManager.getAllQuests().keySet()) {
                    completions.add(String.valueOf(id));
                }
                return completions;
            }
            if ((subCmd.equals("add") || subCmd.equals("complete")) && sender.hasPermission("kakarot.quest.admin")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("debug") && sender.hasPermission("kakarot.quest.admin")) {
                completions.add("reload");
                return completions;
            }
        }
        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            if ((subCmd.equals("add") || subCmd.equals("complete")) && sender.hasPermission("kakarot.quest.admin")) {
                for (int id : questManager.getAllQuests().keySet()) {
                    completions.add(String.valueOf(id));
                }
                return completions;
            }
        }
        return completions;
    }
    private void handleSetLanguageCommand(Player player, String newLanguage) {
        Main.instance.getSettingsManager().setPlayerLanguage(player.getUniqueId(), newLanguage);
        player.sendMessage("§aLanguage changed to " + newLanguage + " correctly");
    }
}
