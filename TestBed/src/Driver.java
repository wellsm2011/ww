import java.io.IOException;
import java.util.LinkedList;

import backend.U;
import config.core.Config;

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

	private static void testConfig()
	{
		Config config = new Config("config.json");
		U.p(config);
		config.writeToFile("parsedConfig.json");
	}

	@SuppressWarnings("unused")
	private static void testLoadStore()
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

}
