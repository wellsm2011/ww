import java.io.IOException;
import java.util.LinkedList;

import backend.U;

public class Driver
{

	public static void main(String[] args)
	{
		LinkedList<String> bob;

		try
		{
			bob = U.readObjectFromFile("bob.store");
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
			U.writeObjectToFile(bob, "bob.store");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		U.p("Saved!");
	}

}
