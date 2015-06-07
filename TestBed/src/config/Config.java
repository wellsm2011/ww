package config;

import global.Globals;

import java.lang.reflect.Constructor;
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
 * <p>
 * Procedure to add a new config file section: Add line in Constructor line
 * like...
 * </p>
 * 
 * <p>
 * <code>configMembers.put(<json key here>, <parseing class>.class);</code>
 * </p>
 * 
 * <p>
 * and write the parseing class. Done!
 * </p>
 * 
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

	/**
	 * Based on a given filename, as well as a set of config members, parses
	 * them into their own classes.
	 * 
	 * @param filename
	 *            the filename to open
	 * @param configMembers
	 *            the config categories to parse, maps strings to class
	 *            definitions
	 */
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

	/**
	 * Based on a JSON config file, pulls the given key, and instantiates
	 * objects into the passed class.
	 * 
	 * @param data
	 *            the json data to load from
	 * @param key
	 *            the json key to pull things from
	 * @param type
	 *            the class type to instantiate.
	 */
	private <T> void parse(JSONObject data, String key, Class<T> type)
	{
		JSONObject jsonData = data.getJSONObject(key);
		HashMap<String, T> parsed = this.getMap(key);

		for (String cur : jsonData.keySet())
			try
			{
				Constructor<T> constructor = type.getConstructor(String.class, JSONObject.class);
				parsed.put(cur, (T) constructor.newInstance(cur, jsonData.getJSONObject(cur)));

			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | JSONException | NoSuchMethodException e)
			{
				U.e("Issue parseing " + key + " during config loading. Probably an internal error with the \"" + key + "\" handler.");
				e.printStackTrace();
			}
	}

	/**
	 * Returns a hashmap of the implied type for the specified section.
	 * 
	 * @param key
	 *            the key for the config section required
	 * @return a hashmap of strings to the implied type.
	 */
	public <T> HashMap<String, T> getSection(String key)
	{
		return this.getMap(key);
	}

	/**
	 * Internal map getter, for a given name either returns, or makes and
	 * returns a hashmap that corresponds to the given key.
	 * 
	 * @param name
	 *            the key of the section
	 * @return a hashmap from strings to the implied type
	 */
	@SuppressWarnings("unchecked")
	private <T> HashMap<String, T> getMap(String name)
	{
		if (!this.maps.containsKey(name))
			this.maps.put(name, new HashMap<String, T>());
		return (HashMap<String, T>) this.maps.get(name);
	}

	/**
	 * Basic toString method, simply returns the string representation
	 */
	@Override
	public String toString()
	{
		return this.maps.toString();
	}
}
