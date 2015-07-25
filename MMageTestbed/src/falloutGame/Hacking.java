package falloutGame;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;

import backend.U;

public class Hacking
{
	public static boolean guess(LinkedList<String> chosenWords, String correctWord, String guess)
	{

		int correctChar = 0;

		if (guess.equals(correctWord))
		{
			U.p("Access Granted.");
			return false;
		}

		// Checks to see how many letters are correct
		for (int i = 0; i < guess.length(); i++)
		{
			char user = guess.charAt(i);
			char system = correctWord.charAt(i);

			if (user == system)
				correctChar++;
		}

		U.p("" + correctChar + " / " + correctWord.length() + " Correct.");

		return true;
	}

	public static void main(String[] args) throws FileNotFoundException
	{
		Scanner input = new Scanner(System.in); // Temporary, main file would
												// give the Scanner
		// Turns the file into a list of passwords
		LinkedList<String> passwords = Hacking.readFile(new Scanner(new File("passwords.txt")));
		// Chooses the words from the passwords
		LinkedList<String> chosenWords = Hacking.select(passwords, input);

		for (int i = 0; i < chosenWords.size(); i++)
			// Prints out every chosen password to the user
			U.p(chosenWords.get(i) + " | " + chosenWords.get(++i));

		// Chooses one word to be the correct word
		String correctWord = chosenWords.get((int) (chosenWords.size() * Math.random()));

		int attempts = 5;
		boolean cont = true;

		// Runs the guess function until the user guesses 5 times or is correct.
		while (cont)
		{
			if (attempts <= 0)
			{
				U.p("Access Denied.");
				break;
			}
			String guess = input.next();
			if (chosenWords.contains(guess))
			{
				cont = Hacking.guess(chosenWords, correctWord, guess);
				attempts -= 1;
				if (attempts > 0 && cont)
					U.p("" + attempts + " attempts left.");
			} else
				U.p("Invalid entry. Please input a word exactly as shown above.");
		}
		input.close();
		// return (cont != cont);
	}

	public static LinkedList<String> readFile(Scanner file)
	{

		LinkedList<String> words = new LinkedList<>();

		while (file.hasNextLine())
			words.add(file.nextLine());

		return words;
	}

	public static LinkedList<String> select(LinkedList<String> passwords, Scanner keyboard)
	{

		U.p("What difficulty do you want from 1 - 5?");

		int diff = keyboard.nextInt();
		LinkedList<String> words = new LinkedList<>();

		if (diff > 0 && diff < 6)
		{

			int wordAmount = diff * 2 + 8;

			for (int i = 0; i < wordAmount; i++)
			{
				int pick = (int) (passwords.size() * Math.random());

				words.add(passwords.remove(pick));
				pick -= 1;
			}
		} else
			U.p("Unacceptable difficulty input. Please try again.");

		return words;
	}
}

/*
 * Fallout Game Psuedocode - Mastermind
 * 
 * Import file of passwords.
 * Choose a random number of them based on difficulty.
 * Choose one of them to be the correct password.
 * Print all chosen passwords to user
 * Prompt user to input a password
 * Check that the password is an actual password
 * Check every letter in user password against chosen password
 * Return # of correct letters to user
 * If success, congradumurate them and end game.
 */
