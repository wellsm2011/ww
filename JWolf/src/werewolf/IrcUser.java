package werewolf;

import java.util.Vector;

import org.jibble.pircbot.User;

import werewolf.define.Interactable;
import werewolf.define.interactable.Item;
import werewolf.define.interactable.Role;
import werewolf.define.interactable.Status;
import werewolf.role.Person;
import werewolf.util.Infection;
import werewolf.util.Kill;
import werewolf.util.PeekMod;
import werewolf.util.Protection;

public class IrcUser implements Interactable
{
	private String				m_nick			= "";
	private String				m_user			= "";
	private String				m_host			= "";
	private boolean				m_isOP			= false;
	private boolean				m_isVoice		= false;
	private boolean				m_isConnected	= true;
	private boolean				m_isAdmin		= false;
	private Game				m_game;
	private WerewolfHost		m_bot;

	private Role				m_role;
	private Vector<Item>		m_items			= new Vector<Item>();
	private Vector<Status>		m_statuses		= new Vector<Status>();
	private Vector<Infection>	m_infections	= new Vector<Infection>();
	private Vector<Kill>		m_kills			= new Vector<Kill>();
	private Vector<Protection>	m_protections	= new Vector<Protection>();

	public IrcUser(Game game, String nick, String user, String host)
	{
		this.m_nick = nick;
		this.m_user = user;
		this.m_host = host;
		this.m_game = game;
		this.m_role = new Person(game, this);
		this.m_bot = game.getBot();
	}

	public IrcUser(Game game, User user)
	{
		this.m_nick = user.getNick();
		this.m_isOP = user.isOp();
		this.m_isVoice = user.hasVoice();
		this.m_game = game;
		this.m_role = new Person(game, this);
		this.m_bot = game.getBot();
	}

	@Override
	public void assigned()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).assigned();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).assigned();
		this.m_role.assigned();
	}

	@Override
	public void blockTrigger()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).blockTrigger();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).blockTrigger();
		this.m_role.blockTrigger();
	}

	@Override
	public void call(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		String[] aliases;
		for (int i = 0; i < this.m_items.size(); ++i)
		{
			aliases = this.m_items.elementAt(i).getAliases();
			for (int j = 0; j < aliases.length; ++j)
				if (aliases[j].equals(command))
				{
					this.m_items.elementAt(i).call(caller, command, arguments, isChannel);
					return;
				}
		}
		for (int i = 0; i < this.m_statuses.size(); ++i)
		{
			aliases = this.m_statuses.elementAt(i).getAliases();
			for (int j = 0; j < aliases.length; ++j)
				if (aliases[j].equals(command))
				{
					this.m_statuses.elementAt(i).call(caller, command, arguments, isChannel);
					return;
				}
		}
		aliases = this.m_role.getAliases();
		for (int i = 0; i < aliases.length; ++i)
			if (aliases[i].equals(command))
			{
				this.m_role.call(caller, command, arguments, isChannel);
				return;
			}
	}

	public int checkClass()
	{
		return this.m_role.checkClass();
	}

	/**
	 * Removes OP from this user in the game channel.
	 */
	public void deop()
	{
		if (!this.m_isOP)
			return;
		this.m_bot.deOp(this.m_game.getSettings().getSetting("channel", Settings.channel), this.getNick());
	}

	/**
	 * Removes voice for this user in the game channel.
	 */
	public void devoice()
	{
		if (!this.m_isVoice)
			return;
		this.m_bot.deVoice(this.m_game.getSettings().getSetting("channel", Settings.channel), this.getNick());
	}

	public boolean equals(IrcUser orig)
	{
		return orig.getNick() == this.getNick() && orig.isConnected() == this.isConnected();
	}

	public boolean equals(String orig)
	{
		return orig.equalsIgnoreCase(this.getNick());
	}

	@Override
	public String[] getAliases()
	{
		String[] aliases = new String[0];
		for (int i = 0; i < this.m_items.size(); ++i)
			aliases = Game.concat(aliases, this.m_items.elementAt(i).getAliases());
		for (int i = 0; i < this.m_statuses.size(); ++i)
			aliases = Game.concat(aliases, this.m_statuses.elementAt(i).getAliases());
		return Game.concat(aliases, this.m_role.getAliases());
	}

	/**
	 *
	 * @return The bot object that this user is a member of.
	 */
	public WerewolfHost getBot()
	{
		return this.m_bot;
	}

	@Override
	public String[] getCommands()
	{
		String[] commands = new String[0];
		for (int i = 0; i < this.m_items.size(); ++i)
			commands = Game.concat(commands, this.m_items.elementAt(i).getCommands());
		for (int i = 0; i < this.m_statuses.size(); ++i)
			commands = Game.concat(commands, this.m_statuses.elementAt(i).getCommands());
		return Game.concat(commands, this.m_role.getCommands());
	}

	/**
	 *
	 * @return The game object that this user is a member of.
	 */
	public Game getGame()
	{
		return this.m_game;
	}

	/**
	 *
	 * @return The host string of this user.
	 */
	public String getHost()
	{
		return this.m_host;
	}

	public Item[] getItems()
	{
		return this.m_items.toArray(new Item[this.m_items.size()]);
	}

	/**
	 *
	 * @return The nick string of this user.
	 */
	public String getNick()
	{
		return this.m_nick;
	}

	public boolean getPreference(String key, boolean value)
	{
		return this.m_game.getSettings().getPreference(this.getNick() + "-" + key, value);
	}

	public int getPreference(String key, int value)
	{
		return this.m_game.getSettings().getPreference(this.getNick() + "-" + key, value);
	}

	public String getPreference(String key, String value)
	{
		return this.m_game.getSettings().getPreference(this.getNick() + "-" + key, value);
	}

	public Role getRole()
	{
		return this.m_role;
	}

	public Status[] getStatuses()
	{
		return this.m_statuses.toArray(new Status[this.m_statuses.size()]);
	}

	/**
	 *
	 * @return Returns the user string of this user.
	 */
	public String getUser()
	{
		return this.m_user;
	}

	/**
	 *
	 * @return True if and only if the user is in the bot admin list, or the
	 *         user has OP in the game channel.
	 */
	public boolean hasAccess()
	{
		return this.isOp() || this.isBotAdmin();
	}

	@Override
	public void help(IrcUser caller, boolean isChannel)
	{
	}

	@Override
	public void help(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		String[] aliases;
		for (int i = 0; i < this.m_items.size(); ++i)
		{
			aliases = this.m_items.elementAt(i).getAliases();
			for (int j = 0; j < aliases.length; ++j)
				if (aliases[j].equals(command))
				{
					this.m_items.elementAt(i).help(caller, command, arguments, isChannel);
					return;
				}
		}
		for (int i = 0; i < this.m_statuses.size(); ++i)
		{
			aliases = this.m_statuses.elementAt(i).getAliases();
			for (int j = 0; j < aliases.length; ++j)
				if (aliases[j].equals(command))
				{
					this.m_statuses.elementAt(i).help(caller, command, arguments, isChannel);
					return;
				}
		}
		aliases = this.m_role.getAliases();
		for (int i = 0; i < aliases.length; ++i)
			if (aliases[i].equals(command))
			{
				this.m_role.help(caller, command, arguments, isChannel);
				return;
			}
	}

	@Override
	public void infectBlossom(Role conversion, IrcUser user)
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).infectBlossom(conversion, user);
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).infectBlossom(conversion, user);
		this.m_role.infectBlossom(conversion, user);
		this.m_role = conversion;
	}

	@Override
	public void infectTrigger()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).infectTrigger();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).infectTrigger();
		this.m_role.infectTrigger();
	}

	/**
	 *
	 * @return True if and only if the user has admin status in the bot.
	 */
	public boolean isBotAdmin()
	{
		return this.m_isAdmin;
	}

	/**
	 *
	 * @return True if and only if the user is present in the channel.
	 */
	public boolean isConnected()
	{
		return this.m_isConnected;
	}

	@Override
	public boolean isMason()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			if (this.m_items.elementAt(i).isMason())
				return true;
		for (int i = 0; i < this.m_statuses.size(); ++i)
			if (this.m_statuses.elementAt(i).isMason())
				return true;
		return this.m_role.isMason();
	}

	/**
	 *
	 * @return True if and only if the user has OP in the game channel.
	 */
	public boolean isOp()
	{
		return this.m_isOP;
	}

	/**
	 *
	 * @return True if and only if this user is playing in the current game.
	 */
	public boolean isPlayer()
	{
		return this.m_game.isPlayer(this);
	}

	/**
	 *
	 * @return True if and only if the user is still a member of the game
	 *         channel.
	 */
	public boolean isUser()
	{
		return this.m_game.isUser(this);
	}

	/**
	 *
	 * @return True if and only if the user has voice in the game channel.
	 */
	public boolean isVoice()
	{
		return this.m_isVoice;
	}

	@Override
	public boolean killHold()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			if (this.m_items.elementAt(i).killHold())
				return true;
		for (int i = 0; i < this.m_statuses.size(); ++i)
			if (this.m_statuses.elementAt(i).killHold())
				return true;
		return this.m_role.killHold();
	}

	@Override
	public void killTrigger()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).killTrigger();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).killTrigger();
		this.m_role.killTrigger();
	}

	public boolean knowsTeam()
	{
		return this.m_role.knowsTeam();
	}

	@Override
	public int mafiaParity()
	{
		int parity = 0;
		for (int i = 0; i < this.m_items.size(); ++i)
			parity += this.m_items.elementAt(i).mafiaParity();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			parity += this.m_statuses.elementAt(i).mafiaParity();
		return parity + this.m_role.mafiaParity();
	}

	/**
	 * Marks this user as a player.
	 */
	public void makePlayer()
	{
		if (!this.isPlayer())
			this.m_game.addPlayer(this);
	}

	/**
	 * Removes any player mark from this user.
	 */
	public void makeUser()
	{
		if (this.isPlayer())
			this.m_game.removePlayer(this);
	}

	/**
	 * Sends the user a private message.
	 *
	 * @param message
	 *            The message to send.
	 */
	public void message(String message)
	{
		this.message(message, false);
	}

	/**
	 * Sends the user a private message.
	 *
	 * @param message
	 *            The message to send.
	 * @param toChannel
	 *            If true, a notice will respond directly to the channel.
	 */
	public void message(String message, boolean toChannel)
	{
		if (this.getPreference("notice", false))
			this.notice(message, toChannel);
		else
			this.m_game.say(this.m_nick, message);
	}

	@Override
	public String name()
	{
		return this.m_role.name();
	}

	@Override
	public boolean nightlyHold()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			if (this.m_items.elementAt(i).nightlyHold())
				return true;
		for (int i = 0; i < this.m_statuses.size(); ++i)
			if (this.m_statuses.elementAt(i).nightlyHold())
				return true;
		return this.m_role.nightlyHold();
	}

	/**
	 * Sends the user a notice.
	 *
	 * @param message
	 *            The message to send.
	 * @param toChannel
	 *            True if the message should be sent with the channel's tag.
	 *            (eg, <B>[#bots] Hello there!</B>)
	 */
	private void notice(String message, boolean toChannel)
	{
		if (toChannel)
			message = "[" + this.m_game.getSettings().getSetting("channel", Settings.channel) + "] " + message;
		this.m_game.notice(this.getNick(), message);
	}

	@Override
	public boolean onBlocked()
	{
		boolean changed = false;
		for (int i = 0; i < this.m_items.size(); ++i)
			changed = this.m_items.elementAt(i).onBlocked() || changed;
		for (int i = 0; i < this.m_statuses.size(); ++i)
			changed = this.m_statuses.elementAt(i).onBlocked() || changed;
		return this.m_role.onBlocked() || changed;
	}

	/**
	 * Changes this user's nick string.
	 *
	 * @param newNick
	 *            The new nick string.
	 */
	public void onNickChange(String newNick)
	{
		this.m_nick = newNick;
	}

	/**
	 * Changes this user's op status.
	 *
	 * @param isOp
	 *            True if the user gained OP, otherwise false.
	 */
	public void onOpChange(boolean isOp)
	{
		this.m_isOP = isOp;
	}

	@Override
	public void onPeek(IrcUser peeker)
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).onPeek(peeker);
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).onPeek(peeker);
		this.m_role.onPeek(peeker);
	}

	/**
	 * Changes this user's voice status.
	 *
	 * @param isVoice
	 *            True if the user gained voice, otherwise false.
	 */
	public void onVoiceChange(boolean isVoice)
	{
		this.m_isVoice = isVoice;
	}

	/**
	 * Gives this user OP in the game channel.
	 */
	public void op()
	{
		if (this.m_isOP)
			return;
		this.m_bot.op(this.m_game.getSettings().getSetting("channel", Settings.channel), this.getNick());
	}

	@Override
	public int peekClass()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			if (this.m_items.elementAt(i).peekClass() >= 0)
				return this.m_items.elementAt(i).peekClass();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			if (this.m_statuses.elementAt(i).peekClass() >= 0)
				return this.m_statuses.elementAt(i).peekClass();
		return this.m_role.peekClass();
	}

	public String peekRole()
	{
		return this.m_role.peekRole();
	}

	@Override
	public void peekTrigger(PeekMod additions)
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).peekSetup(additions);
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).peekSetup(additions);
		this.m_role.peekSetup(additions);

		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).peekTrigger(additions);
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).peekTrigger(additions);
		this.m_role.peekTrigger(additions);
	}

	@Override
	public void protectTrigger()
	{
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).protectTrigger();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).protectTrigger();
		this.m_role.protectTrigger();
	}

	/**
	 * Replies to the user in the context they sent the message.
	 *
	 * @param message
	 *            The message to send.
	 * @param fromChannel
	 *            True if this method is a reply to a message from the channel.
	 */
	public void replyTo(String message, boolean fromChannel)
	{
		if (fromChannel)
			this.m_game.say(this.getNick(), message);
		else
			this.message(message);
	}

	@Override
	public void roundEnd()
	{
		for (int i = 0; i < this.m_protections.size(); ++i)
			if (this.m_protections.elementAt(i).isFinished())
				this.m_protections.remove(i);

		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).roundEnd();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).roundEnd();
		this.m_role.roundEnd();
	}

	/**
	 * Sets the host string of this user.
	 *
	 * @param hostname
	 *            The new host string.
	 */
	public void setHost(String hostname)
	{
		this.m_host = hostname;
	}

	public void setPreference(String key, boolean value)
	{
		this.m_game.getSettings().setPreference(this.getNick() + "-" + key, value);
	}

	public void setPreference(String key, int value)
	{
		this.m_game.getSettings().setPreference(this.getNick() + "-" + key, value);
	}

	public void setPreference(String key, String value)
	{
		this.m_game.getSettings().setPreference(this.getNick() + "-" + key, value);
	}

	/**
	 * Sets the user string of this user.
	 *
	 * @param username
	 *            The new user string.
	 */
	public void setUser(String username)
	{
		this.m_user = username;
	}

	@Override
	public void stopKill(Kill kill)
	{
		for (int i = 0; i < this.m_protections.size(); ++i)
			this.m_protections.elementAt(i).protect(kill);
		for (int i = 0; i < this.m_items.size(); ++i)
			this.m_items.elementAt(i).stopKill(kill);
		for (int i = 0; i < this.m_statuses.size(); ++i)
			this.m_statuses.elementAt(i).stopKill(kill);
		this.m_role.stopKill(kill);
	}

	/**
	 * Voices this user in the game channel.
	 */
	public void voice()
	{
		if (this.m_isVoice)
			return;
		this.m_bot.voice(this.m_game.getSettings().getSetting("channel", Settings.channel), this.getNick());
	}

	@Override
	public int votePower(IrcUser[][] history, IrcUser[] players)
	{
		int power = 0;
		for (int i = 0; i < this.m_items.size(); ++i)
			power += this.m_items.elementAt(i).votePower(history, players);
		for (int i = 0; i < this.m_statuses.size(); ++i)
			power += this.m_statuses.elementAt(i).votePower(history, players);
		return power + this.m_role.votePower(history, players);
	}

	@Override
	public int wolfParity()
	{
		int parity = 0;
		for (int i = 0; i < this.m_items.size(); ++i)
			parity += this.m_items.elementAt(i).wolfParity();
		for (int i = 0; i < this.m_statuses.size(); ++i)
			parity += this.m_statuses.elementAt(i).wolfParity();
		return parity + this.m_role.wolfParity();
	}
}
