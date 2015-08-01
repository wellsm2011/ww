package werewolf.item;

import werewolf.Game;
import werewolf.IrcUser;
import werewolf.define.interactable.Item;

public class Gun extends Item
{
	Game	m_game;
	IrcUser	m_owner;

	public Gun(Game game, IrcUser owner)
	{
		this.m_game = game;
		this.m_owner = owner;
	}

	@Override
	public void assigned()
	{
	}
}
