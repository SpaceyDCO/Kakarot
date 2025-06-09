package github.kakarot.Commands;

import github.kakarot.Tools.CC;
import github.kakarot.Tools.Commands.BaseCommand;
import github.kakarot.Tools.Commands.Command;
import github.kakarot.Tools.Commands.CommandArgs;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class TPSCommand extends BaseCommand {
    @Override
    @Command(name = "givepercentagetps", aliases = {"gptps", "tppercentage", "triviatps"}, permission = "PERCENTAGETPS.GIVE", inGameOnly = false)
    public void onCommand(CommandArgs command) throws IOException {
        CommandSender sender = command.getSender();
        String[] args = command.getArgs();
        if(args.length < 2) {
            sender.sendMessage(CC.translate("&cDebes indicar el nombre de un usuario y el porcentaje de su nivel que se dara en tps\n&c/gptps <nick> <porcentaje>"));
            return;
        }
        if(Bukkit.getPlayer(args[0]) == null) {
            sender.sendMessage(CC.translate("&cEse jugador no existe!"));
            return;
        }
        if(!Bukkit.getPlayer(args[0]).isOnline()) {
            sender.sendMessage(CC.translate("&cEl jugador debe estar conectado"));
            return;
        }
        Player player = Bukkit.getPlayer(args[0]);
        try {
            Integer.parseInt(args[1].trim());
        }catch(NumberFormatException e) {
            sender.sendMessage(CC.translate("&cEl porcentaje debe ser un numero entero"));
            return;
        }
        int percentage = Integer.parseInt(args[1].trim()) / 100;
        int total = percentage * calculateLevel(player);
        IDBCPlayer idbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
        idbcPlayer.setTP(idbcPlayer.getTP() + total);
        player.sendMessage(CC.translate("&a+ " + total));
        sender.sendMessage(CC.translate("&a" + total + " han sido entregados a " + player.getName()));
    }
    private static int calculateLevel(Player player) {
        IDBCPlayer dbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();
        int str = dbcPlayer.getStat("str");
        int dex = dbcPlayer.getStat("dex");
        int con = dbcPlayer.getStat("con");
        int wil = dbcPlayer.getStat("wil");
        int mnd = dbcPlayer.getStat("mnd");
        int spi = dbcPlayer.getStat("spi");
        return (((str+dex+con+wil+mnd+spi)/5)-11);
    }
}
