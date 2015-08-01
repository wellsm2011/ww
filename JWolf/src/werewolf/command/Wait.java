package werewolf.command;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Command;

public class Wait implements Command
{
	Game	m_game;

	public Wait(Game game)
	{
		this.m_game = game;
	}

	@Override
	public void call(IrcUser caller, String command, String arguments, boolean isChannel)
	{

	}

	@Override
	public String[] getAliases()
	{
		String[] result =
		{ "wait" };
		return result;
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "wait" };
	}

	@Override
	public void help(werewolf.IrcUser caller, String command, String arguments, boolean isChannel)
	{
		caller.replyTo("Wait - Adds .", isChannel);
	}
}
