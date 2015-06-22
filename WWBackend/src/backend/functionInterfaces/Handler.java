package backend.functionInterfaces;
@FunctionalInterface
public interface Handler<T>
{
	public void handle(T input);
}
