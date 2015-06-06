package config;

import backend.U;
import backend.json.JSONObject;

public class Item
{

	public Item(String name, JSONObject data)
	{
		for (String cur : data.keySet())
			U.p(name + " - " + cur + ":" + data.get(cur).toString());
	}

}
