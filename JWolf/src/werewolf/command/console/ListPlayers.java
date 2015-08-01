package werewolf.command.console;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.WerewolfHost;
import werewolf.define.ConsoleCommand;

public class ListPlayers implements ConsoleCommand
{
	private Game			m_game;
	private WerewolfHost	m_bot;

	public ListPlayers(WerewolfHost bot, Game game)
	{
		this.m_game = game;
		this.m_bot = bot;
	}

	@Override
	public String[] getAliases()
	{
		String[] reply =
		{ "players" };
		return reply;
	}

	@Override
	public void help(String args)
	{
		return;
	}

	@Override
	public void onUse(String command, String args)
	{
		IrcUser[] users = this.m_game.getPlayers();
		String reply;
		if (users.length > 0)
		{
			reply = users[0].getNick();
			for (int i = 1; i < users.length; ++i)
				reply += ", " + users[i].getNick();
		} else
			reply = "No current users.";
		System.out.println(reply);
	}
}
