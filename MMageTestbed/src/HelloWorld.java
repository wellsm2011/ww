import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Scanner;

import lol.Caitlyn;
import lol.Champion; //from lol import *
import lol.Fizz;
import lol.Graves;
import lol.Zyra;
import backend.U;

public class HelloWorld
{
	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		Scanner keyboard = new Scanner(System.in);
		Scanner derp = keyboard;

		LinkedList<Integer> list = new LinkedList<>();
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(1, 52);
		// 1. 52. 2. 3

		LinkedList<Integer> list2 = new LinkedList<>();
		for (int i = 0; i < 3;)
			list2.add(++i);

		if (list.containsAll(list2))
			U.p("Taco");
		else
			U.p("Burrito");

		LinkedList<Champion> champList = new LinkedList<>();
		// Labeled breaks are usually a bit of bad practice unless heavily
		// documented -Sudo
		champLoop: while (true)
		{
			U.p("What champion do you want to look up?");
			String champ = keyboard.next();

			switch (champ)
			{
				case "Caitlyn":
					champList.add(new Caitlyn());
					break;
				case "Graves":
					champList.add(new Graves());
					break;
				case "Fizz":
					champList.add(new Fizz());
					break;
				case "Zyra":
					champList.add(new Zyra());
					break;
				default:
					U.p("Unknown champ.");
					break champLoop;
			}
		}

		for (Champion i : champList)
			U.p(i.getClass().getSimpleName() + "'s ult is named: " + i.getUltName() + "!");

		// if (champ == "Caitlyn");
	}

	public static void loopExample()
	{
		// First example: for loop
		for (int i = 0; i < 5; ++i)
			U.p(i);

		// Second example: for each loop
		int[] ary = new int[]
		{ 0, 1, 2, 3, 4 };
		for (int i : ary)
			U.p(i);

		// Third example: while loop
		int i = 0;
		while (i < 5)
			U.p(i++);

		// fourth example: do while loop
		i = 0;
		do
			U.p(i++);
		while (i < 5);
	}

	/**
	 * Repeats the given string a given number of times and returns.
	 *
	 * @param seq
	 *            The sequence to be repeated.
	 * @param repeat
	 *            The number of times to repeat.
	 * @return The correctly constructed string.
	 */
	public static String repeat(String seq, int repeat)
	{

		String output = seq;

		int i = 0;
		while (i++ < repeat)
			output += seq;

		return output;
	}
}
