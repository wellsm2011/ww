package werewolf.define.interactable;

import werewolf.IrcUser;
import werewolf.define.Interactable;
import werewolf.define.Reactable;
import werewolf.util.Kill;
import werewolf.util.PeekMod;

public class Status implements Interactable, Reactable
{

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
		caller.replyTo("Error: No help data found for this status.", isChannel);
	}

	@Override
	public void help(IrcUser caller, String command, String arguments, boolean isChannel)
	{
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

	@Override
	public int mafiaParity()
	{
		return 0;
	}

	@Override
	public String name()
	{
		return null;
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

	@Override
	public int peekClass()
	{
		return 0;
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
		return 0;
	}

	@Override
	public int wolfParity()
	{
		return 0;
	}
}
