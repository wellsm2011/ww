import java.io.IOException;
import java.util.LinkedList;

import backend.U;
import config.core.Config;
import config.explorer.Explorer;
import editor.gui.EditorGui;

public class Driver
{

	public static void main(String[] args)
	{
		// testLoadStore();
		Driver.testConfig();
	}

	private static void testConfig()
	{
		Config config = new Config("config.json");
		U.p(config);
		config.writeToFile("parsedConfig.json");
		Explorer e = new Explorer(config);
		EditorGui gui = new EditorGui(e);
		gui.init();

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
