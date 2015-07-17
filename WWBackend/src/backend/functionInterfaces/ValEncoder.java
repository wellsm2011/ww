package backend.functionInterfaces;

import org.json.JSONObject;

@FunctionalInterface
public interface ValEncoder<T>
{
	public JSONObject encode(T input);
}
