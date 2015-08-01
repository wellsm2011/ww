package werewolf.util;

import java.util.Vector;

import werewolf.IrcUser;

public class RolesetFactory
{
	private static Vector<String>	roleset	= new Vector<String>();

	public static void assignRoles(IrcUser[] players)
	{

	}

	public static void construct(int minPlayers, String input) throws ClassNotFoundException
	{
		String[] roles = input.split("\\|");

		for (int i = 0; i < roles.length; ++i)
		{

		}

		String child = String.class.getName(); // not necessary, can just use
												// String.class directly
		Class childClass;
		childClass = Class.forName(child);
		Class parentClass = Object.class;
	}

	public static void construct(String input) throws ClassNotFoundException
	{
		RolesetFactory.construct(3, input);
	}

	public static void initalize()
	{
		RolesetFactory.roleset.removeAllElements();
	}
}
