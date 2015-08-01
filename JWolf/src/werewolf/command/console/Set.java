package werewolf.command.console;

import javax.swing.JOptionPane;

import werewolf.Game;
import werewolf.Settings;
import werewolf.WerewolfHost;
import werewolf.define.ConsoleCommand;

public class Set implements ConsoleCommand
{
	private Game			m_game;
	private WerewolfHost	m_bot;

	public Set(WerewolfHost bot, Game game)
	{
		this.m_game = game;
		this.m_bot = bot;
	}

	@Override
	public String[] getAliases()
	{
		String[] result =
		{ "set" };
		return result;
	}

	@Override
	public void help(String args)
	{
		return;
	}

	@Override
	public void onUse(String command, String args)
	{
		String result;
		args = args.toLowerCase();

		if (args.startsWith("nick"))
		{
			result = JOptionPane.showInputDialog(null, "Enter Nickname:", this.m_game.getSettings().getSetting("nick", Settings.nick));
			if (result != null)
				this.m_game.getSettings().setSetting("nick", result);
		} else if (args.startsWith("network"))
		{
			result = JOptionPane.showInputDialog(null, "Enter IRC Network:", this.m_game.getSettings().getSetting("network", Settings.network));
			if (result != null)
				this.m_game.getSettings().setSetting("network", result);
		} else if (args.startsWith("channel"))
		{
			result = JOptionPane.showInputDialog(null, "Enter Channel Name:", this.m_game.getSettings().getSetting("channel", Settings.channel));
			if (result != null)
				this.m_game.getSettings().setSetting("channel", result);
		} else
			System.err.println("Unknown setting: " + args);
	}
}
