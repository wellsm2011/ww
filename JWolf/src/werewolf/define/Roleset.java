package werewolf.define;

import werewolf.IrcUser;

/**
 * A roleset handles initalization for a game of Werewolf. The roleset assignes
 * roles, items, and statuses to all players before returning. Rolesets also
 * define the maximum and minimum number of players, and can decide wither role
 * names are shown on death.
 */
public interface Roleset
{
	public void assinRoles(IrcUser[] players);

	public int maxPlayers();

	public int minPlayers();

	public String name();

	public boolean showRoles();
}
