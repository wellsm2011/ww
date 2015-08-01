package werewolf.define;

import werewolf.Game;

public interface Messages
{
	public static final String[]	msg_added			=
														{ "You have been added to this Werewolf game by " + Game.bold("[1]") + "." };
	public static final String[]	msg_quit			=
														{ Game.bold("[1]") + " escaped the village with their life..." };
	public static final String[]	msg_modkill			=
														{ Game.bold("[1]") + " has been modkilled by " + Game.bold("[2]") + "." };
	public static final String[]	msg_revealTeam1		=
														{ "There is " + Game.bold("one") + " other player on your team." };
	public static final String[]	msg_revealTeam2		=
														{ "There are " + Game.bold("[1]") + " other players on your team." };

	public static final String[]	msg_nightNone		=
														{};
	public static final String[]	msg_nightMaul		=
														{};
	public static final String[]	msg_nightShot		=
														{};
	public static final String[]	msg_nightMulti		=
														{};

	public static final String[]	msg_lynchHidden		=
														{};
	public static final String[]	msg_lynchWolf		=
														{};
	public static final String[]	msg_lynchMafia		=
														{};
	public static final String[]	msg_lynchEvil		=
														{};
	public static final String[]	msg_lynchNeutral	=
														{};
	public static final String[]	msg_lynchVillage	=
														{};

	public static final String[]	msg_nightWarning	=
														{ Game.bold("The sun slowly rises over the village. There are [1] seconds left before night can end!") };
	public static final String[]	msg_nightEnding		=
														{ Game.bold("The sun can be seen over the far off mountains. Night may end at any time!") };
	public static final String[]	msg_dayWarning		=
														{ Game.bold("The sun drops below the far off mountains. There are [1] seconds left before day can end!") };
	public static final String[]	msg_dayEnding		=
														{ Game.bold("The sun slowly dips out of view. Day may end at any time!") };
}
