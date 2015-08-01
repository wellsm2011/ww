package werewolf.role;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.interactable.Role;

public class Person extends Role
{
	public Person(Game game, IrcUser owner)
	{
		super(game, owner);
	}

	@Override
	public String name()
	{
		return "Person";
	}
}
