import java.util.LinkedList;

import backend.U;

public class GenericsExample
{
	public static class Pair<T, E>
	{
		public final T	first;
		public final E	second;

		public Pair(T first, E second)
		{
			this.first = first;
			this.second = second;
		}
	}

	public static void main(String... cheese)
	{
		LinkedList<String> list1 = new LinkedList<>();

		list1.add("Hello World");

		list1.get(0);

		Pair<String, Integer> examplePair = new Pair<>("Hello World", 5);
		Pair<Pair<String, Integer>, String> example2 = new Pair<>(examplePair, "Scrub");
		U.p(examplePair.first + examplePair.second);

		Pair<Integer, String> stuff = getStuff(5);
		U.p(stuff.first + stuff.second);
	}

	public static <T> Pair<T, String> getStuff(T thing)
	{
		return new Pair<T, String>(thing, "Stuff");
	}
}
