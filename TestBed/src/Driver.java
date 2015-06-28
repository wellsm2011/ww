import java.io.IOException;
import java.util.LinkedList;
import java.util.Map.Entry;

import backend.U;
import config.core.Config;
import config.core.SectionManager;

public class Driver
{

	public static void main(String[] args)
	{
		// Since people can't apparently figure out how to do this themselves
		// and have to go editing source code instead <_<
		U.setDebugLevel(9001);
		// testLoadStore();
		Driver.testConfig();
	}

	@SuppressWarnings("unused")
	private static void objLoadStoreExample()
	{
		LinkedList<String> bob;

		try
		{
			bob = U.objReadFromFile("bob.store");
		} catch (ClassNotFoundException | IOException e)
		{
			bob = new LinkedList<String>();
		}
		U.p("Loaded " + bob.size() + " entries!");
		U.p("Adding more...");
		for (int i = 0; i < 1000; i++)
			bob.add("Hello from " + U.getTimestamp());

		try
		{
			U.objWriteToFile(bob, "bob.store");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		U.p("Saved!");
	}

	private static void testConfig()
	{
		Config config = new Config("config.json");
		StringBuilder sb = new StringBuilder("\n");
		for (Entry<String, SectionManager> curSec : config.getAllMaps().entrySet())
		{
			SectionManager curSecMan = curSec.getValue();
			sb.append("Current Section Key: " + curSecMan.getKey() + "\n");
			for (String curElemKey : curSecMan.getKeys())
				sb.append("\t" + curElemKey + " -> " + curSecMan.getGettablesForKey(curElemKey) + "\n");
		}
		U.p(sb.toString());
		config.writeToFile("parsedConfig.json");
	}

}
