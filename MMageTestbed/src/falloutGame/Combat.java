package falloutGame;

import java.util.Scanner;

import backend.U;

public class Combat
{
	public static void main(String[] cheese)
	{
		Scanner keyboard = new Scanner(System.in);
		int diff = 1; // Import from somewhere else later
		int playerHP = 100;
		int enemyHP = 100;
		int playerAttack = 10;
		int enemyAttack = 5 + (int) (diff * 0.5 + 0.5);
		int damage = 0;
		boolean cont = true;

		U.p("Welcome to combat. In this minigame, it is your goal to make");
		U.p("the enemy's HP hit 0 before he does the same to you. You may");
		U.p("input 'a' (attack), 'd' (defend), or 'r' (run) until the battle ends.");

		while (cont)
		{
			U.p("");
			U.p("Please input a command.");
			U.p("");
			char command = keyboard.next().charAt(0);
			int defend = 0;

			if (command == 'a') // Attack
			{
				damage = playerAttack + (int) (Math.random() * 5 - 2);
				enemyHP -= damage; // Random variation in damage
				U.p("You dealt " + damage + " damage!");
			}

			if (command == 'd') // Defend
			{
				defend = playerAttack / 2 + (int) (Math.random() * 2);
				U.p("You protect yourself from " + defend + " damage!");
			}

			if (command == 'r') // Run
			{
				U.p("You run from the battle!");
				cont = false;
				break;
				// return false;
			}

			if (enemyHP <= 0)
			{
				U.p("You have defeated the enemy!");
				break;
				// return true;
			}

			else
			{
				damage = enemyAttack + (int) (Math.random() * 5 - 2);
				if (defend >= damage)
				{
					damage = 0;
					U.p("You fully defend the enemy's attack!");
				} else
				{
					damage = damage - defend;
					playerHP -= damage;
					U.p("The enemy deals " + damage + " damage to you!");
				}
			}

			if (playerHP <= 0)
			{
				U.p("You have been defeated!");
				cont = false;
				break;
				// return false;
			}

			U.p("");
			U.p("You have " + playerHP + " HP left.");
			U.p("The enemy has " + enemyHP + " HP left.");

		}
		keyboard.close();
	}
}
