package werewolf.define.interactable;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Interactable;
import werewolf.define.Reactable;
import werewolf.util.Kill;
import werewolf.util.PeekMod;

public class Role implements Interactable, Reactable
{
	private IrcUser	m_owner;
	private Game	m_game;

	public Role(Game game, IrcUser owner)
	{
		this.m_game = game;
		this.m_owner = owner;
	}

	@Override
	public void assigned()
	{
	}

	@Override
	public void blockTrigger()
	{
	}

	@Override
	public void call(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		System.err.println("Invalid call to call method of a Role.");
		Thread.currentThread();
		Thread.dumpStack();
	}

	/**
	 * Possible returns:<BR>
	 * 0 = Neutral. Third party role.<BR>
	 * 1 = Killer. Third party role.<BR>
	 * 2 = Village Aligned.<BR>
	 * 3 = Werewolf Aligned.<BR>
	 * 4 = Mafia Aligned.<BR>
	 * 5 = Cult Aligned.<BR>
	 * 6 = Zombie Aligned.<BR>
	 * 7 = Vampire Aligned.
	 *
	 * @return <b>Default:</b> Village (2)
	 */
	public int checkClass()
	{
		return 2;
	}

	@Override
	public String[] getAliases()
	{
		return new String[0];
	}

	@Override
	public String[] getCommands()
	{
		return new String[0];
	}

	@Override
	public void help(IrcUser caller, boolean isChannel)
	{
		caller.replyTo("Error: No help data found for this role.", isChannel);
	}

	@Override
	public void help(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		System.err.println("Invalid call to help method of a Role.");
		Thread.currentThread();
		Thread.dumpStack();
	}

	@Override
	public void infectBlossom(IrcUser infected)
	{
	}

	@Override
	public void infectBlossom(Role conversion, IrcUser user)
	{
	}

	@Override
	public void infectDie(IrcUser infected)
	{
	}

	@Override
	public void infectTrigger()
	{
	}

	/**
	 * Should return true if a player is aligned with a non-village team but
	 * does not know who they are and serves in more of a support role.
	 */
	public boolean isEvilVillager()
	{
		return false;
	}

	@Override
	public boolean isMason()
	{
		return false;
	}

	@Override
	public boolean killHold()
	{
		return false;
	}

	@Override
	public void killTrigger()
	{
	}

	/**
	 * Note: If knowsTeam and isMason both return true, the player's mason
	 * messages go out to BOTH masons AND other members of the team.
	 *
	 * @return True if this role should know the other members of its team.
	 *         <b>Default:</b> false.
	 */
	public boolean knowsTeam()
	{
		return false;
	}

	@Override
	public int mafiaParity()
	{
		return -1;
	}

	@Override
	public String name()
	{
		return "Default Role";
	}

	@Override
	public boolean nightlyHold()
	{
		return false;
	}

	@Override
	public void onBlock(boolean successful)
	{
	}

	@Override
	public boolean onBlocked()
	{
		return false;
	}

	@Override
	public void onKill(boolean successful)
	{
	}

	@Override
	public void onPeek(IrcUser peeker)
	{
	}

	@Override
	public void onProtect()
	{
	}

	/**
	 * Possible returns:<BR>
	 * 0 = Neutral. Third party role.<BR>
	 * 1 = Killer. Third party role.<BR>
	 * 2 = Village Aligned.<BR>
	 * 3 = Werewolf Aligned.<BR>
	 * 4 = Mafia Aligned.<BR>
	 * 5 = Cult Aligned.<BR>
	 * 6 = Zombie Aligned.<BR>
	 * 7 = Vampire Aligned.
	 *
	 * @return <b>Default:</b> Village (2)
	 */
	@Override
	public int peekClass()
	{
		return 2;
	}

	/**
	 *
	 * @return The name other players should see when peeking this role.
	 *         <b>Default:</b> The true name of the role.
	 */
	public String peekRole()
	{
		return this.name();
	}

	@Override
	public void peekSetup(PeekMod additions)
	{
	}

	@Override
	public void peekTrigger(PeekMod additions)
	{
	}

	@Override
	public void protectTrigger()
	{
	}

	@Override
	public void roundEnd()
	{
	}

	@Override
	public void stopKill(Kill kill)
	{
	}

	@Override
	public int votePower(IrcUser[][] history, IrcUser[] players)
	{
		return 1;
	}

	@Override
	public int wolfParity()
	{
		return -1;
	}
}
