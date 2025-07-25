package github.kakarot.Parties.Managers;

import github.kakarot.Parties.Party;

import java.util.Collection;
import java.util.UUID;

public interface IPartyManager {
    /**
     * Crea una nueva party con un nombre y líder específico.
     *
     * @param name Nombre de la party.
     * @param leader UUID del jugador líder.
     * @return Party creada.
     */
    Party createParty(String name, UUID leader);
    /**
     * Disuelve una party dado su ID.
     *
     * @param partyId UUID de la party a disolver.
     */
    void disbandParty(UUID partyId);
    /**
     * Obtiene la party a la que pertenece un jugador.
     *
     * @param playerId UUID del jugador.
     * @return La party a la que pertenece el jugador, o null si no pertenece a ninguna.
     */
    Party getPartyOf(UUID playerId);
    /**
     * Verifica si un jugador pertenece a una party.
     *
     * @param playerId UUID del jugador.
     * @return true si pertenece a una party, false en caso contrario.
     */
    boolean isInParty(UUID playerId);
    /**
     * Obtiene una party por su ID.
     *
     * @param partyId UUID de la party.
     * @return La party correspondiente, o null si no existe.
     */
    Party getPartyById(UUID partyId);
    /**
     * Obtiene todas las parties existentes.
     *
     * @return Colección inmodificable de todas las parties.
     */
    Collection<Party> getAllParties();
    /**
     * Elimina todas las parties existentes y disuelve cada una de ellas.
     */
    void clearAll();
    /**
     * Actualiza los datos de una party existente o la añade si no existe.
     *
     * @param party Party a actualizar.
     */
    void updateParty(Party party);
    /**
     * Guarda las parties actualmente registradas en el servidor a la config
     */
    void savePartiesToFile();
    /**
     * Lee las parties registradas en la config
     */
    void loadPartiesFromFile();
}
