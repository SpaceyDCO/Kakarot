package github.kakarot.Quests.Commands;

import github.kakarot.Main;
import github.kakarot.Quests.Managers.QuestManager;
import github.kakarot.Quests.Models.PlayerQuestProgress;
import github.kakarot.Quests.Models.QuestObjective;
import github.kakarot.Quests.Models.QuestReward;
import github.kakarot.Quests.Models.QuestStatus;
import github.kakarot.Quests.Quest;
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
                    //TODO: add player online check
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
    private void showQuestInfo(Player player, String questIdStr) {
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
            player.sendMessage("&cQuest #" + questId + " not found!");
            //TODO: add a check so players can query latest quests or make them query only collected quests
            return;
        }
        PlayerQuestProgress progress = questManager.getPlayerQuestProgress(player.getUniqueId(), questId);
        player.sendMessage("§8§m─────────────────────────────");
        player.sendMessage("§6§l " + quest.getName("es") + " §7(#" + questId + ")");
        player.sendMessage("§f" + quest.getDescription("es"));
        player.sendMessage("");
        player.sendMessage("§e§lObjectives:");
        for(int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective obj = quest.getObjectives().get(i);
            int currentProgress = progress != null ? progress.getObjectiveProgress()[i] : 0;
            boolean complete = obj.isComplete(currentProgress);
            String checkmark = complete ? "§a✓" : "§7○";
            String progressText = "§7(" + currentProgress + "/" + obj.getRequired() + ")";
            player.sendMessage(checkmark + " §f" + obj.getTarget() + " " + progressText);
            player.sendMessage("");
            player.sendMessage("§a§lRewards:");
            for(QuestReward questReward : quest.getRewards()) {
                player.sendMessage("§a+ §f" + getRewardDescription("es", questReward));
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
        if(!questManager.hasPickedUpQuest(player.getUniqueId(), questId)) {
            player.sendMessage("§You don't have this quest");
            return;
        }
        //TODO: Implement scoreboard tracking
        player.sendMessage("§a✓ Now tracking quest #" + questIdStr);
    }
    private void untrackQuest(Player player) {
        player.sendMessage("§a✓ Stopped tracking quest");
    }
    private void addQuestToPlayer(CommandSender sender, String playerName, String questIdStr) {
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
        if(questManager.hasPickedUpQuest(target.getUniqueId(), questId)) {
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
        return "locale " + locale + " TODO: add reward description"; //TODO: reward description
    }
    private String getProgressBar(int percentage) {
        int bars = 10;
        int filled = (percentage / 100) * bars;
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
}
