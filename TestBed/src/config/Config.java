package config;

import global.Globals;

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
	private HashMap<String, HashMap<String, ?>>	maps;

	public Config(String filename)
	{
		try
		{
			JSONObject data = new JSONObject(U.readFile(filename));
			this.maps = new HashMap<String, HashMap<String, ?>>();
			this.parseItems(data);
			this.parseStatuses(data);
			this.parseRoles(data);

		} catch (JSONException e)
		{
			U.e("Error parsing config file", e);
			Globals.exit();
		} catch (NullPointerException e)
		{
			U.e("Internal error parsing config file.");
			e.printStackTrace();
			Globals.exit();
		}
	}

	private void parseRoles(JSONObject data)
	{
		JSONObject rolesData = data.getJSONObject("roles");
		HashMap<String, Role> roles = this.getMap("roles");

		for (String cur : rolesData.keySet())
			roles.put(cur, new Role(cur, rolesData.getJSONObject(cur)));
	}

	public HashMap<String, Item> getItems()
	{
		return this.getMap("name");
	}

	private void parseItems(JSONObject data)
	{
		JSONObject itemData = data.getJSONObject("items");
		HashMap<String, Item> items = this.getMap("items");

		for (String cur : itemData.keySet())
			items.put(cur, new Item(cur, itemData.getJSONObject(cur)));
	}

	private void parseStatuses(JSONObject data)
	{
		JSONObject statusesData = data.getJSONObject("statuses");
		HashMap<String, Status> statuses = this.getMap("statuses");

		for (String cur : statusesData.keySet())
			statuses.put(cur, new Status(cur, statusesData.getJSONObject(cur)));
	}

	@SuppressWarnings("unchecked")
	private <T> HashMap<String, T> getMap(String name)
	{
		if (!this.maps.containsKey(name))
			this.maps.put(name, new HashMap<String, T>());
		return (HashMap<String, T>) this.maps.get(name);
	}

	public String toString()
	{
		return this.maps.toString();
	}
}
