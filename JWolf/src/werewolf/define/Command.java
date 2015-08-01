package werewolf.define;

import werewolf.IrcUser;

public interface Command extends Messages
{
	/**
	 * Handles commands that are called by users. Responsible for handling error
	 * messaging if a command was mis-called.
	 *
	 * @param caller
	 *            The player who would call the command.
	 * @param arguments
	 *            A String containing any arguments passed to the command.
	 */
	public void call(IrcUser caller, String command, String arguments, boolean isChannel);

	/**
	 * @return All aliases (including main names) that can be used to call this
	 *         command.
	 */
	public String[] getAliases();

	/**
	 * @return The common name of this command.
	 */
	public String[] getCommands();

	/**
	 * Responsible for returning help messages to the user when called.
	 *
	 * @param caller
	 * @param arguments
	 * @param isChannel
	 */
	public void help(IrcUser caller, String command, String arguments, boolean isChannel);
}
