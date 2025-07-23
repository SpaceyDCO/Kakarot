package github.kakarot.Tools.Handler.Partys.DataManager;
import com.google.gson.reflect.TypeToken;
import github.kakarot.Tools.Data.Party.Party;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Clase que gestiona todas las operaciones relacionadas con las parties (grupos de jugadores).
 * Incluye creación, eliminación, almacenamiento y recuperación de datos de party.
 */
public class PartyManager {

    private static final Map<UUID, Party> parties = new HashMap<> ( );
    private static final Gson gson = new Gson();
    private static final File dataFile = new File("plugins/Kakarot/parties.json");
    /**
     * Crea una nueva party con un nombre y líder específico.
     *
     * @param name   Nombre de la party.
     * @param leader UUID del jugador líder.
     * @return La party creada.
     */
    public static Party createParty ( String name, UUID leader ) {
        Party party = new Party ( );
        party.setId ( UUID.randomUUID ( ) );
        party.setName ( name );
        party.setLeader ( leader );
        party.setMembers ( new ArrayList<> ( ) );
        party.setInviteTimestamps ( new HashMap<> ( ) );
        party.setOpen ( false );
        party.addMember ( leader );

        parties.put ( party.getId ( ), party );
        return party;
    }
    /**
     * Disuelve una party dado su ID.
     *
     * @param partyId UUID de la party a disolver.
     */
    public static void disbandParty ( UUID partyId ) {
        Party party = parties.remove ( partyId );
        if (party != null) {
            party.disband ( );
        }
    }

    /**
     * Obtiene la party a la que pertenece un jugador.
     *
     * @param playerId UUID del jugador.
     * @return La party a la que pertenece el jugador, o null si no pertenece a ninguna.
     */
    public static Party getPartyOf ( UUID playerId ) {
        for (Party party : parties.values ( )) {
            if (party.isMember ( playerId )) {
                return party;
            }
        }
        return null;
    }
    /**
     * Verifica si un jugador pertenece a una party.
     *
     * @param playerId UUID del jugador.
     * @return true si pertenece a una party, false en caso contrario.
     */
    public static boolean isInParty ( UUID playerId ) {
        return getPartyOf ( playerId ) != null;
    }
    /**
     * Obtiene una party por su ID.
     *
     * @param partyId UUID de la party.
     * @return La party correspondiente, o null si no existe.
     */
    public static Party getPartyById ( UUID partyId ) {
        return parties.get ( partyId );
    }
    /**
     * Obtiene todas las parties existentes.
     *
     * @return Colección inmodificable de todas las parties.
     */
    public static Collection<Party> getAllParties () {
        return Collections.unmodifiableCollection ( parties.values ( ) );
    }
    /**
     * Expulsa a un jugador de su party con un motivo dado.
     *
     * @param playerId UUID del jugador.
     * @param reason   Motivo de la expulsión.
     * @return true si el jugador fue expulsado, false si no pertenecía a ninguna party.
     */
    public static boolean kickFromParty ( UUID playerId, String reason ) {
        Party party = getPartyOf ( playerId );
        if (party != null) {
            Player player = Bukkit.getPlayer ( playerId );
            if (player != null) {
                player.sendMessage ( "§cFuiste expulsado de la party. Motivo: " + reason );
            }
            return party.removeMember ( playerId );
        }
        return false;
    }
    /**
     * Elimina todas las parties existentes y disuelve cada una de ellas.
     */
    public static void clearAll () {
        for (Party party : parties.values ( )) {
            party.disband ( );
        }
        parties.clear ( );
    }
    /**
     * Actualiza los datos de una party existente o la añade si no existe.
     *
     * @param party Party a actualizar.
     */
    public static void updateParty ( Party party ) {
        if (party == null || party.getId ( ) == null) return;
        parties.put ( party.getId ( ), party );
    }
    public static void savePartiesToFile() {
        try (Writer writer = new FileWriter (dataFile)) {
            gson.toJson(parties, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadPartiesFromFile() {
        if (!dataFile.exists()) return;

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, Party>> () {}.getType();
            Map<UUID, Party> loaded = gson.fromJson(reader, type);
            parties.clear();
            if (loaded != null) {
                parties.putAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
