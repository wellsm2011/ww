package werewolf.command;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Command;

public class Will implements Command
{
	Game	m_game;

	public Will(Game game)
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
		{ "will" };
		return result;
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "will" };
	}

	@Override
	public void help(werewolf.IrcUser caller, String command, String arguments, boolean isChannel)
	{
	}
}
