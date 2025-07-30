package github.kakarot.Parties.Managers;

import github.kakarot.Parties.Party;
import org.bukkit.entity.Player;

public interface IPartyManager {
    /**
     * Creates a new Party with the provided player as a leader
     * @param leader The player creating the party
     */
    void createParty(Player leader);

    /**
     * Disbands a party
     * @param party The party to be disbanded
     */
    void disbandParty(Party party);

    /**
     * Sends an invitation request to a party
     * @param leader The leader of the party
     * @param target The player to be invited
     */
    void invitePlayer(Player leader, Player target);
    /**
     * Accepts an incoming invite to a party
     * @param player The player accepting the invitation
     */
    void acceptInvite(Player player);
    /**
     * Denies an invitation request to join a party
     * @param player The player denying the invitation
     */
    void denyInvite(Player player);
    /**
     * Removes the player from their current party,
     * if they are the leader, the party is disbanded
     * @param player The player leaving the party
     */
    void leaveParty(Player player);
    /**
     * Kicks a given player from their current party
     * @param leader The leader of the party (Usually the player running the command)
     * @param target The player to be kicked
     */
    void kickPlayer(Player leader, Player target);
    /**
     * Obtains the party this player is in, if any
     * @param player The player to be scanned
     * @return The party the player is in, null if not in any party
     */
    Party getParty(Player player);
    /**
     * Checks whether this player is in a valid party
     * @param player The player to be checked
     * @return true if they're in party, false otherwise
     */
    boolean isInParty(Player player);
    /**
     * Clears any pending invites from parties,
     * called when the player joins a party, invitation time expires, or logs off
     * @param player Player whose invites will be cleared
     */
    void clearInvites(Player player);
}
