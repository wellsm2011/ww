package werewolf.define;

public interface ConsoleCommand
{
	public String[] getAliases();

	public void help(String args);

	public void onUse(String command, String args);
}
