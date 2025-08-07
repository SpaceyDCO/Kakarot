package github.kakarot.Commands;

import github.kakarot.Main;
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
    @Override
    @Command(name = "raids", aliases = "raid", isAdminOnly = true)
    public void onCommand(CommandArgs command) throws IOException {
        CommandSender sender = command.getSender();
        String[] args = command.getArgs();
        Player player = (Player) sender;
        switch(args[0].toLowerCase()) {
            case "reload":
                handleReloadCommand(player);
                break;
            case "default":
                player.sendMessage(CC.translate("&5Commands:"));
                player.sendMessage(CC.translate("&d/raid reload"));
        }
    }
    private void handleReloadCommand(Player player) {
        if(player.isOp()) {
            player.sendMessage(CC.translate("&aReloading raid messages..."));
            messageManager.reloadMessages(false, true);
            player.sendMessage(CC.translate("&aRaid messages reloaded successfully."));
        }
    }
}
