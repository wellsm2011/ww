package werewolf.command;

import java.util.Date;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.Settings;
import werewolf.define.Command;

public class Ping implements Command
{
	Game			m_game;
	private long	lastUse	= 0;

	public Ping(Game game)
	{
		this.m_game = game;
	}

	@Override
	public void call(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		if (!isChannel)
			return;
		if (this.m_game.getPhase() == 0)
		{
			if (this.m_game.getSettings().getSetting("pingRate", Settings.pingRate) * 1000 + this.lastUse <= new Date().getTime())
			{
				String output = "PING!";
				IrcUser[] users = this.m_game.getUsers();
				for (int i = 0; i < users.length; ++i)
				{
					if (this.m_game.isPlayer(users[i]))
						continue;
					if (users[i].getPreference("away", false))
						continue;
					if (caller.equals(users[i]))
						continue;
					if (users[i].equals(this.m_game.getBot().getNick()))
						continue;
					output += " " + users[i].getNick();
				}
				if (output.length() > 6)
					this.m_game.say(output);
				this.lastUse = new Date().getTime();
			} else
				caller.message("This command is rate-limited. Try again later.", isChannel);
		} else
			caller.message("You can't use " + command.toLowerCase() + " while a game is running.", isChannel);
	}

	@Override
	public String[] getAliases()
	{
		String[] result =
		{ "ping", "beep" };
		return result;
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "ping" };
	}

	@Override
	public void help(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		caller.replyTo("Ping - Mentions each non-away player in the channel by name.", isChannel);
	}
}
