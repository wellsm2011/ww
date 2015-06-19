package config.core;

import global.Globals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import backend.U;
import backend.json.JSONException;
import backend.json.JSONObject;
import config.Item;
import config.Role;
import config.Status;
import config.explorer.Explorer;
import config.explorer.ExportedParam.MType;
import config.explorer.ExportedParameter;

/**
 * <p>
 * Testbed config, uses the JSON parser to parse different parts of a given
 * config file. Maintains ordering of original config file, as well as adding
 * config sections during writing out that were not in the original config file.
 * Assumes that JSONExportable classes properly export themselves, this also
 * allows for full, pretty-printed config writing.
 * </p>
 *
 * <p>
 * Future: Currently has a simple example for items, in the future, order of
 * loading will probably matter, for example: Actions will probably need to be
 * loaded before Items, if Items refer to Actions, etc, etc. Bi-directional
 * links would be nice to avoid architecturely, but wouldn't be a show-stopper.
 * </p>
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
 * and write the parsing class. Done!
 * </p>
 *
 *
 */
public class Config
{
	private LinkedHashMap<String, LinkedHashMap<String, ? extends ConfigMember>>	maps;

	/**
	 * Attempts to load a config from the file passed.
	 *
	 * @param filename
	 *            the file to load a config from
	 */
	public Config(String filename)
	{
		LinkedHashMap<String, Class<? extends ConfigMember>> configMembers = new LinkedHashMap<String, Class<? extends ConfigMember>>();

		// Note: If it shows an error, make sure that your <something>.class
		// implements JSONExportable.

		/*
		 * Reflections reflections = new Reflections("my.project.prefix");
		 *
		 * Set<Class<? extends Object>> allClasses =
		 * reflections.getSubTypesOf(Object.class);
		 */

		configMembers.put("items", Item.class);
		configMembers.put("statuses", Status.class);
		configMembers.put("roles", Role.class);
		configMembers.put("actions", Action.class);
		configMembers.put("abilities", Ability.class);
		configMembers.put("gameCons", GameCon.class);
		configMembers.put("actionMods", ActionModifier.class);
		configMembers.put("rounds", Rounds.class);

		// Add new config sections here, order here merely changes the default
		// export ordering.

		this.loadConfig(filename, configMembers);
	}

	/**
	 * Access all config members stored. <b>
	 * <p>
	 * Note: this is the base map, so all changes are backed against the primary
	 * config object.</b>
	 * </p>
	 *
	 * @return
	 */
	public LinkedHashMap<String, LinkedHashMap<String, ? extends ConfigMember>> getAllMaps()
	{
		return this.maps;
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
	private <T extends ConfigMember> HashMap<String, T> getMap(String name)
	{
		if (!this.maps.containsKey(name))
			this.maps.put(name, new LinkedHashMap<String, T>());
		return (HashMap<String, T>) this.maps.get(name);
	}

	/**
	 * Returns a hashmap of the implied type for the specified section.
	 *
	 * @param key
	 *            the key for the config section required
	 * @return a hashmap of strings to the implied type.
	 */
	public <T extends ConfigMember> HashMap<String, T> getSection(String key)
	{
		return this.getMap(key);
	}

	private JSONObject intelliGen(ConfigMember value)
	{
		// curMemberItem.getValue().exportAsJSON()
		return new JSONObject();
	}

	private <T extends ConfigMember> void intelliParse(JSONObject data, String key, Class<T> type)
	{
		JSONObject jsonData = data.optJSONObject(key);
		if (jsonData == null)
			jsonData = new JSONObject();

		for (String cur : jsonData.keySet())
			try
			{
				JSONObject curJSONSection = jsonData.optJSONObject(cur);
				if (curJSONSection == null)
					curJSONSection = new JSONObject();
				T curInstance = type.newInstance();
				Map<String, ExportedParameter> map = Explorer.findParametersByFilter(curInstance, MType.GETTER, MType.SETTER);
				for (String curKey : curJSONSection.keySet())
					if (map.containsKey(curKey))
						switch (map.get(curKey).getDatatype())
						{
							case NUMBER:
								map.get(curKey).call(MType.SETTER, curJSONSection.getDouble(key));
								break;
							case NUMCOLLECTION:
								break;
							case NUMMAP:
								break;
							case STRCOLLECTION:
								break;
							case STRING:
								map.get(curKey).call(MType.SETTER, curJSONSection.getString(key));
								break;
							case STRMAP:
								break;
							default:
								break;
						}

			} catch (SecurityException e)
			{
				U.e("Error finding proper constructor for class " + type.getName() + " for config section " + key + ". Make sure " + type.getName()
						+ " actually has a constructor compatible with the standard. (As in, it should take a String for the name, and a JSONObject for the ");
			} catch (InstantiationException e)
			{
				U.e("Error instantiating class " + type.getName() + " for what reason did you try and use an abstract class or interface or something?"
						+ " Make sure you are using the correct type for key " + key + " in the Config class.", e);
			} catch (IllegalAccessException | IllegalArgumentException | JSONException e)
			{
				U.e("Issue parsing " + key + " during config loading. Probably an internal error with the \"" + key + "\" handler.");
				e.printStackTrace();
			}
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
	private void loadConfig(String filename, HashMap<String, Class<? extends ConfigMember>> configMembers)
	{
		try
		{
			JSONObject data = new JSONObject(U.readFile(filename));
			this.maps = new LinkedHashMap<String, LinkedHashMap<String, ? extends ConfigMember>>();
			for (String curJSONKey : data.keySet())
			{
				Class<? extends ConfigMember> type = configMembers.get(curJSONKey);
				if (type != null)
					this.parse(data, curJSONKey, type);
				else
					U.e("Unknown key " + curJSONKey + " in config file. Might want to look at that.");
			}
			for (Entry<String, Class<? extends ConfigMember>> configMembs : configMembers.entrySet())
				if (!this.maps.containsKey(configMembs.getKey()))
					this.intelliParse(data, configMembs.getKey(), configMembs.getValue());

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
	private <T extends ConfigMember> void parse(JSONObject data, String key, Class<T> type)
	{
		JSONObject jsonData = data.optJSONObject(key);
		if (jsonData == null)
			jsonData = new JSONObject();
		HashMap<String, T> parsed = this.getMap(key);

		for (String cur : jsonData.keySet())
			try
			{
				Constructor<T> constructor = type.getConstructor(String.class, JSONObject.class);
				JSONObject curJSONSection = jsonData.optJSONObject(cur);
				if (curJSONSection == null)
					curJSONSection = new JSONObject();
				parsed.put(cur, constructor.newInstance(cur, curJSONSection));

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
	 * Basic toString method, simply returns the string representation
	 */
	@Override
	public String toString()
	{
		return this.maps.toString();
	}

	/**
	 * Writes to file a JSON equivalent of this loaded config.
	 *
	 * @param filename
	 *            the file to export to
	 */
	public void writeToFile(String filename)
	{
		JSONObject output = new JSONObject();
		for (Entry<String, LinkedHashMap<String, ? extends ConfigMember>> curConfigMember : this.maps.entrySet())
		{
			JSONObject member = new JSONObject();
			for (Entry<String, ? extends ConfigMember> curMemberItem : curConfigMember.getValue().entrySet())
				member.putOpt(curMemberItem.getKey(), this.intelliGen(curMemberItem.getValue()));
			output.putOnce(curConfigMember.getKey(), member);
		}
		U.writeToFile(filename, output.toString(4));
	}
}
