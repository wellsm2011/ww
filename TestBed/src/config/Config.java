package config;

import java.util.HashMap;




import backend.U;
import backend.json.JSONException;
import backend.json.JSONObject;

public class Config {
	private HashMap<String, Item> items;

	public Config(String filename) {
		try {
			JSONObject data = new JSONObject(U.readFile(filename));
			parseItems(data);
			
		} catch (JSONException e) {
			U.e("Error parsing config file", e);
			System.exit(1);
		} catch (NullPointerException e) {
			U.e("Error parseing config file.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void parseItems(JSONObject data) {
		JSONObject items = data.getJSONObject("items");
		this.items = new HashMap<String, Item>();

		for (String cur : items.keySet())
			this.items.put(cur, new Item(cur, items.getJSONObject(cur)));
		U.p("Parsed Actions: " + this.items);
	}

	public HashMap<String, Item> getItems()
	{
		return items;
	}
}
