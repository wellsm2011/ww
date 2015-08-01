package werewolf.command;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Command;
import werewolf.define.Messages;

public class Quit implements Command
{
	Game	m_game;

	public Quit(Game game)
	{
		this.m_game = game;
	}

	@Override
	public void call(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		if (!caller.isPlayer())
			caller.replyTo("You are not currently playing.", isChannel);
		else if (this.m_game.getPhase() == 0 || !this.m_game.getRoleset().showRoles())
		{
			this.m_game.say(Game.chooseMessage(Messages.msg_quit, caller.getNick()));
			caller.makeUser();
		} else
		{
			this.m_game.say(Game.chooseMessage(Messages.msg_quit, caller.getNick()) + " He was the " + Game.bold(caller.getRole().name()));
			caller.makeUser();
		}
	}

	@Override
	public String[] getAliases()
	{
		String[] result =
		{ "quit", "leave" };
		return result;
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "quit" };
	}

	@Override
	public void help(werewolf.IrcUser caller, String command, String arguments, boolean isChannel)
	{
		caller.replyTo("Quit - Removes the user to the list of players. If a game was running, the player's role is revealed.", isChannel);
	}
}
