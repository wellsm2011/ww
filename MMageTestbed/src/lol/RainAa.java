package lol;

import java.util.LinkedList;
import java.util.Scanner;

import backend.U;

public class RainAa
{
	public static void main(String[] args)
	{
		LinkedList<RainAa> champs = new LinkedList<>();
		Scanner keyboard = new Scanner(System.in);

		U.p("Please input three champion names and their attack damage:");

		for (int i = 0; i < 3; ++i)
			champs.add(new RainAa(keyboard.next(), keyboard.nextInt()));

		while (champs.size() > 1)
			RainAa.runAttack(champs);
		U.p(champs.getFirst() + " is the winner!");
	}

	private static void runAttack(LinkedList<RainAa> champs)
	{
		RainAa defender = champs.remove((int) (Math.random() * champs.size()));
		if (champs.get((int) (Math.random() * champs.size())).attack(defender))
		{
			String phrase = "Killing Spree! ";
			if (champs.size() == 3)
				phrase = "First Blood! ";
			U.p(phrase + defender + " has been killed.");
			return;
		}
		champs.add(defender);
	}

	private int		health	= (int) (900 * Math.random()) + 100;

	private String	name;

	private int		attackDamage;

	public RainAa(String name, int attackDamage)
	{
		this.name = name;
		this.attackDamage = attackDamage;
	}

	// Returns true on kill.
	public boolean attack(RainAa other)
	{
		if (this == other)
			return false;
		U.p(this + " attacks " + other + "...");
		other.health -= this.attackDamage;
		return other.health <= 0;
	}

	@Override
	public String toString()
	{
		return this.name + "[" + this.health + " hp, " + this.attackDamage + " ad]";
	}
}
