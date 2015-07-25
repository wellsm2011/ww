package game.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import config.core.Config;
import config.core.annotations.HasCustomConfigType;

@HasCustomConfigType
public class Atomic
{
	static
	{
		Config.registerType("Atomic", obj -> {
			Atomic res = new Atomic();
			for (String key : obj.keySet())
				res.data.put(key, obj.optString(key));
			return res;
		} , cur -> {
			JSONObject res = new JSONObject();
			for (Entry<String, String> c : ((Atomic) cur).getData().entrySet())
				res.put(c.getKey(), c.getValue());
			return res;
		});
	}

	private Map<String, String> data;

	public Atomic()
	{
		this.data = new HashMap<>();
	}

	private Map<String, String> getData()
	{
		return this.data;
	}
}
