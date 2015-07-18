package global;

import java.util.LinkedList;

import backend.functionInterfaces.Func;

public class Globals
{
	private static LinkedList<Func> onClose;

	static
	{
		Globals.onClose = new LinkedList<Func>();
	}

	/**
	 * Given a lambda, adds it to the internal onclose handler list.
	 *
	 * @param f
	 *            the lambda to add
	 */

	public static void addExitHandler(Func f)
	{
		Globals.onClose.add(f);
	}

	/**
	 * Wrapper call for System.exit, calls registered handlers before exiting.
	 */
	public static void exit()
	{
		for (Func f : Globals.onClose)
			f.exec();
		System.exit(0);
	}
}
