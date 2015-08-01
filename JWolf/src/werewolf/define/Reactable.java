package werewolf.define;

import werewolf.IrcUser;
import werewolf.util.PeekMod;

public interface Reactable extends Messages
{

	/**
	 * Called on the player who initiated a blossomed infection.
	 *
	 * @param infected
	 *            The player whose infection blossomed.
	 */
	public void infectBlossom(IrcUser infected);

	/**
	 * Called on the player who initiated a dead infection.
	 *
	 * @param infected
	 *            The player whose infection died.
	 */
	public void infectDie(IrcUser infected);

	/**
	 * Signifies that this object's abilities have blocked another user
	 * successfully.
	 * 
	 * @param successful
	 *            True if the block altered the target's abilities.
	 */
	public void onBlock(boolean successful);

	/**
	 * Reacts to a killing action by this object.
	 */
	public void onKill(boolean successful);

	/**
	 * Reacts to a successful protection.
	 */
	public void onProtect();

	/**
	 * Sets up the statuses for peeks.
	 */
	public void peekSetup(PeekMod additions);
}
