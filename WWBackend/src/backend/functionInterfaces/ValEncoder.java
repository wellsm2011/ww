package backend.functionInterfaces;

import org.json.JSONObject;

@FunctionalInterface
public interface ValEncoder
{
	public JSONObject encode(Object val);
}
