package falloutGame;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

import backend.U;

public class Explore
{
	//public static class Room
	//{
		//private Room[]	exits	= new Room[4];

		//public Room(int n, int s, int e, int w)
		//{

		//}
	//}

	public static void main(String[] cheese)
	{
		int[][] rooms =
		{
		{ 1, 0, 0, 0 },
		{ 0, 0, -1, 1 },
		{ 0, -1, 1, 0 },
		{ -1, 0, 0, 0 } };
		LinkedList<Room> roomMap = new LinkedList<>();
		
		for(int i = 0; i > 4; i++)
		{
			//roomMap.add(new Room(rooms[i]));
		}

		String[] exit = new String[]
		{ "North", "East", "South", "West" };

		int currentRoom = 0;
		int destination = 0;
		Scanner keyboard = new Scanner(System.in);

		while (true)
		{
			LinkedList<String> printout = new LinkedList<>();
			//if (roomMap.get(currentRoom).north != 0);
			
			
			
			
			
			//for (int i = 0; i < 4; i++)
			//{
				//if (roomMap.get(i) != 0)
					//printout.addLast(exit[i]);
			//}
			//LinkedList<E> list;
			//list.toArray(new E[list.size()]);
			//Arrays.toString({x, y, z});
			//prints out: "[x, y, z]"
			U.p("The valid exit(s) are: " + printout);
			U.p("Where do you want to go?");

			String input = keyboard.next();
			if (printout.contains(input))
			{
				if (input.charAt(0) == 'n' || input.charAt(0) == 'N')
					destination = 0;

				if (input.charAt(0) == 'e' || input.charAt(0) == 'E')
					destination = 1;

				if (input.charAt(0) == 's' || input.charAt(0) == 'S')
					destination = 2;

				if (input.charAt(0) == 'w' || input.charAt(0) == 'W')
					destination = 3;

				currentRoom += rooms[currentRoom][destination];
			} else
				U.p("I can't accept that command. Please input the destination with a capital letter, spelled correctly.");
		}
	}
}
/*
 * Blah blah commit the changes he says, even if its unfinished
 * 
 * Room data
 *
 * Rooms must contain data for the following.
 *
 * Exits - North, East, South, West. 0-Nope, 1-Open, 2-Locked
 * Hacking Terminal - None, Unhacked, Hacked
 * Locks - None, Unpicked, Picked
 * Keycard? - Not Necessary, Very Necessary
 * Enemies - Which type (0 = None, 1 = 1st...)
 * Loot - None, Obtainable, Unobtainable
 * Loot2 - Which type of loot
 *
 * Scrublet Version - 0-3 Exits, N E S W
 *
 */
