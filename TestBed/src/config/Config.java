package config;

import global.Globals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	private LinkedHashMap<String, LinkedHashMap<String, ? extends JSONExportable>>	maps;

	public Config(String filename)
	{
		LinkedHashMap<String, Class<? extends JSONExportable>> configMembers = new LinkedHashMap<String, Class<? extends JSONExportable>>();

		// Note: If it shows an error, make sure that your <something>.class
		// implements JSONExportable.
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
	private void loadConfig(String filename, HashMap<String, Class<? extends JSONExportable>> configMembers)
	{
		try
		{
			JSONObject data = new JSONObject(U.readFile(filename));
			this.maps = new LinkedHashMap<String, LinkedHashMap<String, ? extends JSONExportable>>();
			for (Entry<String, Class<? extends JSONExportable>> cur : configMembers.entrySet())
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
	private <T extends JSONExportable> void parse(JSONObject data, String key, Class<T> type)
	{
		JSONObject jsonData = data.getJSONObject(key);
		HashMap<String, T> parsed = this.getMap(key);

		for (String cur : jsonData.keySet())
			try
			{
				Constructor<T> constructor = type.getConstructor(String.class, JSONObject.class);
				JSONObject curJSONSection = jsonData.optJSONObject(cur);
				if (curJSONSection == null)
					curJSONSection = new JSONObject();
				parsed.put(cur, (T) constructor.newInstance(cur, curJSONSection));

			} catch (NoSuchMethodException | SecurityException e)
			{
				U.e("Error finding proper constructor for class " + type.getName() + " for config section " + key + ". Make sure " + type.getName()
						+ " actually has a constructor compatible with the standard. (As in, it should take a String for the name, and a JSONObject for the ");
			} catch (InstantiationException e)
			{
				U.e("Error instantiating class " + type.getName() + " for what reason did you try and use an abstract class or interface or something?"
						+ " Make sure you are using the correct type for key " + key + " in the Config class.", e);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | JSONException e)

			{
				U.e("Issue parsing " + key + " during config loading. Probably an internal error with the \"" + key + "\" handler.");
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
	public <T extends JSONExportable> HashMap<String, T> getSection(String key)
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
	private <T extends JSONExportable> HashMap<String, T> getMap(String name)
	{
		if (!this.maps.containsKey(name))
			this.maps.put(name, new LinkedHashMap<String, T>());
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

	public void writeToFile(String filename)
	{
		JSONObject output = new JSONObject();
		for (Entry<String, LinkedHashMap<String, ? extends JSONExportable>> curConfigMember : this.maps.entrySet())
		{
			JSONObject member = new JSONObject();
			for (Entry<String, ? extends JSONExportable> curMemberItem : curConfigMember.getValue().entrySet())
				member.putOpt(curMemberItem.getKey(), curMemberItem.getValue().exportAsJSON());
			output.putOnce(curConfigMember.getKey(), member);
		}
		U.writeToFile(filename, output.toString(4));
	}
}
