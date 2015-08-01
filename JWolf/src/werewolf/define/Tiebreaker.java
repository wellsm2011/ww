package werewolf.define;

import werewolf.Game;
import werewolf.IrcUser;

public interface Tiebreaker
{
	public IrcUser breakFinalTie(Game currentGame);

	public IrcUser[] breakTie(Game currentGame);
}
