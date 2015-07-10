package config.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import backend.U;
import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;

/**
 * This class is a helper class for the main Config system, what this does is it
 * allows nice populating and handling of distinct types, designed to allow for
 * simple per-type editing and accessing of loaded types at runtime.
 * 
 * @author Andrew Binns
 */
public class SectionManager
{
	private static MType[]	stdParamFilter;
	static
	{
		SectionManager.stdParamFilter = new MType[]
		{ MType.GETTER, MType.SETTER };
	}

	/**
	 * Takes a input class, and a list of method types to find, and returns a
	 * mapping of those those methods wrapped in ExportedParameters with names.
	 * 
	 * @param input
	 *            the input class to analyse
	 * @param filter
	 *            the method types to look for in the class
	 * @return
	 */
	public static Map<String, ExportedParameter> findParametersByFilter(Class<?> input, MType... filter)
	{
		LinkedHashMap<String, ExportedParameter> res = new LinkedHashMap<String, ExportedParameter>();
		LinkedHashMap<String, LinkedHashMap<MType, Method>> map = new LinkedHashMap<String, LinkedHashMap<MType, Method>>();
		LinkedHashMap<String, SType> keyToDataTypeMap = new LinkedHashMap<String, SType>();

		// Finds and lists all methods by method type
		for (Method curMethod : SectionManager.getSortedMethods(input))
			if (curMethod.isAnnotationPresent(ExportedParam.class))
			{
				ExportedParam paramData = curMethod.getAnnotation(ExportedParam.class);
				if (!map.containsKey(paramData.key()))
					map.put(paramData.key(), new LinkedHashMap<MType, Method>());
				map.get(paramData.key()).put(paramData.methodtype(), curMethod);
				keyToDataTypeMap.put(paramData.key(), paramData.storetype());
			}

		// If no filters are selected, return them all
		if (filter.length <= 0)
			for (Entry<String, LinkedHashMap<MType, Method>> curParam : map.entrySet())
				res.put(curParam.getKey(), new ExportedParameter(curParam.getKey(), curParam.getValue(), keyToDataTypeMap.get(curParam.getKey())));

		// Get all methods mentioned in the filter
		if (filter.length > 0)
			for (Entry<String, LinkedHashMap<MType, Method>> curParam : map.entrySet())
			{
				LinkedHashMap<MType, Method> methods = new LinkedHashMap<MType, Method>();
				for (MType curFilter : filter)
					methods.put(curFilter, curParam.getValue().get(curFilter));
				res.put(curParam.getKey(), new ExportedParameter(curParam.getKey(), methods, keyToDataTypeMap.get(curParam.getKey())));
			}
		return res;
	}

	/**
	 * Finds a list of methods in a given class that have the ExportedParam
	 * annotation, and sorts them as it goes
	 * 
	 * @param input
	 *            the class to scan for ExportedParam annotated methods
	 * @return a sorted array of sorted, ExportedParam-annotated methods.
	 */
	private static Method[] getSortedMethods(Class<?> input)
	{
		PriorityQueue<Method> sorted = new PriorityQueue<Method>((Method a, Method b) -> {
			int aOrd = a.getAnnotation(ExportedParam.class).sortVal();
			int bOrd = b.getAnnotation(ExportedParam.class).sortVal();
			return aOrd - bOrd;
		});
		Method[] declaredMethods = input.getDeclaredMethods();
		for (Method m : declaredMethods)
			if (m.isAnnotationPresent(ExportedParam.class))
				sorted.offer(m);
		Method[] res = new Method[sorted.size()];
		int i = 0;
		while (!sorted.isEmpty())
			res[i++] = sorted.poll();
		return res;
	}

	private Class<?>						type;

	private Map<String, Object>				dataItems;

	private Map<String, ExportedParameter>	paramMappings;

	private String							keyName;

	/**
	 * <p>
	 * Initializes this SectionManager with the provided Class object. This then
	 * caches a mapping of parameters for this type, as well as some other
	 * information to make editing objects of this type easy later on.
	 * </p>
	 * <p>
	 * This is here primarily for organization and to allow for per-type
	 * structures, such as the config editing system.
	 * </p>
	 * <p>
	 * This also stores a map of the currently stored type.
	 * 
	 * @param type
	 *            the type this SectionManager manages.
	 */
	public SectionManager(Class<?> type)
	{
		this.type = type;
		this.dataItems = new LinkedHashMap<String, Object>();
		if (this.type.isAnnotationPresent(ConfigMember.class))
			this.keyName = this.type.getAnnotation(ConfigMember.class).sectionKey();
	}

	/**
	 * Based on a given key, returns the element associated with said key stored
	 * in this section. Returns null if key is invalid.
	 * 
	 * @param key
	 *            the key of the item to attempt to retrieve
	 * @return the element assotiated with the given key
	 * @see SectionManager#getKeys
	 */
	public Object getElem(String key)
	{
		return this.dataItems.get(key);
	}

	/**
	 * Returns the map of strings to this section's stored items.
	 * 
	 * @return a map named entries of this section's type.
	 */
	@Deprecated
	public Map<String, Object> getEntries()
	{
		return this.dataItems;
	}

	/**
	 * Gets the JSON key for this manager's stored type.
	 * 
	 * @return the JSON key for this type in the config file.
	 */
	public String getKey()
	{
		return this.keyName;
	}

	/**
	 * Returns a set of strings for iteration over this section's elements. Can
	 * be used to retrieve the element associated with the given key.
	 * 
	 * @return a set of key strings
	 * @see SectionManager#getElem
	 */
	public Set<String> getKeys()
	{
		return this.dataItems.keySet();
	}

	/**
	 * Gets a mapping of all parameters for this type, for easy manipulation of
	 * runtime objects of this section's type.
	 * 
	 * @return a mapping of names to specific modifiable parameters.
	 */
	public Map<String, ExportedParameter> getParamMappings()
	{
		if (this.paramMappings == null)
			this.paramMappings = SectionManager.findParametersByFilter(this.type, SectionManager.stdParamFilter);
		return this.paramMappings;
	}

	/**
	 * Gets the type this particular SectionManager manages
	 * 
	 * @return the type this manager manages.
	 */
	public Class<?> getType()
	{
		return this.type;
	}

	/**
	 * Stores a data-member by name. Does not accept objects that are of the
	 * correct type, this is simply not allowed.
	 * 
	 * @param key
	 * @param curInstance
	 */
	public void offer(String key, Object curInstance)
	{
		if (!this.type.isInstance(curInstance))
			U.e("Error, was passed " + curInstance.toString() + " of type " + curInstance.getClass() + " for type " + this.type + ".\nThis is not OK. No data stored.");
		else
			this.dataItems.put(key, curInstance);
	}

	/**
	 * When given an element that fits in this section, returns a mapping of
	 * param keys to current values as strings. Designed for reporting and
	 * debugging use.
	 * 
	 * @param in
	 *            the element to find the current status of
	 * @return a map of param names to current values for the given object
	 */
	public Map<String, String> getGettablesFor(Object in)
	{
		Map<String, String> res = new HashMap<String, String>();
		if (!this.type.isInstance(in) && in != null)
			U.e("Error, was passed " + in.toString() + " of type " + in.getClass() + " for type " + this.type + ".\nThis is not OK. No data parsed.");
		else if (in != null)
			for (Entry<String, ExportedParameter> curParam : this.paramMappings.entrySet())
				res.put(curParam.getKey(), curParam.getValue().getGettableAsString(in));
		return res;
	}

	public Map<String, String> getGettablesForKey(String key)
	{
		if (!this.dataItems.containsKey(key))
			U.e("Error, was passed key " + key + ". This does not matcha anything on file, returning a blank map.");
		return this.getGettablesFor(this.dataItems.get(key));
	}

	public <T> T[] getItems()
	{
		return U.cleanCast(this.dataItems.values().toArray());
	}
}
