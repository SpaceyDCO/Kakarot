package github.kakarot.Commands;

import github.kakarot.Main;
import github.kakarot.Raids.Managers.RaidManager;
import github.kakarot.Tools.CC;
import github.kakarot.Tools.Commands.BaseCommand;
import github.kakarot.Tools.Commands.Command;
import github.kakarot.Tools.Commands.CommandArgs;
import github.kakarot.Tools.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class RaidCommands extends BaseCommand {
    private final MessageManager messageManager = Main.instance.getMessageManager();
    private final RaidManager raidManager = Main.instance.getRaidManager();
    @Override
    @Command(name = "raids", aliases = "raid", isAdminOnly = true)
    public void onCommand(CommandArgs command) throws IOException {
        CommandSender sender = command.getSender();
        String[] args = command.getArgs();
        Player player = (Player) sender;
        if(args.length == 0) {
            player.sendMessage(CC.translate("&5======== Commands ========"));
            player.sendMessage(CC.translate("&d/raid reload"));
            player.sendMessage(CC.translate("&d/raid reloadArena <arena_name>"));
            player.sendMessage(CC.translate("&d/raid reloadScenario <scenario_name>"));
            return;
        }
        switch(args[0].toLowerCase()) {
            case "reload":
                handleReloadCommand(player);
                break;
            case "reloadarena":
                handleArenaReloadCommand(sender, args);
                break;
            case "reloadscenario":
                handleScenarioReloadCommand(sender, args);
                break;
            default:
                player.sendMessage(CC.translate("&5======== Commands ========"));
                player.sendMessage(CC.translate("&d/raid reload"));
                player.sendMessage(CC.translate("&d/raid reloadArena <arena_name>"));
                player.sendMessage(CC.translate("&d/raid reloadScenario <scenario_name>"));
        }
    }
    private void handleReloadCommand(Player player) {
        if(player.isOp()) {
            player.sendMessage(CC.translate("&aReloading raid messages..."));
            messageManager.reloadMessages(false, true);
            player.sendMessage(CC.translate("&aRaid messages reloaded successfully."));
        }
    }
    private void handleArenaReloadCommand(CommandSender sender, String[] args) {
        if(sender.isOp()) {
            if(args.length < 2) {
                sender.sendMessage("Usage: /raid reloadArena <arena_name>");
                return;
            }
            String arenaName = args[1];
            raidManager.attemptReloadArena(sender, arenaName);
        }
    }
    private void handleScenarioReloadCommand(CommandSender sender, String[] args) {
        if(sender.isOp()) {
            if(args.length < 2) {
                sender.sendMessage("Usage: /raid reloadScenario <scenario_name>");
                return;
            }
            String scenarioName = args[1];
            raidManager.attemptReloadScenario(sender, scenarioName);
        }
    }
}
