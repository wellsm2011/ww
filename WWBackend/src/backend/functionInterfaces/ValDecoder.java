package backend.functionInterfaces;

import org.json.JSONObject;

@FunctionalInterface
public interface ValDecoder
{
	public <T> T decode(JSONObject input);
}
