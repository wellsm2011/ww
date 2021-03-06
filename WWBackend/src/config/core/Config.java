package config.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.impetus.annovention.ClasspathDiscoverer;
import com.impetus.annovention.Discoverer;

//Static import of U not used due to cleanup and proper styling, as well as basic readability.
import backend.U;
import backend.functionInterfaces.ValDecoder;
import backend.functionInterfaces.ValEncoder;
import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.HasCustomConfigType;
import config.core.exceptions.UnknownDecoderException;
import config.core.exceptions.UnknownReferenceException;
import global.Globals;

/**
 * <p>
 * Testbed config, uses the JSON parser to parse different parts of a given
 * config file. Maintains ordering of original config file, as well as adding
 * config sections during writing out that were not in the original config file.
 * </p>
 * <p>
 * Rest under development.
 * </p>
 * <p>
 * Actually uses reflection to scan and look over the entire classpath and find
 * things with the @ConfigMember annotation.
 * </p>
 * 
 * @see ConfigMember
 * @see ExportedParam
 * @see ExportedParameter
 * @author Andrew Binns
 */
public class Config
{
	private static Map<String, ValDecoder<?>>	decoders;
	private static Map<String, ValEncoder>		encoders;

	static
	{
		Config.decoders = new HashMap<>();
		Config.encoders = new HashMap<>();
	}

	/**
	 * Looks through all classes, and finds those with the ConfigMember
	 * annotation. Note, this actually via the tricks of javaassist bytecode
	 * scanning, the sun JDI useful stuff, and the annoventions magic manages to
	 * avoid actually loading all classes in the classpath during scanning. At
	 * least, it should.
	 *
	 * @return a map of classnames to class objects that are annotated with the
	 *         ConfigMember annotation
	 */
	public static Map<String, Class<?>> findConfigMembers()
	{
		Map<String, Class<?>> res = new LinkedHashMap<>();
		Discoverer discoverer = new ClasspathDiscoverer();
		// Find all our config members
		discoverer.addAnnotationListener(new FinderListener((in) -> {
			try
			{
				res.put(Class.forName(in).getAnnotation(ConfigMember.class).sectionKey(), Class.forName(in));
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} , ConfigMember.class));
		// Also pick up on items that mention having a custom config decoder.
		// Designed to merely make sure that their static constructor is called,
		// so they can register themselves for our use later
		discoverer.addAnnotationListener(new FinderListener((in) -> {
			try
			{
				Class.forName(in);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} , HasCustomConfigType.class));
		discoverer.discover();
		return res;
	}

	/**
	 * Allows for registration of custom encoder/decoder pairs with a type, this
	 * allows for custom types to be nicely imported and exported. Eventually
	 * this may also require a GUI pane be offered so that the editor can
	 * properly handle the custom type.
	 * 
	 * @param name
	 *            the name of the type to register
	 * @param decoder
	 *            the decoder function, goes from a JSONObject to whatever the
	 *            expected type is.
	 * @param encoder
	 *            the encoder function, goes from some object to a JSONObject
	 *            for exporting.
	 * @return true if the type was successfully registered, false if there is a
	 *         already registered encoder or decoder.
	 */
	public static boolean registerType(String name, ValDecoder<?> decoder, ValEncoder encoder)
	{
		if (Config.decoders.containsKey(name))
			return false;
		if (Config.encoders.containsKey(name))
			return false;
		Config.decoders.put(name.toLowerCase(), decoder);
		Config.encoders.put(name.toLowerCase(), encoder);
		return true;
	}

	private LinkedHashMap<String, SectionManager>	maps;
	private Map<Class<?>, SectionManager>			classToSectionMap;

	/**
	 * Attempts to load a config from the file passed.
	 *
	 * @param filename
	 *            the file to load a config from
	 */
	public Config(String filename)
	{

		this.maps = new LinkedHashMap<>();
		this.classToSectionMap = new LinkedHashMap<>();
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
	public LinkedHashMap<String, SectionManager> getAllMaps()
	{
		// TODO: Don't expose internal data members.
		// TODONT: mistake internal data members for general datastructure, stop
		// trying to make bad code by mass memory copy.
		return this.maps;
	}

	/**
	 * For a given exported parameter, attempts to get the decoder it refers to.
	 * 
	 * @param curParam
	 *            the parameter to search for
	 * @return the ValDecoder for that type, or null if not found.
	 */
	private ValDecoder<?> getDecoder(ExportedParameter curParam)
	{
		String name = curParam.getDataType().substring(curParam.getDataType().indexOf(':') + 1);
		return Config.decoders.get(name.toLowerCase());
	}

	/**
	 * For a given exported parameter, attempts to get the encoder it refers to.
	 * 
	 * @param curParam
	 *            the parameter to search for
	 * @return the ValEncoder for that type, or null if not found.
	 */
	private ValEncoder getEncoder(ExportedParameter curParam)
	{
		String name = curParam.getDataType().substring(curParam.getDataType().indexOf(':') + 1);
		return Config.encoders.get(name.toLowerCase());
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
	 * @return a section manager for the given type
	 */
	private SectionManager getManager(String name, Class<?> type)
	{
		if (!(type == null))
			if (!this.maps.containsKey(name))
			{
				SectionManager secMan = new SectionManager(type);
				this.maps.put(name, secMan);
				this.classToSectionMap.put(type, secMan);
			}
		return this.maps.get(name);
	}

	/**
	 * Returns a hashmap of the implied type for the specified section.
	 *
	 * @param key
	 *            the key for the config section required
	 * @return the section manager for the given key. Null if not found.
	 */
	public SectionManager getSection(String key)
	{
		return this.getManager(key, null);
	}

	/**
	 * Given a class, finds the section manager that corresponds to the given
	 * type. Returns null if not found.
	 * 
	 * @param type
	 * @return
	 */
	public SectionManager getSectionByClass(Class<?> type)
	{
		return this.classToSectionMap.get(type);
	}

	/**
	 * Handles decoding the given json section into the given instance using a
	 * custom decoder.
	 * 
	 * @param curJSONSection
	 *            the json section to pull from to get a section to decode
	 * @param curKey
	 *            the key for the current section of json to decode
	 * @param curParam
	 *            the data for the current parameter
	 * @param instance
	 *            the object to load the parameters into
	 * @throws UnknownDecoderException
	 *             if the decoder for the specified type has not been declared.
	 */
	private void handleCustomDecode(JSONObject curJSONSection, String curKey, ExportedParameter curParam, Object instance) throws UnknownDecoderException
	{
		JSONObject obj;

		ValDecoder<?> decoder = this.getDecoder(curParam);
		if (decoder == null)
			throw new UnknownDecoderException("No registered decoder for " + curParam.getDataType());
		switch (curParam.getStoreType())
		{
			case SINGLE:
				obj = curJSONSection.optJSONObject(curKey);
				if (obj == null)
					obj = new JSONObject();
				curParam.set(instance, decoder.decode(obj));
				break;
			case LIST:
				throw new IllegalArgumentException("Custom decoders cannot parse list structures." + curJSONSection.toString());
			case MAP:
				obj = this.getJSONObj(curJSONSection, curKey);
				Map<String, Object> map = new LinkedHashMap<>();
				for (String mapKey : obj.keySet())
				{
					JSONObject curItem = obj.optJSONObject(mapKey);

					if (curItem != null)
						map.put(mapKey, decoder.decode(curItem));
				}
				curParam.set(instance, map);
				break;
			default:
				U.d("Unknown exported parameter found.", 2);
				break;
		}
	}

	/**
	 * Parses the given json section, as an enum-like type system. Currently
	 * unimplemented due to lack of need.
	 * 
	 * @param curJSONSection
	 * @param curKey
	 * @param curParam
	 * @param instance
	 */
	private void handleEnum(JSONObject curJSONSection, String curKey, ExportedParameter curParam, Object instance)
	{
		// TODO iff ever needed, handle enum types
	}

	/**
	 * Handles the given section as primitives
	 * 
	 * @param curJSONSection
	 * @param curKey
	 * @param curParam
	 * @param instance
	 */
	private void handlePrimitive(JSONObject curJSONSection, String curKey, ExportedParameter curParam, Object instance)
	{
		String str;
		Double dbl;
		switch (curParam.getStoreType())
		{
			case SINGLE:
				Object elem = curJSONSection.get(curKey);
				if (elem instanceof Number)
					curParam.set(instance, curJSONSection.getDouble(curKey));
				else
					curParam.set(instance, curJSONSection.getString(curKey));
				break;
			case LIST:
				JSONArray arr = this.getJSONArr(curJSONSection, curKey);
				// Using arraylist because overall we should be having fairly
				// static lengths
				try
				{
					List<String> list = new ArrayList<>(arr.length());
					for (int i = 0; i < arr.length(); i++)
						list.add(arr.getString(i));
					curParam.set(instance, list);
				} catch (JSONException e)
				{
					List<Double> list = new ArrayList<>(arr.length());
					for (int i = 0; i < arr.length(); i++)
						list.add(arr.getDouble(i));
					curParam.set(instance, list);
				}
				break;
			case MAP:
				JSONObject obj = this.getJSONObj(curJSONSection, curKey);
				try
				{
					Map<String, String> map = new LinkedHashMap<>();
					for (String mapKey : obj.keySet())
					{
						str = obj.getString(mapKey);
						map.put(mapKey, str);
					}
					curParam.set(instance, map);
				} catch (JSONException e)
				{
					Map<String, Double> map = new LinkedHashMap<>();
					for (String mapKey : obj.keySet())
					{
						dbl = obj.getDouble(mapKey);
						map.put(mapKey, dbl);
					}
					curParam.set(instance, map);
				}
				break;
			default:
				U.d("Unknown exported parameter found.", 2);
				break;
		}
	}

	/**
	 * Handles the given section as references to other config sections.
	 * 
	 * @param curJSONSection
	 * @param curKey
	 * @param curParam
	 * @param instance
	 * @throws UnknownReferenceException
	 */
	private void handleReferences(JSONObject curJSONSection, String curKey, ExportedParameter curParam, Object instance) throws UnknownReferenceException
	{
		String refName;
		SectionManager secMan = this.getSection(curParam.getDataType().substring(curParam.getDataType().indexOf(':') + 1));
		if (secMan == null)
			throw new UnknownReferenceException("Could not find section manager for declared reference type " + curParam.getDataType());
		switch (curParam.getStoreType())
		{
			case SINGLE:
				refName = curJSONSection.getString(curKey);
				curParam.set(instance, secMan.getElem(refName));
				break;
			case LIST:
				JSONArray arr = this.getJSONArr(curJSONSection, curKey);
				// Using arraylist because overall we should be having fairly
				// static lengths
				List<Object> list = new ArrayList<>(arr.length());
				for (int i = 0; i < arr.length(); i++)
				{
					refName = arr.getString(i);
					list.add(secMan.getElem(refName));
				}
				curParam.set(instance, list);
				break;
			case MAP:
				JSONObject obj = this.getJSONObj(curJSONSection, curKey);
				Map<String, Object> map = new LinkedHashMap<>();
				for (String mapKey : obj.keySet())
				{
					refName = obj.getString(mapKey);
					map.put(mapKey, secMan.getElem(refName));
				}
				curParam.set(instance, map);
				break;
			default:
				U.d("Unknown exported parameter found.", 2);
				break;
		}
	}

	/**
	 * Loops over all the elements in a given json section, instantiates each.
	 * This is done as a seperate step to allow for references to nicely be
	 * handled at the next pass.
	 * 
	 * @param curSectionKey
	 *            key of the current section, used to create the section
	 *            manager.
	 * @param jsonData
	 *            the json data to loop over
	 * @param type
	 *            the internal type that this section refers to
	 */
	private void instantiateSectionElems(String curSectionKey, JSONObject jsonData, Class<?> type)
	{
		if (type == null)
			throw new ClassCastException();
		SectionManager secMan = this.getManager(curSectionKey, type);
		// Loop over all elements in this section, instantiating each,
		// and putting in the relevant section manager
		for (String curElemKey : jsonData.keySet())
			try
			{
				Object curInstance = type.newInstance();
				secMan.offer(curElemKey, curInstance);

			} catch (InstantiationException | IllegalAccessException e)
			{
				U.e("Error instantiating class " + type.getName() + ". Probably you hid the blank constructor, or something equally odd. "
						+ "Like you specifiying an abstract class or interface as a config member, instead of a instantiable class...", e);
			}
	}

	/**
	 * Intelligently generates a JSONRepresentation of the specified object
	 * based on pre-specified annotations.
	 *
	 * @param object
	 *            the input object to create a representation of
	 * @return the resulting JSON representation
	 * @throws UnknownDecoderException
	 */
	private JSONObject intelliGen(Object input, SectionManager secMan) throws UnknownDecoderException
	{
		JSONObject res = new JSONObject();
		Map<String, ExportedParameter> paramMap = secMan.getParamMappings();
		for (Entry<String, ExportedParameter> curExport : paramMap.entrySet())
		{
			ExportedParameter curParam = curExport.getValue();
			if (curParam.getDataType().startsWith("ref:"))
			{
				SectionManager targSecMan = this.getSection(curParam.getDataType().substring(curParam.getDataType().indexOf(':') + 1));
				res.put(curExport.getKey(), targSecMan.getKeyFor(curParam.get(input)));
				switch (curParam.getStoreType())
				{
					case LIST:
						List<?> listToExport = curParam.get(input);
						List<String> encodedList = new ArrayList<>(listToExport.size());
						listToExport.forEach(cur -> encodedList.add(targSecMan.getKeyFor(cur)));
						res.put(curExport.getKey(), encodedList);
						break;
					case MAP:
						Map<String, ?> mapToExport = curParam.get(input);
						Map<String, String> encodedMap = new LinkedHashMap<>();
						mapToExport.forEach((key, val) -> encodedMap.put(key, targSecMan.getKeyFor(val)));
						res.put(curExport.getKey(), encodedMap);
						break;
					case SINGLE:
						res.put(curExport.getKey(), targSecMan.getKeyFor(curParam.get(input)));
						break;
				}
			} else if (curParam.getDataType().startsWith("decode:"))
			{
				ValEncoder encoder = this.getEncoder(curParam);
				if (encoder == null)
					throw new UnknownDecoderException(curParam.getDataType());
				switch (curParam.getStoreType())
				{
					case LIST:
						List<?> listToExport = curParam.get(input);
						List<JSONObject> encodedList = new ArrayList<>(listToExport.size());
						listToExport.forEach(cur -> encodedList.add(encoder.encode(cur)));
						res.put(curExport.getKey(), encodedList);
						break;
					case MAP:
						Map<String, ?> mapToExport = curParam.get(input);
						Map<String, JSONObject> encodedMap = new LinkedHashMap<>();
						mapToExport.forEach((key, val) -> encodedMap.put(key, encoder.encode(val)));
						res.put(curExport.getKey(), encodedMap);
						break;
					case SINGLE:
						res.put(curExport.getKey(), encoder.encode(curParam.get(input)));
						break;

				}
			} else if (curParam.getDataType().startsWith("enum:"))
				// TODO iff ever needed, handle enum types.
				;
			else
				res.putObj(curExport.getKey(), curParam.get(input));
		}
		return res;
	}

	/**
	 * Intelligently parses the specified data from the json object and the
	 * specified type. Uses the standard annotations (ExportedParam) to
	 * correctly parse the methods as needed
	 *
	 * @param data
	 *            the JSON Data to attempt to parse
	 * @param sectionKey
	 *            the key for this section
	 * @param type
	 *            the type to attempt to use for this part of the config file
	 * @throws UnknownReferenceException
	 * @throws UnknownDecoderException
	 */
	private void intelliParse(JSONObject data, String sectionKey, SectionManager secMan) throws UnknownReferenceException, UnknownDecoderException
	{
		JSONObject jsonData = data.optJSONObject(sectionKey);
		if (jsonData == null)
			jsonData = new JSONObject();
		// parse all the param data
		for (String elemName : jsonData.keySet())
			try
			{
				// Get the current JSON object, instantiate it to the given type
				// and populate the class.
				Object curInstance = secMan.getElem(elemName);
				JSONObject curJSONSection = jsonData.optJSONObject(elemName);
				if (curJSONSection == null)
					curJSONSection = new JSONObject();
				Map<String, ExportedParameter> paramMap = secMan.getParamMappings();
				for (String paramKey : curJSONSection.keySet())
					if (paramMap.containsKey(paramKey))
						this.parseParam(curJSONSection, paramKey, paramMap.get(paramKey), curInstance);
					else
						U.d("Dropped extra key found in JSON structure: " + paramKey + " for entry " + elemName + " in " + sectionKey + ".", 1);
			} catch (JSONException e)
			{
				U.e("Issue parsing " + sectionKey + " during config loading. Probably an internal error with the \"" + sectionKey + "\" handler.");
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
		Map<String, Class<?>> configMembers = Config.findConfigMembers();
		JSONObject data = new JSONObject(U.readFile(filename));
		// instantiate all objects first off, so references can be resolved
		// later
		for (String curSectionKey : data.keySet())
			try
			{
				JSONObject jsonData = data.optJSONObject(curSectionKey);
				Class<?> type = configMembers.get(curSectionKey);
				this.instantiateSectionElems(curSectionKey, jsonData, type);
			} catch (ClassCastException ex)
			{
				U.e("Error, couldn't find parsing structure for " + curSectionKey + ". Did you spell the name correctly? Or are the correct parseables not listed?");
			}
		// Parse each into memeory
		for (String curJSONKey : data.keySet())
			try
			{
				this.intelliParse(data, curJSONKey, this.getSection(curJSONKey));
			} catch (JSONException | UnknownReferenceException | UnknownDecoderException e)
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
	 * From the given JSONSection, and the specified key, parses the data into
	 * the passed exported parameter.
	 *
	 * @param curJSONSection
	 *            the json section to parse from
	 * @param curKey
	 *            the key to pull the data from
	 * @param curParam
	 *            the parameter to attempt to parse
	 * @throws UnknownReferenceException
	 *             if an unknown reference type is declared
	 * @throws UnknownDecoderException
	 *             if an unknown decoder is referenced
	 */
	private void parseParam(JSONObject curJSONSection, String curKey, ExportedParameter curParam, Object instance) throws UnknownReferenceException, UnknownDecoderException
	{
		if (U.matchesAny(curParam.getDataType(), "string", "str", "val", "value", "num", "number"))
			this.handlePrimitive(curJSONSection, curKey, curParam, instance);
		if (curParam.getDataType().startsWith("ref:"))
			this.handleReferences(curJSONSection, curKey, curParam, instance);
		else if (curParam.getDataType().startsWith("enum"))
			this.handleEnum(curJSONSection, curKey, curParam, instance);
		else if (curParam.getDataType().startsWith("decode:"))
			this.handleCustomDecode(curJSONSection, curKey, curParam, instance);
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
	 * Writes to file, a JSON equivalent of this loaded config.
	 *
	 * @param filename
	 *            the file to export to
	 * @throws UnknownDecoderException
	 * @throws JSONException
	 */
	public void writeToFile(String filename) throws JSONException, UnknownDecoderException
	{
		JSONObject output = new JSONObject();
		for (Entry<String, SectionManager> curConfigMember : this.maps.entrySet())
		{
			JSONObject member = new JSONObject();
			SectionManager curSecMan = curConfigMember.getValue();
			for (String curElemKey : curSecMan.getKeys())
				member.putOpt(curElemKey, this.intelliGen(curSecMan.getElem(curElemKey), curSecMan));
			output.putOnce(curConfigMember.getKey(), member);
		}
		U.writeToFile(filename, output.toString(4));
	}
}
