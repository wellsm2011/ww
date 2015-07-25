package falloutGame;

import java.util.Scanner;

import backend.U;

public class Lockpicking
{
	public static boolean guess(int spot, int guess, int diff)
	{
		int dist = Math.abs(spot - guess);
		double close = 6.25 - 0.75 * diff;

		if (dist < close) // Victory
		{
			U.p("You picked the lock!");
			return true;
		}

		if (dist < 12.5 - 1.5 * diff) // Burning Up
		{
			U.p("You're sooooooo close!");
			return false;
		}

		if (dist < 25 - 3 * diff) // Hot
		{
			U.p("You're hot!");
			return false;
		}

		if (dist < 50 - 6 * diff) // Warm
		{
			U.p("You're kinda warm.");
			return false;
		}

		if (dist < 100 - 12 * diff) // Cold
		{
			U.p("You're really cold.");
			return false;
		}

		U.p("You're freezing! You're not even close bby.");
		return false;
	}

	public static void main(String[] args)
	{
		U.p("Welcome to the Lockpicking minigame. The objective is to find the");
		U.p("sweet spot of the lock. To do this, you may guess #s between 0 and 180");
		U.p("and you will get messages based on how close you are.");
		U.p("");

		Scanner keyboard = new Scanner(System.in);
		U.p("Please input your difficulty level.");
		int diff = keyboard.nextInt();
		int attempts = 10 - diff * 1;
		int sweetSpot = (int) (Math.random() * 181);
		boolean cont = true;

		U.p(sweetSpot); // Debugging and stuff.

		while (attempts > 0 && cont)
		{
			U.p("");
			U.p("Please input a guess.");
			boolean victory = Lockpicking.guess(sweetSpot, keyboard.nextInt(), diff);
			attempts -= 1;
			if (cont = !victory)
				U.p("You have " + attempts + " attempts left.");
		}

		keyboard.close();
		// return victory;
	}
}

/*
 * Fallout Lockpicking - "Hot / Cold" User must get close to the "sweet spot" of
 * the lock. User only has 5 attempts to do so. The spot starts at a position
 * between 0 and 180. User inputs an int between those two numbers as a guess.
 * The User is told about how far away they are from the spot. (Burning Up, Hot,
 * Warm, Cold, Not even close bby) If the User gets within 2.5-5 of the spot,
 * they win. Difficulty may decrease the sweet spot win limit.
 */
