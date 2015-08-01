package werewolf.role;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.interactable.Role;

public class Villager extends Role
{
	public Villager(Game game, IrcUser owner)
	{
		super(game, owner);
	}
}
