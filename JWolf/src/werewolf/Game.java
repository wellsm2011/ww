package werewolf;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import org.jibble.pircbot.Colors;

import werewolf.define.Command;
import werewolf.define.Messages;
import werewolf.define.Roleset;
import werewolf.util.PeekMod;

public class Game implements Runnable, Messages
{
	/**
	 * Kill/Protect from night lynch.
	 */
	public static final int	LYNCH_TYPE			= 0x001;

	/**
	 * Kill/Protect from village lynch.
	 */
	public static final int	VILLAGE_LYNCH		= 0x002;

	/**
	 * Protect from all lynches.
	 */
	public static final int	LYNCH_PROTECT		= 0x003;

	/**
	 * Kill/Protect from night maul. Protect from wolf maul also.
	 */
	public static final int	MAUL_TYPE			= 0x004;

	/**
	 * Protect from all mauls.
	 */
	public static final int	WOLF_PROTECT		= 0x008;
	public static final int	WOLF_MAUL			= 0x00C;	// Subtype of maul.
	public static final int	SHOT_TYPE			= 0x010;
	public static final int	MAFIA_PROTECT		= 0x020;
	public static final int	MAFIA_SHOT			= 0x030;	// Subtype of shot.
	public static final int	MAGIC_TYPE			= 0x040;
	public static final int	POISON_TYPE			= 0x080;
	public static final int	DOOM_TYPE			= 0x100;
	public static final int	ULTIMATE_TYPE		= 0x200;
	public static final int	PHYSICAL_PROTECT	= 0x0BF;	// Supertype of
															// lynch, maul and
															// shot.
	public static final int	MAGICAL_PROTECT		= 0x140;	// Supertype of
															// magic and doom.

	public static String bold(String input)
	{
		return Colors.BOLD + input + Colors.BOLD;
	}

	public static String chooseMessage(String[] choices)
	{
		return choices[(int) (Math.random() * choices.length)];
	}

	public static String chooseMessage(String[] choices, String replace1)
	{
		String choice = choices[(int) (Math.random() * choices.length)];
		return choice.replaceAll("\\[1\\]", replace1);
	}

	public static String chooseMessage(String[] choices, String replace1, String replace2)
	{
		String choice = choices[(int) (Math.random() * choices.length)];
		return choice.replaceAll("\\[1\\]", replace1).replaceAll("\\[2\\]", replace2);
	}

	public static String chooseMessage(String[] choices, String replace1, String replace2, String replace3)
	{
		String choice = choices[(int) (Math.random() * choices.length)];
		return choice.replaceAll("\\[1\\]", replace1).replaceAll("\\[2\\]", replace2).replaceAll("\\[3\\]", replace3);
	}

	/**
	 * Concatinates two arrays of type T and returns a single array of the same
	 * type.
	 * 
	 * @param <T>
	 * @param first
	 * @param second
	 * @return
	 */
	public static <T> T[] concat(T[] first, T[] second)
	{
		if (first == null)
			return second;
		if (second == null)
			return first;
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static String getClass(int team, boolean isEvilVillager)
	{
		if (isEvilVillager)
			return "Evil Village";
		switch (team)
		{
			case 0:
				return "Neutral";
			case 1:
				return "Killer";
			case 2:
				return "Village";
			case 3:
				return "Werewolf";
			case 4:
				return "Mafia";
			case 5:
				return "Cult";
			case 6:
				return "Zombie";
			case 7:
				return "Vampire";
		}
		return "Unknown";
	}

	public static String underline(String input)
	{
		return Colors.UNDERLINE + input + Colors.UNDERLINE;
	}

	private Vector<IrcUser>	m_players		= new Vector<IrcUser>();	// Array
																		// of
																		// all
																		// players.
	private Vector<IrcUser>	m_users			= new Vector<IrcUser>();	// Array
																		// of
																		// all
																		// users
																		// in
																		// the
																		// channel.
	private Vector<String>	m_events		= new Vector<String>();	// Double
																		// array
																		// of
																		// event
																		// messages.

	private Command[]		m_commands;								// Array
																		// of
																		// all
																		// possible
																		// user
																		// commands.

	protected int			m_phase			= 0;						// Current
																		// phase.
																		// 0=Setup,
																		// 1=Night,
																		// 2=Dawn,
																		// 3=Day,
																		// 4=Dusk.

	private long			m_phaseStart	= new Date().getTime();	// Time
																		// that
																		// the
																		// current
																		// phase
																		// started.

	private long			m_phaseWait		= new Date().getTime();	// Time
																		// that
																		// the
																		// current
																		// phase
																		// may
																		// start
																		// at.

	private WerewolfHost	m_bot;										// Reference
																		// to
																		// IRC
																		// bot.

	private Settings		m_config;									// Preferences
																		// for
																		// bot.

	private Roleset[]		m_rolesets;

	private String			m_roleset;

	public Game(WerewolfHost bot, Settings config, Command[] commands, Roleset[] rolesets)
	{
		this.m_commands = commands;
		this.m_rolesets = rolesets;
		this.m_bot = bot;
		this.m_config = config;

		this.m_roleset = config.getPreference("roleset", "Default");
	}

	public void action(String message)
	{
		this.action(this.m_config.getSetting("channel", Settings.channel), message);
	}

	public void action(String target, String message)
	{
		this.m_bot.sendAction(target, message);
	}

	public void addPlayer(IrcUser newPlayer)
	{
		if (!this.isPlayer(newPlayer))
			this.m_players.add(newPlayer);
	}

	private void calcDay()
	{
		if (this.m_config.getSetting("dualPhase", Settings.dualPhase))
			this.calcNight();
	}

	private void calcNight()
	{
		IrcUser[] players = this.getPlayers();
		for (int i = 0; i < players.length; ++i)
			players[i].blockTrigger();
		for (int i = 0; i < players.length; ++i)
			players[i].protectTrigger();
		for (int i = 0; i < players.length; ++i)
			players[i].killTrigger();
		for (int i = 0; i < players.length; ++i)
			players[i].peekTrigger(new PeekMod());
		if (this.calcWin())
			return;
		for (int i = 0; i < players.length; ++i)
			players[i].infectTrigger();
		this.calcWin();
	}

	private boolean calcWin()
	{
		return false;
	}

	public void dispatch(String str)
	{
		String command = str;
		String arg = "";
		if (str.contains(" "))
		{
			command = str.substring(0, str.indexOf(" ")).toLowerCase();
			arg = str.substring(str.indexOf(" ") + 1, str.length());
		}

		if (command.contentEquals("say"))
			this.say(arg);
		else if (command.contentEquals("msg"))
			this.say(arg.substring(0, arg.indexOf(" ")), arg.substring(arg.indexOf(" ") + 1, arg.length()));
		else if (command.contentEquals("notice"))
			this.notice(arg.substring(0, arg.indexOf(" ")), arg.substring(arg.indexOf(" ") + 1, arg.length()));
		else if (command.contentEquals("me"))
			this.action(arg);
		else if (command.contentEquals("meu"))
			this.action(arg.substring(0, arg.indexOf(" ")), arg.substring(arg.indexOf(" ") + 1, arg.length()));
		else if (command.contentEquals("kick"))
		{
			if (arg.contains(" "))
				this.m_bot.kick(this.m_config.getSetting("channel", Settings.channel), arg.substring(0, arg.indexOf(" ")), arg.substring(arg.indexOf(" ") + 1, arg.length()));
			else
				this.m_bot.kick(this.m_config.getSetting("channel", Settings.channel), arg);
		} else if (command.contentEquals("op"))
			this.m_bot.op(this.m_config.getSetting("channel", Settings.channel), arg);
		else if (command.contentEquals("voice"))
			this.m_bot.voice(this.m_config.getSetting("channel", Settings.channel), arg);
		else if (command.contentEquals("deop"))
			this.m_bot.deOp(this.m_config.getSetting("channel", Settings.channel), arg);
		else if (command.contentEquals("devoice"))
			this.m_bot.deVoice(this.m_config.getSetting("channel", Settings.channel), arg);
		else if (command.contentEquals("quit"))
		{
			if (arg.length() == 0)
				this.m_bot.quitServer();
			else
				this.m_bot.quitServer(arg);
		} else if (command.contentEquals("cop"))
			this.toChanserv("OP " + this.m_config.getSetting("channel", Settings.channel) + " " + arg);
		else if (command.contentEquals("cvoice"))
			this.toChanserv("VOICE " + this.m_config.getSetting("channel", Settings.channel) + " " + arg);
		else if (command.contentEquals("cdeop"))
			this.toChanserv("DEOP " + this.m_config.getSetting("channel", Settings.channel) + " " + arg);
		else if (command.contentEquals("cdevoice"))
			this.toChanserv("DEVOICE " + this.m_config.getSetting("channel", Settings.channel) + " " + arg);
		else
			System.err.println("Unknown command: " + command);
	}

	private void displayResults()
	{

	}

	public void errorLog(String text)
	{
		this.logFile(text, "Error.log");
	}

	public void gameLog(String text)
	{
		this.logFile(text, "Game.log");
	}

	public WerewolfHost getBot()
	{
		return this.m_bot;
	}

	private int getObjectIndex(String nick, boolean partial, Vector<IrcUser> object)
	{
		nick = nick.toLowerCase();
		int found = -1;
		for (int i = 0; i < object.size(); ++i)
			if (object.elementAt(i).equals(nick))
				return i;
		if (partial)
		{
			for (int i = 0; i < object.size(); ++i)
				if (object.elementAt(i).getNick().toLowerCase().startsWith(nick))
				{
					if (found >= 0)
						return -2;
					found = i;
				}
			if (found >= 0)
				return found;
			for (int i = 0; i < object.size(); ++i)
				if (object.elementAt(i).getNick().toLowerCase().contains(nick))
				{
					if (found >= 0)
						return -2;
					found = i;
				}
		}
		return found;
	}

	public int getPhase()
	{
		return this.m_phase;
	}

	/**
	 * Takes a nick argument and returns a player with the exact same nick.
	 *
	 * @param nick
	 *            The nick requested.
	 * @return The IrcPlayer in the game with the same nick, or null if not
	 *         found.
	 */
	public IrcUser getPlayer(String nick)
	{
		return this.getPlayer(nick, false);
	}

	public IrcUser getPlayer(String nick, boolean partial)
	{
		int index = this.getPlayerIndex(nick, partial);
		if (index < 0)
			return null;
		return this.m_players.elementAt(index);
	}

	public int getPlayerIndex(String nick)
	{
		return this.getPlayerIndex(nick, false);
	}

	public int getPlayerIndex(String nick, boolean partial)
	{
		return this.getObjectIndex(nick, partial, this.m_players);
	}

	/**
	 *
	 * @return Returns an array of all current players in the game.
	 */
	public IrcUser[] getPlayers()
	{
		return this.m_players.toArray(new IrcUser[0]);
	}

	public Roleset getRoleset()
	{
		return this.getRoleset(this.m_roleset);
	}

	public Roleset getRoleset(String name)
	{
		for (int i = 0; i < this.m_rolesets.length; ++i)
			if (this.m_rolesets[i].name().matches(name))
				return this.m_rolesets[i];
		return null;
	}

	public Settings getSettings()
	{
		return this.m_config;
	}

	public IrcUser getUser(String nick)
	{
		return this.getUser(nick, false);
	}

	public IrcUser getUser(String nick, boolean partial)
	{
		int index = this.getUserIndex(nick, partial);
		if (index < 0)
			return null;
		return this.m_users.elementAt(index);
	}

	public int getUserIndex(String nick)
	{
		return this.getUserIndex(nick, false);
	}

	public int getUserIndex(String nick, boolean partial)
	{
		return this.getObjectIndex(nick, partial, this.m_users);
	}

	public IrcUser[] getUsers()
	{
		return this.m_users.toArray(new IrcUser[0]);
	}

	public void invite(String nick)
	{
		this.m_bot.sendInvite(nick, this.m_config.getSetting("channel", Settings.channel));
	}

	public void ircLog(String text)
	{
		this.logFile(text, "IRC.log");
	}

	public boolean isPlayer(IrcUser user)
	{
		return this.m_players.contains(user) || this.isPlayer(user.getNick());
	}

	public boolean isPlayer(String nick)
	{
		return this.getPlayerIndex(nick) >= 0;
	}

	public boolean isPlayer(String nick, boolean partial)
	{
		return this.getPlayerIndex(nick, partial) >= 0;
	}

	public boolean isUser(IrcUser user)
	{
		return this.m_users.contains(user) || this.isUser(user.getNick());
	}

	public boolean isUser(String nick)
	{
		return this.getUserIndex(nick) >= 0;
	}

	public boolean isUser(String nick, boolean partial)
	{
		return this.getUserIndex(nick, partial) >= 0;
	}

	public void logFile(String text, String location)
	{
		try
		{
			FileWriter out = new FileWriter(location, true);
			out.write(text);
			out.close();
		} catch (IOException ex)
		{
			ex.printStackTrace();
			if (!location.matches("Error.log"))
				this.errorLog(ex.getMessage());
		}
	}

	public void notice(String message)
	{
		this.notice(this.m_config.getSetting("channel", Settings.channel), message);
	}

	public void notice(String target, String message)
	{
		this.m_bot.sendNotice(target, message);
	}

	public void onJoin(IrcUser joiner)
	{
		this.onJoin(joiner, false);
	}

	public void onJoin(IrcUser joiner, boolean chanList)
	{
		System.out.println(joiner.getNick());
		if (this.isUser(joiner))
			return;
		this.m_users.add(joiner);
	}

	/**
	 * Function called when a user gives a valid command.
	 *
	 * @param user
	 *            The IrcPlayer that used the command.
	 * @param message
	 *            The string message containing the command.
	 * @param isChannel
	 *            True if the command was used in a channel context.
	 */
	public void onMessage(IrcUser user, String message, boolean isChannel)
	{
		String[] aliases;
		String[] userCommands = user.getAliases();
		if (!isChannel)
			this.onTeamMessage(user, message);
		for (int i = 0; i < userCommands.length; ++i)
			if ((message.toLowerCase() + " ").startsWith(userCommands[i] + " "))
			{
				user.call(user, userCommands[i], message.substring(userCommands[i].length()), isChannel);
				return;
			}
		for (int i = 0; i < this.m_commands.length; ++i)
		{
			aliases = this.m_commands[i].getAliases();
			for (int j = 0; j < aliases.length; ++j)
				if ((message.toLowerCase() + " ").startsWith(aliases[j] + " "))
				{
					System.err.println("Command recieved: " + aliases[j]);
					this.m_commands[i].call(user, aliases[j], message.substring(aliases[j].length()), isChannel);
					return;
				}
		}
	}

	/**
	 * Called when a user changes their nickname.
	 *
	 * @param origNick
	 *            Old nickname the user had before.
	 * @param newNick
	 *            New nickname that the user just changed to.
	 */
	public void onNickChange(String origNick, String newNick)
	{
		IrcUser player = this.getUser(origNick);
		if (player != null)
			player.onNickChange(newNick);
	}

	/**
	 * Function called when a user is ejected from the game channel. Called for
	 * kick and system initiated quit.
	 *
	 * @param nick
	 *            The nick of the user.
	 */

	public void onPart(String nick)
	{
		int index = this.getUserIndex(nick);
		if (index == -1)
			return;
		this.m_users.remove(index);
		index = this.getPlayerIndex(nick);
		if (index == -1)
			return;
		this.m_players.remove(index);
	}

	/**
	 * Function called when a user leaves the game channel. Called for part and
	 * user initiated quit.
	 *
	 * @param nick
	 *            The nick of the user.
	 */
	public void onQuit(String nick)
	{
		int index = this.getUserIndex(nick);
		if (index == -1)
			return;
		this.m_users.remove(index);
		index = this.getPlayerIndex(nick);
		if (index == -1)
			return;
		this.m_players.remove(index);
	}

	/**
	 * Called to echo messages to a player's teammates.
	 * 
	 * @param user
	 *            The user who sent the message.
	 * @param message
	 *            The message sent.
	 */
	private void onTeamMessage(IrcUser user, String message)
	{
		IrcUser check;
		if (!user.knowsTeam())
			return;
		for (int i = 0; i < this.m_players.size(); ++i)
		{
			check = this.m_players.elementAt(i);
			if (check.getNick().matches(user.getNick()))
				continue;
			if (check.knowsTeam() && user.checkClass() == check.checkClass())
				check.replyTo(user.getNick() + ": " + message, false);
		}
	}

	public void removePlayer(int index)
	{
		if (index < 0 || index > this.m_players.size())
			return;
		this.m_players.remove(index);
	}

	public void removePlayer(IrcUser player)
	{
		this.removePlayer(this.getPlayerIndex(player.getNick()));
	}

	@Override
	public void run()
	{
		Thread.currentThread().setDaemon(true);
		while (this.m_phase <= 4 && this.m_phase >= 0)
		{
			while (this.m_phase == 0)
				try
				{
					Thread.currentThread().wait();
				} catch (InterruptedException e)
				{
				}
			// Wait for setup phase.
			while (this.m_phase == 1)
				try
				{
					Thread.currentThread().wait();
				} catch (InterruptedException e)
				{
				}

			if (this.m_phase == 1)
			{
				// Wait for night phase.
				try
				{
					int waitTime = this.m_config.getSetting("nightTime", Settings.nightTime);
					int endTime = (int) (Math.random() * this.m_config.getSetting("endRange", Settings.endRange));
					int warningTime = 30;
					Thread.currentThread().wait(1000 * (waitTime - warningTime));
					this.say(Game.chooseMessage(Messages.msg_nightWarning, Integer.toString(warningTime)));
					Thread.currentThread().wait(1000 * warningTime);
					if (endTime > 1)
					{
						this.say(Game.chooseMessage(Messages.msg_nightEnding));
						Thread.currentThread().wait(1000 * endTime);
					}
				} catch (InterruptedException e)
				{
					System.err.println("Night ended early...");
				}
				this.calcNight();
			}

			// Wait for day phase.
			try
			{
				int waitTime = this.m_config.getSetting("nightTime", Settings.nightTime);
				int endTime = (int) (Math.random() * this.m_config.getSetting("endRange", Settings.endRange));
				int warningTime = 60;
				Thread.currentThread().wait(1000 * (waitTime - warningTime));
				this.say(Game.chooseMessage(Messages.msg_nightWarning, Integer.toString(warningTime)));
				Thread.currentThread().wait(1000 * warningTime);
				if (endTime > 1)
				{
					this.say(Game.chooseMessage(Messages.msg_nightEnding));
					Thread.currentThread().wait(1000 * endTime);
				}
			} catch (InterruptedException e)
			{
				System.err.println("Night ended early...");
			}
			this.calcDay();
		}
	}

	public void say(String message)
	{
		this.say(this.m_config.getSetting("channel", Settings.channel), message);
	}

	public void say(String target, String message)
	{
		this.m_bot.sendMessage(target, message);
	}

	public void startGame()
	{
		String users = this.m_users.elementAt(0).getNick();
		for (int i = 1; i < this.m_users.size(); ++i)
			users += ", " + this.m_users.elementAt(i).getNick();
		this.say(users + ": Welcome to Werewolf, the popular detective/party game!");
	}

	public void toChanserv(String message)
	{
		this.say(this.m_config.getSetting("chanserv", Settings.chanserv), message);
	}
}
