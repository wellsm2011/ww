package werewolf.define;

import werewolf.IrcUser;
import werewolf.define.interactable.Role;
import werewolf.util.Kill;
import werewolf.util.PeekMod;

/**
 * Classes that implement Interactable designate themselves as objects of a
 * Werewolf game and are acted upon for a large variety of functions within the
 * game. Interactables include Items, Statuses and Roles. Additionally, every
 * {@link werewolf.IrcUser IrcUser} will impliment this class in order to wrap
 * all the Interactable items that the user controls.
 */
public interface Interactable extends Command
{
	/**
	 * Called when the interactable is assigned to a player. Sends out startup
	 * message(s) to the player, if any.
	 */
	public void assigned();

	/**
	 * Performs any block actions by calling the {@link #onBlocked()}function of
	 * any affected players.
	 */
	public void blockTrigger();

	/**
	 * Responsible for returning help messages to the user when called.
	 *
	 * @param caller
	 * @param isChannel
	 */
	public void help(IrcUser caller, boolean isChannel);

	/**
	 * Called on the infected player when an infection blossoms and performs any
	 * cleanup required.
	 *
	 * @param conversion
	 * @param user
	 */
	public void infectBlossom(Role conversion, IrcUser user);

	/**
	 * Performs any infect actions by calling the {@link #onInfect} function of
	 * any affected players.
	 */
	public void infectTrigger();

	/**
	 * Note: If knowsTeam and isMason both return true, the player's mason
	 * messages go out to BOTH masons AND other members of the team.
	 *
	 * @return True if this object grants access to Mason chat. False if this
	 *         object does not affect the player's access.
	 */
	public boolean isMason();

	/**
	 *
	 * @return True if and only if the object has not set all nightly kill
	 *         targets.
	 */
	public boolean killHold();

	/**
	 * Performs any kills the object may have.
	 */
	public void killTrigger();

	/**
	 * Returns the mafia Parity of this role. A negative number means that this
	 * role works against parity, while a positive one means the role works for
	 * mafia parity.
	 *
	 * @return The mafia Parity of this role.
	 */
	public int mafiaParity();

	/**
	 *
	 * @return The given name of the interactable object.
	 */
	public String name();

	/**
	 *
	 * @return True if and only if the object has not set all nightly action
	 *         targets.
	 */
	public boolean nightlyHold();

	/**
	 * Signifies that this object's abilities have been blocked.
	 * 
	 * @return True if this object's targets changed.
	 */
	public boolean onBlocked();

	/**
	 * Called when the user is peeked.
	 */
	public void onPeek(IrcUser peeker);

	/**
	 * Returns any cloak this object provides for the user's class.
	 * 
	 * Possible returns:<BR>
	 * 0 = Neutral. Third party role.<BR>
	 * 1 = Killer. Third party role.<BR>
	 * 2 = Village Aligned.<BR>
	 * 3 = Werewolf Aligned.<BR>
	 * 4 = Mafia Aligned.<BR>
	 * 5 = Cult Aligned.<BR>
	 * 6 = Zombie Aligned.<BR>
	 * 7 = Vampire Aligned.<BR>
	 * 
	 * @return
	 */
	public int peekClass();

	/**
	 * Performs any peeking actions.
	 */
	public void peekTrigger(PeekMod additions);

	/**
	 * Perfoms any protection actions for this role.
	 */
	public void protectTrigger();

	/**
	 * Called at the end of each round. Should reset any targets and statuses
	 * the object had.
	 */
	public void roundEnd();

	/**
	 * Gives this object a chance to stop a kill if it has the power to do so.
	 * 
	 * @param kill
	 *            The Kill object attacking the user.
	 */
	public void stopKill(Kill kill);

	/**
	 * Returns the vote power afforded by this object. Should never depend on or
	 * call other objects' vote power.
	 *
	 * @param history
	 *            The complete history of the round in a {voter, target} array.
	 * @param players
	 *            The array of currently living players in the game.
	 * @return The voting power of this role.
	 */
	public int votePower(IrcUser[][] history, IrcUser[] players);

	/**
	 * Returns the wolf Parity of this role. A negative number means that this
	 * role works against parity, while a positive one means the role works for
	 * wolf parity.
	 *
	 * @return The wolf Parity of this role.
	 */
	public int wolfParity();
}
