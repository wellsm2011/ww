package werewolf.command;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Command;

public class Join implements Command
{
	Game	m_game;

	public Join(Game game)
	{
		this.m_game = game;
	}

	@Override
	public void call(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		if (!isChannel)
			return;
		else if (this.m_game.isPlayer(caller))
			caller.message("You are already playing.", true);
		else if (this.m_game.getPhase() != 0)
			caller.message("The game has already started. Please wait for it to finish.", true);
		else if (this.m_game.getPlayers().length >= this.m_game.getRoleset().maxPlayers())
			caller.message("This game has already reached its maximum number of players. Try again next game.", true);
		else
		{
			caller.makePlayer();
			caller.voice();
		}
	}

	@Override
	public String[] getAliases()
	{
		return new String[]
		{ "join", "j" };
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "join" };
	}

	@Override
	public void help(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		caller.replyTo("Join - Adds the user to the list of players. Only usable by non-players during setup.", isChannel);
	}
}
