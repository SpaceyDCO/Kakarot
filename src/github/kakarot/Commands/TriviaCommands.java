package github.kakarot.Commands;

import github.kakarot.Main;
import github.kakarot.Tools.CC;
import github.kakarot.Tools.Commands.BaseCommand;
import github.kakarot.Tools.Commands.Command;
import github.kakarot.Tools.Commands.CommandArgs;
import github.kakarot.Tools.Input.PlayerInput;
import github.kakarot.Trivias.TriviaDataHandler;
import github.kakarot.Trivias.TriviasData;
import org.bukkit.entity.Player;

import java.io.IOException;

public class TriviaCommands extends BaseCommand {
    @Override
    @Command(name = "trivia", aliases = "trivias", permission = "TRIVIAS.MANAGE")
    public void onCommand(CommandArgs command) throws IOException {
        Player player = command.getPlayer();
        String[] args = command.getArgs();
        if(args.length < 1) {
            sendCorrectUsage(player);
            return;
        }
        switch(args[0].toLowerCase().trim()) {
            case "list":
                if(TriviaDataHandler.triviasDataMap.isEmpty()) {
                    player.sendMessage(CC.translate("&cLa lista de trivias está vacia!"));
                    return;
                }
                player.sendMessage(CC.translate("&9Lista de trivias registradas (IDs):"));
                for(String key : TriviaDataHandler.triviasDataMap.keySet()) {
                    player.sendMessage(CC.translate("&b- " + key));
                }
                break;
            case "create":
                player.sendMessage(CC.translate("&f&oIniciando creador de trivia..."));
                player.sendMessage(CC.translate("&7Por favor introduce la ID con la que la trivia se guardará en la config"));
                PlayerInput input = new PlayerInput(player, Main.instance, true);
                input.waitForPlayerInput(120, "Tiempo de espera excedido", id -> {
                    player.sendMessage(CC.translate("&fDato para la ID guardado: &o" + id));
                    player.sendMessage(CC.translate("&7Introduce la categoria a la cual pertenecerá esta trivia"));
                    PlayerInput input2 = new PlayerInput(player, Main.instance, true);
                    input2.waitForPlayerInput(120, "Tiempo de espera excedido", category -> {
                        player.sendMessage(CC.translate("&fDato categoria guardado: &o" + category));
                        player.sendMessage(CC.translate("&7Introduce la pregunta de la trivia"));
                        PlayerInput input3 = new PlayerInput(player, Main.instance, false);
                        input3.waitForPlayerInput(120, "Tiempo de espera agotado", question -> {
                            player.sendMessage(CC.translate("&fDato pregunta guardado: &o" + question));
                            player.sendMessage(CC.translate("&9--------------------------------------"));
                            player.sendMessage(CC.translate("&7Guardando trivia con los siguientes datos:"));
                            player.sendMessage(CC.translate("&8ID: &7" + id.toUpperCase().trim()));
                            player.sendMessage(CC.translate("&8Categoria: &7" + category));
                            player.sendMessage(CC.translate("&8Pregunta: &7" + question));
                            player.sendMessage(CC.translate("&9--------------------------------------"));
                            TriviaDataHandler.addTrivia(id.toUpperCase().trim(), category, question, 120, null, null, false);
                        });
                    });
                });
                break;
            case "remove":
                if(args.length < 2) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia remove <TriviaID>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                TriviaDataHandler.triviasDataMap.remove(args[1].toUpperCase().trim());
                player.sendMessage(CC.translate("&cTrivia " + args[1] + " eliminada correctamente"));
                break;
            case "info":
                if(args.length < 2) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia info <TriviaID>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                TriviasData trivia = TriviaDataHandler.getTrivia(args[1].toUpperCase().trim());
                if(trivia == null) return;
                player.sendMessage(CC.translate("&1--------------------------------------"));
                player.sendMessage(CC.translate("&9Datos para trivia con id &b" + args[1].trim().toUpperCase() + "&9:"));
                player.sendMessage(CC.translate("&9Categoria: &b" + trivia.getCategory()));
                player.sendMessage(CC.translate("&9Pregunta: &b" + trivia.getQuestion()));
                player.sendMessage(CC.translate("&9Tiempo limite (segundos): &b" + trivia.getTimeLimit()));
                if(trivia.getAnswers() != null) {
                    player.sendMessage(CC.translate("&9Respuestas:"));
                    for(String answer : trivia.getAnswers()) {
                        player.sendMessage(CC.translate("&b- " + answer));
                    }
                }else player.sendMessage(CC.translate("&9Respuestas: &b" + "No configurado aún"));
                if(trivia.getRewardsCommands() != null) {
                    player.sendMessage(CC.translate("&9Recompensas:"));
                    for(String reward : trivia.getRewardsCommands()) {
                        player.sendMessage(CC.translate("&b- /" + reward));
                    }
                }else player.sendMessage(CC.translate("&9Recompensas: &b" + "No configurado aún"));
                player.sendMessage(CC.translate("&9Habilitada: &b" + trivia.isEnabled()));
                player.sendMessage(CC.translate("&1--------------------------------------"));
                break;
            case "addanswer":
                if(args.length < 2) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia addAnswer <TriviaID>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                player.sendMessage(CC.translate("&7Introduce la respuesta a agregar:"));
                PlayerInput answerInput = new PlayerInput(player, Main.instance);
                answerInput.waitForPlayerInput(120, "&fTiempo de espera agotado", answer -> {
                    TriviaDataHandler handler = new TriviaDataHandler(args[1].toUpperCase().trim());
                    handler.addAnswer(answer);
                    handler.save();
                    player.sendMessage(CC.translate("&9Respuesta agregada correctamente a trivia con ID &b" + args[1].toUpperCase().trim()));
                });
                break;
            case "removeanswers":
                if(args.length < 2) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia removeAnswers <TriviaID>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                TriviaDataHandler handler1 = new TriviaDataHandler(args[1].toUpperCase().trim());
                handler1.removeAnswers();
                handler1.save();
                player.sendMessage(CC.translate("&9Respuestas para la trivia con ID &b" + args[1].toUpperCase().trim() + " &9eliminadas correctamente."));
                break;
            case "addreward":
                if(args.length < 2) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia addReward <TriviaID>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                player.sendMessage(CC.translate("&7Introduce un comando a ejecutar como recompensa"));
                PlayerInput reward = new PlayerInput(player, Main.instance);
                reward.waitForPlayerInput(120, "&fTiempo de espera agotado", r -> {
                    TriviaDataHandler rHandler = new TriviaDataHandler(args[1].toUpperCase().trim());
                    rHandler.addReward(r);
                    rHandler.save();
                    player.sendMessage(CC.translate("&9Recompensa agregada correctamente a trivia con ID &b" + args[1].toUpperCase().trim()));
                });
                break;
            case "removerewards":
                if(args.length < 2) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia removeRewards <TriviaID>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                TriviaDataHandler rHandler1 = new TriviaDataHandler(args[1].toUpperCase().trim());
                rHandler1.removeRewards();
                rHandler1.save();
                player.sendMessage(CC.translate("&9Recompensas para la trivia con ID &b" + args[1].toUpperCase().trim() + " &9eliminadas correctamente."));
                break;
            case "setenabled":
                if(args.length < 2) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia setEnabled <TriviaID>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                TriviaDataHandler eHandler = new TriviaDataHandler(args[1].toUpperCase().trim());
                if(!eHandler.isConfigured()) {
                    player.sendMessage(CC.translate("&cAntes de activar esta trivia debe tener una respuesta y recompensa asignadas!"));
                    return;
                }
                if(eHandler.getData().isEnabled()) {
                    eHandler.setEnabled(false);
                    player.sendMessage(CC.translate("&9Trivia &b" + args[1].toUpperCase().trim() + " &9ha sido deshabilitada."));
                }
                else {
                    eHandler.setEnabled(true);
                    player.sendMessage(CC.translate("&9Trivia &b" + args[1].toUpperCase().trim() + " &9ha sido habilitada."));
                }
                break;
            case "settimelimit":
                if(args.length < 3) {
                    player.sendMessage(CC.translate("&cUso correcto: /trivia setTimeLimit <TriviaID> <Límite>"));
                    return;
                }
                if(TriviaDataHandler.getTrivia(args[1].toUpperCase().trim()) == null) {
                    player.sendMessage(CC.translate("&cTrivia con ID " + args[1] + " no existe"));
                    return;
                }
                try {
                    Integer.parseInt(args[2].trim());
                }catch(NumberFormatException e) {
                    player.sendMessage(CC.translate("&cEl tiempo límite debe ser un número entero!"));
                    return;
                }
                if(Integer.parseInt(args[2].trim()) <= 0) {
                    player.sendMessage(CC.translate("&cEl tiempo límite debe ser un número mayor a 0!"));
                    return;
                }
                TriviaDataHandler tHandler = new TriviaDataHandler(args[1].toUpperCase().trim());
                tHandler.getData().setTimeLimit(Integer.parseInt(args[2].trim()));
                tHandler.save();
                player.sendMessage(CC.translate("&9Tiempo límite actualizado exitosamente."));
                break;
            default:
                sendCorrectUsage(player);
        }
    }
    private void sendCorrectUsage(Player player) {
        player.sendMessage(CC.translate("&1--------------------------------------"));
        player.sendMessage(CC.translate("&9Comandos disponibles:"));
        player.sendMessage(CC.translate("&b/trivia create -> &9Inicia el creador de trivia"));
        player.sendMessage(CC.translate("&b/trivia remove <ID> -> &9Elimina la trivia con ID proporcionada"));
        player.sendMessage(CC.translate("&b/trivia addAnswer <ID> -> &9Agrega una respuesta a una trivia"));
        player.sendMessage(CC.translate("&b/trivia removeAnswers <ID> -> &9Elimina las respuestas de una trivia (todas)"));
        player.sendMessage(CC.translate("&b/trivia addReward <ID> -> &9Agrega una recompensa a una trivia"));
        player.sendMessage(CC.translate("&b/trivia removeRewards <ID> -> &9Elimina todas las recompensas de una trivia"));
        player.sendMessage(CC.translate("&b/trivia setEnabled <ID> -> &9Activa/Desactiva una trivia"));
        player.sendMessage(CC.translate("&b/trivia info <ID> -> &9Muestra información de una trivia"));
        player.sendMessage(CC.translate("&b/trivia setTimeLimit <ID> -> &9Modifica el tiempo límite para responder esa trivia"));
        player.sendMessage(CC.translate("&b/trivia list -> &9Muestra la lista de trivias registradas"));
        player.sendMessage(CC.translate("&1--------------------------------------"));
    }
}
