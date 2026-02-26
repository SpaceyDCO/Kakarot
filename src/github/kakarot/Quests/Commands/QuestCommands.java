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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
public class QuestCommands implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
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
        QuestManager questManager = Main.instance.getQuestManager();
        switch(subCommand) {
            case "language":
                if(args.length < 2) {
                    player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.correct_usage", "%usage%", "/quest language <en, es> "));
                    return true;
                }
                if(!args[1].equalsIgnoreCase("en") && !args[1].equalsIgnoreCase("es")) {
                    player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.invalid-language"));
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
                    player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.correct_usage", "%usage%", "/quest info <quest_id>"));
                    return true;
                }
                showQuestInfo(player, args[1]);
                break;
            case "track":
                if(args.length < 2) {
                    player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.correct_usage", "%usage%", "/quest track <quest_id>"));
                    return true;
                }
                trackQuest(player, args[1]);
                break;
            case "untrack":
                untrackQuest(player);
                break;
            default:
                player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.unknown-command", "%command%", subCommand));
        }
        return true;
    }
    private void showQuestList(Player player) {
        QuestListGUI.INVENTORY.open(player);
        player.sendMessage(Main.instance.getQuestManager().getLangMessage(player.getUniqueId(), "commands.opening-gui"));
    }
//    private void showQuestListText(Player player) {
//        QuestManager questManager = Main.instance.getQuestManager();
//        Map<Integer, PlayerQuestProgress> playerQuests = questManager.getPlayerQuests(player.getUniqueId());
//        if(playerQuests.isEmpty()) {
//            player.sendMessage("§cYou don't have any quests yet.");
//            return;
//        }
//        player.sendMessage("§8§m─────────────────────────────");
//        for(Map.Entry<Integer, PlayerQuestProgress> entry : playerQuests.entrySet()) {
//            int questId = entry.getKey();
//            PlayerQuestProgress progress = entry.getValue();
//            Quest quest = questManager.getQuest(questId);
//            if(quest == null) return;
//            String statusColor;
//            String statusText;
//            switch(progress.getStatus()) {
//                case IN_PROGRESS:
//                    statusColor = "§e";
//                    statusText = "In Progress";
//                    break;
//                case COMPLETED:
//                    if(quest.isRepeatable() && progress.canRepeat()) {
//                        statusColor = "§a";
//                        statusText = "Can repeat";
//                    }else {
//                        statusColor = "§7";
//                        statusText = "Completed";
//                    }
//                    break;
//                default:
//                    statusColor = "§c";
//                    statusText = "Unknown";
//            }
//            int percentage = progress.getTotalProgressPercentage(quest);
//            String progressBar = getProgressBar(percentage);
//            player.sendMessage("§f#" + questId + " §7- §f" + quest.getName("es"));
//            player.sendMessage(statusColor + "● " + statusText + " §7" + progressBar);
//        }
//        player.sendMessage("§8§m─────────────────────────────");
//        player.sendMessage("§7Use §f/quest info <id> §7for details");
//    }
    private void showQuestInfo(Player player, String questIdStr) {
        QuestManager questManager = Main.instance.getQuestManager();
        int questId;
        try {
            questId = Integer.parseInt(questIdStr);
        }catch(NumberFormatException e) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.invalid-id"));
            return;
        }
        Quest quest = questManager.getQuest(questId);
        if(quest == null) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.invalid-id"));
            return;
        }
        if(!questManager.hasPickedUpQuest(player.getUniqueId(), questId) && !player.hasPermission("kakarot.quest.admin")) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.no-info-allowed"));
            return;
        }
        PlayerQuestProgress progress = questManager.getPlayerQuestProgress(player.getUniqueId(), questId);
        String playerLocale = Main.instance.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
        player.sendMessage("§8§m─────────────────────────────");
        player.sendMessage("§6§l" + quest.getName().getOrDefault(playerLocale, "Misión") + " §7(#" + questId + ")");
        player.sendMessage("§f" + quest.getDescription().getOrDefault(playerLocale, ""));
        player.sendMessage("");
        player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.info-objectives"));
        for(int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective obj = quest.getObjectives().get(i);
            int currentProgress = progress != null ? progress.getObjectiveProgress()[i] : 0;
            boolean complete = obj.isComplete(currentProgress);
            String checkmark = complete ? "§a✓" : "§7○";
            String objMark;
            String target = obj.getObjectiveInfo().getPlaceholderName() != null && !obj.getObjectiveInfo().getPlaceholderName().isEmpty()
                    ? obj.getObjectiveInfo().getPlaceholderName().replace("&", "§") : obj.getObjectiveInfo().getTarget();
            switch(obj.getType()) {
                case KILL_MOBS:
                    objMark = questManager.getLangMessage(player.getUniqueId(), "commands.info-objective-kill");
                    break;
                case TALK_TO_NPC:
                    objMark = questManager.getLangMessage(player.getUniqueId(), "commands.info-objective-talk");
                    break;
                case COLLECT_ITEMS:
                    objMark = questManager.getLangMessage(player.getUniqueId(), "commands.info-objective-collect");
                    break;
                default:
                    objMark = questManager.getLangMessage(player.getUniqueId(), "commands.info-objective-custom");
                    break;
            }
            String progressText = "§7(" + currentProgress + "/" + obj.getRequired() + ")";
            player.sendMessage(checkmark + " §f" + target + " " + progressText + " " + objMark);
        }
        player.sendMessage("");
        player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.info-rewards"));
        for(QuestReward questReward : quest.getRewards()) {
            player.sendMessage("§a+ §f" + getRewardDescription(playerLocale, questReward));
        }
        if(quest.isRepeatable()) {
            player.sendMessage("");
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.info-cooldown", "%cooldown%", formatCooldown(quest.getRepeatCooldown())));
        }
        player.sendMessage("§8§m─────────────────────────────");
        if(progress != null && progress.getStatus() == QuestStatus.IN_PROGRESS) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.info-track-suggestion", "%quest_id%", questIdStr));
        }
    }
    private void trackQuest(Player player, String questIdStr) {
        QuestManager questManager = Main.instance.getQuestManager();
        int questId;
        try {
            questId = Integer.parseInt(questIdStr);
        }catch(NumberFormatException e) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.invalid-id"));
            return;
        }
        Quest quest = questManager.getQuest(questId);
        if(quest == null) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.invalid-id"));
            return;
        }
        if(!questManager.hasPickedUpQuest(player.getUniqueId(), questId)) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.cant_track"));
            return;
        }
        if(questManager.hasCompletedQuest(player.getUniqueId(), questId)) {
            player.sendMessage(questManager.getLangMessage(player.getUniqueId(), "commands.already_completed"));
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
        //If quest is completed and is not TURN IN, clear quest arrow
        if(nonCompletedObjIndex == -1 && !quest.isTurnIn()) {
            KakarotModAPI.clearQuestTarget(player.getName());
            return;
        }
        //If quest is completed BUT is turn in, instead render quest completed arrow...
        if(nonCompletedObjIndex == -1) {
            NpcTurnInDetails details = quest.getNpcTurnInDetails();
            KakarotModAPI.setQuestTarget(player.getName(), details.getX(), details.getY(), details.getZ(), questManager.getLangMessage(player.getUniqueId(), "manager.quest_completed_label"), HexcodeUtils.parseColor(details.getArrowColor()));
            return;
        }
        QuestObjective obj = quest.getObjectives().get(nonCompletedObjIndex);
        TrackingInfo info = obj.getTrackingInfo();
        if(info == null) return;
        String playerLocale = Main.instance.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
        String label = info.getLabel().getOrDefault(playerLocale, "");
        label = label.replace("%current_progress%", String.valueOf(progress.getObjectiveProgress()[nonCompletedObjIndex])).replace("%required%", String.valueOf(obj.getRequired()));
        KakarotModAPI.setQuestTarget(player.getName(), info.getX(), info.getY(), info.getZ(), label, HexcodeUtils.parseColor(info.getArrowColor()), HexcodeUtils.parseColor(info.getLabelColor()));
        String trackingMsg = questManager.getLangMessage(player.getUniqueId(), "commands.now_tracking", "%quest_name%", quest.getName(playerLocale));
        player.sendMessage(trackingMsg);
        Main.instance.getQuestManager().playerQuestTrack.put(player.getUniqueId(), questId);
    }
    private void untrackQuest(Player player) {
        KakarotModAPI.clearQuestTarget(player.getName());
        Main.instance.getQuestManager().playerQuestTrack.remove(player.getUniqueId());
        player.sendMessage(Main.instance.getQuestManager().getLangMessage(player.getUniqueId(), "commands.stop_tracking"));
    }
    private void addQuestToPlayer(CommandSender sender, String playerName, String questIdStr) {
        Player target = Bukkit.getPlayer(playerName);
        QuestManager questManager = Main.instance.getQuestManager();
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
        Quest quest = questManager.getQuest(questId);
        if(quest == null) {
            sender.sendMessage("§cQuest #" + questIdStr + " not found");
            return;
        }
        if(questManager.hasPickedUpQuest(target.getUniqueId(), questId)) {
            //Check if quest is repeatable and player can do it again
            if(quest.isRepeatable() && questManager.hasCompletedQuest(target.getUniqueId(), questId)) {
                if(questManager.canRepeat(target.getUniqueId(), questId)) questManager.addQuestToPlayer(target.getUniqueId(), questId);
                else target.sendMessage(questManager.getLangMessage(target.getUniqueId(), "manager.on_cooldown", "%time%", questManager.getRemainingCooldown(target.getUniqueId(), questId)));
                return;
            }
            if(!quest.isRepeatable()) target.sendMessage(questManager.getLangMessage(target.getUniqueId(), "commands.not-repeatable"));
            else target.sendMessage(questManager.getLangMessage(target.getUniqueId(), "commands.not-repeatable-yet"));
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
        player.sendMessage(Main.instance.getQuestManager().getLangMessage(player.getUniqueId(), "commands.language-set", "%new_language%", newLanguage));
    }
}
