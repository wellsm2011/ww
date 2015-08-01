package werewolf;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import werewolf.command.Help;
import werewolf.command.Join;
import werewolf.command.Ping;
import werewolf.command.Start;
import werewolf.command.Wait;
import werewolf.command.console.ListPlayers;
import werewolf.command.console.ListUsers;
import werewolf.command.console.Set;
import werewolf.define.Command;
import werewolf.define.ConsoleCommand;
import werewolf.define.Roleset;
import werewolf.roleset.Default;

public class WerewolfHost extends PircBot implements Runnable
{
	public static final int	major	= 0;
	public static final int	minor	= 0;
	public static final int	micro	= 1;

	public static void main(String[] args)
	{
		WerewolfHost bot = new WerewolfHost();
		new Thread(bot).run();
	}

	private Game				m_game;
	private Settings			config;
	private ConsoleCommand[]	m_console;				// Array of all possible
														// console commands.
	private Properties			m_usrConfig;

	private long				m_lastOpCheck	= 0;	// Time that the bot
														// last checked it's OP
														// status in the
														// channel.

	public WerewolfHost()
	{
		this.config = new Settings();
		Command[] commands = new Command[5];
		Roleset[] rolesets = new Roleset[1];
		this.m_console = new ConsoleCommand[3];
		this.m_game = new Game(this, this.config, commands, rolesets);

		commands[0] = new Help(this.m_game, commands);
		commands[1] = new Join(this.m_game);
		commands[2] = new Wait(this.m_game);
		commands[3] = new Start(this.m_game);
		commands[4] = new Ping(this.m_game);

		rolesets[0] = new Default(this.m_game);

		this.m_console[0] = new ListUsers(this, this.m_game);
		this.m_console[1] = new ListPlayers(this, this.m_game);
		this.m_console[2] = new Set(this, this.m_game);

		this.setName(this.config.getSetting("nick", Settings.nick));
		this.setLogin(this.config.getSetting("user", Settings.user));
		this.setMessageDelay(this.config.getSetting("msgDelay", Settings.msgDelay));

		// Enable debugging output.
		this.setVerbose(true);

		// Connect to the IRC server.
		this.connectNow();
	}

	public void checkOp()
	{
		long time = new Date().getTime();
		if (this.m_lastOpCheck > time - 10000) // Only check every 10 seconds at
												// most.
			return;
		this.m_lastOpCheck = time;
		IrcUser user = this.m_game.getUser(this.getName());
		if (user == null)
		{
			System.err.println("Cannot find bot IrcPlayer object.");
			return;
		}
		if (!user.isOp())
			this.sendMessage(this.config.getSetting("chanserv", Settings.chanserv), "OP " + this.config.getSetting("channel", Settings.channel));
	}

	public void connectNow()
	{
		boolean continueConnection = true;
		while (continueConnection)
			try
			{
				continueConnection = false;
				this.connect(this.config.getSetting("network", Settings.network));
			} catch (IOException e)
			{
				continueConnection = true;
				e.printStackTrace();
			} catch (NickAlreadyInUseException e)
			{
				e.printStackTrace();
				this.setName(this.getName() + "`");
				continueConnection = true;
			} catch (IrcException e)
			{
				e.printStackTrace();
				continueConnection = true;
			}
		this.joinChannel(this.config.getSetting("channel", Settings.channel));
		this.m_game.dispatch(this.config.getSetting("ident", Settings.ident));
	}

	@Override
	protected void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient)
	{
		if (this.m_game.isUser(sourceNick))
		{
			IrcUser user = this.m_game.getUser(sourceNick);
			user.setHost(sourceHostname);
			user.setUser(sourceLogin);
		}
		if (!this.m_game.isUser(recipient))
			return;
		this.m_game.getUser(recipient).onOpChange(false);
		this.checkOp();
	}

	protected void onDevoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient)
	{
		this.checkOp();
		if (this.m_game.isUser(sourceNick))
		{
			IrcUser user = this.m_game.getUser(sourceNick);
			user.setHost(sourceHostname);
			user.setUser(sourceLogin);
		}
		if (!this.m_game.isUser(recipient))
			return;
		this.m_game.getUser(recipient).onVoiceChange(false);
		if (this.m_game.isPlayer(recipient))
			this.voice(this.config.getSetting("channel", Settings.channel), recipient);
	}

	@Override
	protected void onDisconnect()
	{
		WerewolfHost.main(new String[]
		{});
	}

	@Override
	protected void onJoin(String channel, String sender, String login, String hostname)
	{
		if (!this.m_game.isUser(sender))
			this.m_game.onJoin(new IrcUser(this.m_game, sender, login, hostname));
		this.checkOp();
	}

	@Override
	protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason)
	{
		this.checkOp();
		this.m_game.onPart(recipientNick);
	}

	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message)
	{
		this.checkOp();
		message = Colors.removeColors(message);
		if (!this.m_game.isUser(sender))
			return;
		IrcUser user = this.m_game.getUser(sender);
		user.setUser(login);
		user.setHost(hostname);
		String chars = this.config.getSetting("cmdChar", Settings.cmdChar);
		for (int i = 0; i < chars.length(); ++i)
			if (message.charAt(0) == chars.charAt(i))
			{
				this.m_game.onMessage(user, message.substring(1), true);
				return;
			}
	}

	@Override
	protected void onNickChange(String oldNick, String login, String hostname, String newNick)
	{
		this.checkOp();
		IrcUser user = this.m_game.getUser(oldNick);
		if (user == null)
			return;
		user.setUser(login);
		user.setHost(hostname);
		user.onNickChange(newNick);
	}

	@Override
	protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice)
	{
		notice = Colors.removeColors(notice);
		if (!this.m_game.isUser(sourceNick))
			return;
		this.checkOp();
		IrcUser user = this.m_game.getUser(sourceNick);
		user.setUser(sourceLogin);
		user.setHost(sourceHostname);
		if (target.charAt(0) != this.config.getSetting("channel", Settings.channel).charAt(0))
		{
			this.m_game.onMessage(user, notice.substring(1), false);
			return;
		}
		String chars = this.config.getSetting("cmdChar", Settings.cmdChar);
		for (int i = 0; i < chars.length(); ++i)
			if (notice.charAt(0) == chars.charAt(i))
			{
				this.m_game.onMessage(user, notice.substring(1), true);
				return;
			}
	}

	@Override
	protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient)
	{
		if (this.m_game.isUser(sourceNick))
		{
			IrcUser user = this.m_game.getUser(sourceNick);
			user.setHost(sourceHostname);
			user.setUser(sourceLogin);
		}
		if (!this.m_game.isUser(recipient))
			return;
		this.m_game.getUser(recipient).onOpChange(true);
		this.checkOp();
	}

	@Override
	protected void onPart(String channel, String sender, String login, String hostname)
	{
		this.checkOp();
		this.m_game.onQuit(sender);
	}

	@Override
	protected void onPrivateMessage(String sender, String login, String hostname, String message)
	{
		this.checkOp();
		message = Colors.removeColors(message);
		if (!this.m_game.isUser(sender))
			return;
		IrcUser user = this.m_game.getUser(sender);
		user.setUser(login);
		user.setHost(hostname);
		this.m_game.onMessage(user, message.substring(1), false); // Private, so
																	// don't
																	// wait for
																	// cmd char.
	}

	@Override
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
	{
		this.checkOp();
		if (reason.substring(0, 5).matches("Quit:"))
			this.m_game.onQuit(sourceNick);
		else
			this.m_game.onPart(sourceNick);
	}

	@Override
	protected void onRemoveModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname)
	{
		this.checkOp();
		if (this.m_game.isPlayer(sourceNick))
		{
			IrcUser user = this.m_game.getUser(sourceNick);
			user.setHost(sourceHostname);
			user.setUser(sourceLogin);
		}
		if (!sourceNick.matches(this.getName()))
			this.setMode(channel, "+m");
	}

	@Override
	protected void onSetModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname)
	{
		this.checkOp();
		if (this.m_game.isPlayer(sourceNick))
		{
			IrcUser user = this.m_game.getUser(sourceNick);
			user.setHost(sourceHostname);
			user.setUser(sourceLogin);
		}
		if (!sourceNick.matches(this.getName()))
			this.setMode(channel, "-m");
	}

	@Override
	protected void onUserList(String channel, User[] users)
	{
		for (int i = 0; i < users.length; ++i)
			if (!this.m_game.isUser(users[i].getNick()))
				this.m_game.onJoin(new IrcUser(this.m_game, users[i]), true);
	}

	@Override
	protected void onVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient)
	{
		this.checkOp();
		if (this.m_game.isUser(sourceNick))
		{
			IrcUser user = this.m_game.getUser(sourceNick);
			user.setHost(sourceHostname);
			user.setUser(sourceLogin);
		}
		if (!this.m_game.isUser(recipient))
			return;
		this.m_game.getUser(recipient).onVoiceChange(true);
		if (!this.m_game.isPlayer(recipient))
			this.deVoice(this.config.getSetting("channel", Settings.channel), recipient);
	}

	@Override
	public void run()
	{
		Scanner input = new Scanner(System.in);
		boolean found = false;

		while (true)
			try
			{
				if (System.in.available() == 0)
					continue;
				found = false;
				String str = input.nextLine();
				if (str.charAt(0) == '?' && str.length() > 1)
				{ // Console command.
					for (int i = 0; i < this.m_console.length; ++i)
					{
						String[] aliases = this.m_console[i].getAliases();
						for (int j = 0; j < aliases.length; ++j)
							if (str.substring(1).startsWith(aliases[j]) && !found)
							{
								found = true;
								String args = "";
								if (str.length() > aliases[j].length() + 2)
									args = str.substring(aliases[j].length() + 2);
								this.m_console[i].onUse(aliases[j], args);
							}
					}
					if (!found)
						System.err.println("Unknown command.");
				} else if (str.charAt(0) == '/') // IRC command.
					this.m_game.dispatch(str.substring(1));
				else
					this.m_game.say(str);
			} catch (Throwable e)
			{
				e.printStackTrace();
			}
	}
}
