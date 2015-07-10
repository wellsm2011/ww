import parseables.Phase;
import config.core.Config;

public class Game
{
	public Game(Config config)
	{
		Phase[] phases = config.getSection("phases").getItems();
	}
}
