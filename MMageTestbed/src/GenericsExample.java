import lol.Zyra;
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

	public static class Pair2
	{
		public final Object	first;
		public final Object	second;

		public Pair2(Object first, Object second)
		{
			this.first = first;
			this.second = second;
		}
	}

	// String[] args
	public static void main(String... cheese)
	{
		Pair<String, Zyra> example = new Pair<>("This is a Zyra", new Zyra());
		Pair<Integer, String> example2 = new Pair<>(7, "Seven");

		Pair<Pair<String, Zyra>, Integer> example3 = new Pair<>(example, 7);

		example.first.charAt(0);

		U.p(example.first);
		U.p(example2.first);
	}

	// concat(String ... args)
	// concat("Hello World", "Goodbye", "Moon");
	// concat(new String[]{"Hello World", "Goodbye", "Moon"});
}
