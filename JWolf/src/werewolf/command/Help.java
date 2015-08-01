package werewolf.command;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Command;
import werewolf.define.interactable.Role;

public class Help implements Command
{
	Command[]	m_commands;
	Game		m_game;

	public Help(Game game, Command[] commands)
	{
		this.m_game = game;
		this.m_commands = commands;
	}

	@Override
	public void call(IrcUser caller, String command, String arguments, boolean isChannel)
	{
		Role role = caller.getRole();
		String[] commands = role.getCommands();
		for (int i = 0; i < commands.length; ++i)
			if ((arguments.toLowerCase() + " ").startsWith(commands[i] + " "))
			{
				role.help(caller, commands[i], arguments.substring(commands[i].length()), isChannel);
				return;
			}
		for (int i = 0; i < this.m_commands.length; ++i)
		{
			commands = this.m_commands[i].getAliases();
			for (int j = 0; j < commands.length; ++j)
				if ((arguments.toLowerCase() + " ").startsWith(commands[j] + " "))
				{
					this.m_commands[i].help(caller, commands[j], arguments.substring(commands[j].length()), isChannel);
					return;
				}
		}
	}

	@Override
	public String[] getAliases()
	{
		String[] result =
		{ "help" };
		return result;
	}

	@Override
	public String[] getCommands()
	{
		return new String[]
		{ "help" };
	}

	@Override
	public void help(werewolf.IrcUser caller, String command, String arguments, boolean isChannel)
	{

	}
}
