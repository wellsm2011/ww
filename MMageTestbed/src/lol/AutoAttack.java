package lol;

import java.util.LinkedList;
import java.util.Scanner;

import backend.U;

public class AutoAttack
{
	public static void main(String[] args)
	{
		Scanner keyboard = new Scanner(System.in);
		LinkedList<AutoAttack> champs = new LinkedList<>();

		for (int i = 0; i < 3; ++i)
		{
			U.p("Input a Champion Name");
			String champName = keyboard.next();
			U.p("Input the Champion's attack damage");
			int attackDamage = keyboard.nextInt();
			int hitPoints = (int) (900 * Math.random() + 100);
			champs.add(new AutoAttack(champName, attackDamage, hitPoints));
		}
		AutoAttack.randomAttack(champs);
		keyboard.close();
	}

	public static void randomAttack(LinkedList<AutoAttack> champs)
	{
		while (1 != champs.size())
		{
			int pickChamp = (int) (champs.size() * Math.random());
			int pickChamp2 = pickChamp;
			while (pickChamp == pickChamp2)
				pickChamp2 = (int) (champs.size() * Math.random());
			champs.get(pickChamp).takeHit(champs.get(pickChamp2).getAttack());
			if (champs.get(pickChamp).isDead())
			{
				U.p(champs.get(pickChamp2).getName() + " has slain " + champs.get(pickChamp).getName());
				champs.remove(pickChamp);
			}
		}
		U.p(champs.get(0).getName() + " is the Victor!");

	}

	private String	champName;

	// class Taco
	// LinkedList<Toppings> toppings;
	//
	// public Taco(LinkedList<Toppings> toppings) {
	// this.toppings = toppings;
	// }

	// Taco taco = new Taco(new LinkedList<Toppings>());

	// for i in range(3)
	// strName[i] = input
	// intHP[i] = math.rand(1,1000)
	// intDmg[i] = input

	// intTemp = math.rand(0,2)
	// intTemp2 = math.rand(0,2)

	// if intTemp =/= intTemp2
	// intHP[intTemp2] = intHP[intTemp2] - intDmg[intTemp]

	// LinkedList<AutoAttack> champs = new LinkedList<>();
	// taco

	private int		attack;

	private int		hp;

	public AutoAttack(String name, int attack, int hp)
	{
		this.champName = name;
		this.attack = attack;
		this.hp = hp;
	}

	public int getAttack()
	{
		return this.attack;
	}

	public String getName()
	{
		return this.champName;
	}

	public boolean isDead()
	{
		return this.hp <= 0;
	}

	public void takeHit(int damage)
	{
		this.hp -= damage;
	}
}
