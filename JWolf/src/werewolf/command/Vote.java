package werewolf.command;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Command;

public class Vote implements Command
{
	Game	m_game;

	public Vote(Game game)
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
		{ "vote", "lynch" };
		return result;
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "vote" };
	}

	@Override
	public void help(werewolf.IrcUser caller, String command, String arguments, boolean isChannel)
	{

	}
}
