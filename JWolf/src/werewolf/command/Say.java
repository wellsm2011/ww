package werewolf.command;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Command;

public class Say implements Command
{
	Game	m_game;

	public Say(Game game)
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
		{ "say" };
		return result;
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "say" };
	}

	@Override
	public void help(werewolf.IrcUser caller, String command, String arguments, boolean isChannel)
	{

	}
}
