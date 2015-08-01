package werewolf.util;

import werewolf.Game;
import werewolf.IrcUser;

public class PeekMod
{
	private boolean	isTrue		= false;
	private boolean	isInsane	= false;
	private boolean	isParanoid	= false;
	private boolean	isNaive		= false;
	private boolean	isConfused	= false;

	public PeekMod()
	{
	}

	public String peekClass(IrcUser target)
	{
		return this.peekClass(target, false);
	}

	public String peekClass(IrcUser target, boolean ignorant)
	{
		int check = this.isTrue && !ignorant ? 0 : 1;
		return Game.getClass(target.checkClass(), target.getRole().isEvilVillager());
	}

	public boolean peekClass(IrcUser target, int check)
	{
		return this.peekClass(target, check, false);
	}

	public boolean peekClass(IrcUser target, int check, boolean ignorant)
	{
		if (!ignorant)
		{
			if (this.isConfused)
				return Math.random() > .5;
			if (this.isNaive)
				return false;
			if (this.isParanoid)
				return true;
		}
		int element = this.isTrue && !ignorant ? target.checkClass() : target.checkClass();
		boolean result = element == check;
		if (this.isInsane)
			return !result;
		return result;
	}

	public String peekRole(IrcUser target, boolean ignorant)
	{
		if (this.isTrue && !ignorant)
			return target.getRole().name();
		return target.getRole().peekRole();
	}

	public void setConfused()
	{
		this.isConfused = true;
	}

	public void setInsane()
	{
		this.isInsane = true;
	}

	public void setNaive()
	{
		this.isNaive = true;
	}

	public void setParanoid()
	{
		this.isParanoid = true;
	}

	public void setTrue()
	{
		this.isTrue = true;
	}
}
