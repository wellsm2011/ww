package config;

import java.util.HashMap;

import backend.U;
import backend.json.JSONException;
import backend.json.JSONObject;

/**
 * Testbed config, uses the JSON parser to parse different parts of a given
 * config file. Currently has a simple example for items, in the future, order
 * of loading will probably matter, for example: Actions will probably need to
 * be loaded before Items, if Items refer to Actions, etc, etc. Bi-directional
 * links would be nice to avoid architecturely, but wouldn't be a show-stopper.
 *
 * Overall structure of this class should basically be
 *
 * Constructor - Reads the file, then calls parsing subroutines
 *
 * Parsing Subroutines - Pulls the relevant section(s?) from the main json file,
 * and passes individual data-sections to the constructors of other classes
 * (Items, etc, etc, etc)
 *
 * Storage - Also provides hashmaps (May become hashtables if thread-safety
 * becomes a requirement) to parsed items for later use.
 *
 */
public class Config
{
	private HashMap<String, Item>	items;

	public Config(String filename)
	{
		try
		{
			JSONObject data = new JSONObject(U.readFile(filename));
			this.parseItems(data);

		} catch (JSONException e)
		{
			U.e("Error parsing config file", e);
			System.exit(1);
		} catch (NullPointerException e)
		{
			U.e("Internal error parsing config file.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public HashMap<String, Item> getItems()
	{
		return this.items;
	}

	private void parseItems(JSONObject data)
	{
		JSONObject items = data.getJSONObject("items");
		this.items = new HashMap<String, Item>();

		for (String cur : items.keySet())
			this.items.put(cur, new Item(cur, items.getJSONObject(cur)));
		U.p("Parsed Actions: " + this.items);
	}
}
