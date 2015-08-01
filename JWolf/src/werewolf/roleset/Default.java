package werewolf.roleset;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.Roleset;

public class Default implements Roleset
{
	Game				m_game;

	public final String	name		= "Default";
	private int			minPlayers	= 4;			// Minimum number of players
													// required to start a game.
	private int			maxPlayers	= 20;			// Maximum number of players
													// allowed in a game.
	private boolean		showRoles	= true;		// Reveals which roles are
													// alive; dead players'
													// roles are revealed on
													// death.

	public Default(Game game)
	{
		this.m_game = game;
	}

	@Override
	public void assinRoles(IrcUser[] players)
	{
	}

	@Override
	public int maxPlayers()
	{
		return this.maxPlayers;
	}

	@Override
	public int minPlayers()
	{
		return this.minPlayers;
	}

	@Override
	public String name()
	{
		return null;
	}

	@Override
	public boolean showRoles()
	{
		return this.showRoles;
	}
}
