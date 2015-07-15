import config.core.Config;
import game.parseables.Phase;

public class Game
{
	public Game(Config config)
	{
		Phase[] phases = config.getSection("phases").getItems();
	}
}
