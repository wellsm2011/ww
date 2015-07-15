package backend.functionInterfaces;

import org.json.JSONObject;

@FunctionalInterface
public interface ValDecoder<T>
{
	public T decode(JSONObject input);
}
