package config.core;

import global.Globals;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import backend.U;
import backend.lib.json.JSONArray;
import backend.lib.json.JSONException;
import backend.lib.json.JSONObject;
import config.explorer.Explorer;
import config.explorer.ExportedParam.MType;
import config.explorer.ExportedParameter;

/**
 * Testbed config, uses the JSON parser to parse different parts of a given
 * config file. Maintains ordering of original config file, as well as adding
 * config sections during writing out that were not in the original config file.
 * Assumes that JSONExportable classes properly export themselves, this also
 * allows for full, pretty-printed config writing.
 * <p>
 * Future: Currently has a simple example for items, in the future, order of
 * loading might matter, for example: Actions will might need to be loaded
 * before Items, if Items refer to Actions, etc, etc. Bi-directional links would
 * be nice to avoid architecturely, but wouldn't be a show-stopper.
 */
public class Config
{
	private static MType[]															stdParamFilter;
	static
	{
		Config.stdParamFilter = new MType[]
		{ MType.GETTER, MType.SETTER };
	}

	private LinkedHashMap<String, LinkedHashMap<String, ? extends ConfigMember>>	maps;

	/**
	 * Attempts to load a config from the file passed.
	 *
	 * @param filename
	 *            the file to load a config from
	 */
	public Config(String filename)
	{
		this.loadConfig(filename);
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
		// TODO: Don't expose internal data members.
		return this.maps;
	}

	/**
	 * Wrapper, simply returns an empty/default JSONArray instead of null
	 *
	 * @param curJSONSection
	 *            the original JSONObject to extract from
	 * @param curKey
	 *            the key to extract from
	 * @return the resulting JSONArray
	 */
	private JSONArray getJSONArr(JSONObject curJSONSection, String curKey)
	{
		JSONArray val;
		val = curJSONSection.optJSONArray(curKey);
		if (val == null)
			val = new JSONArray();
		return val;
	}

	/**
	 * Wrapper, simply returns an empty/default JSONObject instead of null
	 *
	 * @param curJSONSection
	 *            the original JSONObject to extract from
	 * @param curKey
	 *            the key to extract from
	 * @return the resulting JSONObject
	 */
	private JSONObject getJSONObj(JSONObject curJSONSection, String curKey)
	{
		JSONObject obj;
		obj = curJSONSection.optJSONObject(curKey);
		if (obj == null)
			obj = new JSONObject();
		return obj;
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
	private <T extends ConfigMember> LinkedHashMap<String, T> getMap(String name)
	{
		if (!this.maps.containsKey(name))
			this.maps.put(name, new LinkedHashMap<String, T>());
		return (LinkedHashMap<String, T>) this.maps.get(name);
	}

	/**
	 * Returns a hashmap of the implied type for the specified section.
	 *
	 * @param key
	 *            the key for the config section required
	 * @return a hashmap of strings to the implied type.
	 */
	public <T extends ConfigMember> LinkedHashMap<String, T> getSection(String key)
	{
		return this.getMap(key);
	}

	/**
	 * Intelligently generates a JSONRepresentation of the specified object
	 * based on pre-specified annotations.
	 *
	 * @param value
	 *            the input object to create a representation of
	 * @return the resulting JSON representation
	 */
	private JSONObject intelliGen(ConfigMember value)
	{
		JSONObject res = new JSONObject();
		Map<String, ExportedParameter> paramMap = Explorer.findParametersByFilter(value, Config.stdParamFilter);
		for (Entry<String, ExportedParameter> curExport : paramMap.entrySet())
			res.put(curExport.getKey(), curExport.getValue().call(MType.GETTER));
		return res;
	}

	/**
	 * Intelligently parses the specified data from the json object and the
	 * specified type. Uses the standard annotations (ExportedParam) to
	 * correctly parse the methods as needed
	 *
	 * @param data
	 *            the JSON Data to attempt to parse
	 * @param key
	 *            the key for this section
	 * @param type
	 *            the type to attempt to use for this part of the config file
	 */
	private <T extends ConfigMember> void intelliParse(JSONObject data, String key, Class<T> type)
	{
		JSONObject jsonData = data.optJSONObject(key);
		if (jsonData == null)
			jsonData = new JSONObject();
		HashMap<String, T> parsed = this.getMap(key);
		for (String cur : jsonData.keySet())
			try
			{
				// Get the current JSON object, instantiate it to the given type
				// and populate the class.
				JSONObject curJSONSection = jsonData.optJSONObject(cur);
				if (curJSONSection == null)
					curJSONSection = new JSONObject();
				T curInstance = type.newInstance();
				Map<String, ExportedParameter> paramMap = Explorer.findParametersByFilter(curInstance, Config.stdParamFilter);
				for (String curKey : curJSONSection.keySet())
					if (paramMap.containsKey(curKey))
						this.parseParam(curJSONSection, curKey, paramMap.get(curKey));
					else
						U.d("Dropped extra key found in JSON structure: " + curKey + ".", 1);
				parsed.put(cur, curInstance);
			} catch (InstantiationException e)
			{
				U.e("Error instantiating class " + type.getName() + ". Make sure you are using the correct type for the key '" + key + "' in the Config class.", e);
			} catch (IllegalAccessException | JSONException e)
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
	 */
	private void loadConfig(String filename)
	{
		try
		{
			JSONObject data = new JSONObject(U.readFile(filename));
			this.maps = new LinkedHashMap<String, LinkedHashMap<String, ? extends ConfigMember>>();
			for (String curJSONKey : data.keySet())
				try
				{
					Class<? extends ConfigMember> type = U.cleanCast(Class.forName("config." + curJSONKey));
					this.intelliParse(data, curJSONKey, type);
				} catch (ClassCastException | ClassNotFoundException ex)
				{
					U.d("Extra key found in JSON file: " + curJSONKey + ". Did you spell the name correctly?", 1);
				}
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
	 * Writes to file a JSON equivalent of this loaded config.
	 *
	 * @param filename
	 *            the file to export to
	 */
	public void outputToFile(String filename)
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

	/**
	 * From the given JSONSection, and the specified key, parses the data into
	 * the passed exported parameter.
	 *
	 * @param curJSONSection
	 *            the json section to parse from
	 * @param curKey
	 *            the key to pull the data from
	 * @param curParam
	 *            the parameter to attempt to parse
	 */
	private void parseParam(JSONObject curJSONSection, String curKey, ExportedParameter curParam)
	{
		JSONObject obj;
		JSONArray val;
		switch (curParam.getDatatype())
		{
			case NUM:
				curParam.call(MType.SETTER, curJSONSection.optDouble(curKey, 0.0));
				break;
			case STR:
				curParam.call(MType.SETTER, curJSONSection.optString(curKey, ""));
				break;
			case NUMLIST:
				val = this.getJSONArr(curJSONSection, curKey);
				List<Double> numList = new LinkedList<Double>();
				for (int i = 0; i < val.length(); i++)
					numList.add(val.optDouble(i, 0));
				curParam.call(MType.SETTER, numList);
				break;
			case STRLIST:
				val = this.getJSONArr(curJSONSection, curKey);
				List<String> strList = new LinkedList<String>();
				for (int i = 0; i < val.length(); i++)
					strList.add(val.optString(i, ""));
				curParam.call(MType.SETTER, strList);
				break;
			case NUMMAP:
				obj = this.getJSONObj(curJSONSection, curKey);
				Map<String, Double> numMap = new LinkedHashMap<String, Double>();
				for (String mapKey : obj.keySet())
					numMap.put(mapKey, obj.getDouble(mapKey));
				curParam.call(MType.SETTER, numMap);
				break;
			case STRMAP:
				obj = this.getJSONObj(curJSONSection, curKey);
				Map<String, String> strMap = new LinkedHashMap<String, String>();
				for (String mapKey : obj.keySet())
					strMap.put(mapKey, obj.getString(mapKey));
				curParam.call(MType.SETTER, strMap);
				break;
			case OBJ:
				obj = this.getJSONObj(curJSONSection, curKey);
			default:
				U.d("Unknown exported parameter found.", 2);
				break;
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
}
