package config;

import global.Globals;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map.Entry;

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
		HashMap<String, Class<?>> configMembers = new HashMap<String, Class<?>>();

		configMembers.put("items", Item.class);
		configMembers.put("statuses", Status.class);
		configMembers.put("roles", Role.class);

		loadConfig(filename, configMembers);
	}

	private void loadConfig(String filename, HashMap<String, Class<?>> configMembers)
	{
		try
		{
			JSONObject data = new JSONObject(U.readFile(filename));
			this.maps = new HashMap<String, HashMap<String, ?>>();
			for (Entry<String, Class<?>> cur : configMembers.entrySet())
				this.parse(data, cur.getKey(), cur.getValue());

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

	@SuppressWarnings("unchecked")
	private <T> void parse(JSONObject data, String key, Class<T> classs)
	{
		JSONObject jsonData = data.getJSONObject(key);
		HashMap<String, T> parsed = this.getMap(key);

		for (String cur : jsonData.keySet())
			try
			{
				parsed.put(cur, (T) classs.getConstructors()[0].newInstance(cur, jsonData.getJSONObject(cur)));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | JSONException e)
			{
				U.e("Issue parseing " + key + " during config loading.");
				e.printStackTrace();
			}
	}

	public <T> HashMap<String, T> getSection(String key)
	{
		return this.getMap(key);
	}

	@SuppressWarnings("unchecked")
	private <T> HashMap<String, T> getMap(String name)
	{
		if (!this.maps.containsKey(name))
			this.maps.put(name, new HashMap<String, T>());
		return (HashMap<String, T>) this.maps.get(name);
	}

	@Override
	public String toString()
	{
		return this.maps.toString();
	}
}
