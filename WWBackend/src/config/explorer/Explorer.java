package config.explorer;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import config.core.Config;
import config.core.ConfigMember;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class Explorer
{
	public static Map<String, ExportedParameter> findParametersByFilter(ConfigMember input, MType... filter)
	{
		LinkedHashMap<String, ExportedParameter> res = new LinkedHashMap<String, ExportedParameter>();
		LinkedHashMap<String, LinkedHashMap<MType, Method>> map = new LinkedHashMap<String, LinkedHashMap<MType, Method>>();
		LinkedHashMap<String, DType> keyToDataTypeMap = new LinkedHashMap<String, DType>();

		for (Method curMethod : Explorer.getSortedMethods(input))
			if (curMethod.isAnnotationPresent(ExportedParam.class))
			{
				ExportedParam paramData = curMethod.getAnnotation(ExportedParam.class);
				if (!map.containsKey(paramData.key()))
					map.put(paramData.key(), new LinkedHashMap<MType, Method>());
				map.get(paramData.key()).put(paramData.methodtype(), curMethod);
				keyToDataTypeMap.put(paramData.key(), paramData.datatype());
			}

		if (filter.length <= 0)
			for (Entry<String, LinkedHashMap<MType, Method>> curParam : map.entrySet())
				res.put(curParam.getKey(), new ExportedParameter(curParam.getKey(), input, curParam.getValue(), keyToDataTypeMap.get(curParam.getKey())));

		if (filter.length > 0)
			for (Entry<String, LinkedHashMap<MType, Method>> curParam : map.entrySet())
			{
				LinkedHashMap<MType, Method> methods = new LinkedHashMap<MType, Method>();
				for (MType curFilter : filter)
					methods.put(curFilter, curParam.getValue().get(curFilter));
				res.put(curParam.getKey(), new ExportedParameter(curParam.getKey(), input, methods, keyToDataTypeMap.get(curParam.getKey())));
			}
		return res;
	}

	private static Method[] getSortedMethods(ConfigMember input)
	{
		PriorityQueue<Method> sorted = new PriorityQueue<Method>((Method a, Method b) -> {
			int aOrd = a.getAnnotation(ExportedParam.class).sortVal();
			int bOrd = b.getAnnotation(ExportedParam.class).sortVal();
			return aOrd - bOrd;
		});
		Method[] declaredMethods = input.getClass().getDeclaredMethods();
		for (Method m : declaredMethods)
			if (m.isAnnotationPresent(ExportedParam.class))
				sorted.offer(m);
		Method[] res = new Method[sorted.size()];
		int i = 0;
		while (!sorted.isEmpty())
			res[i++] = sorted.poll();
		return res;
	}

	private LinkedHashMap<String, LinkedHashMap<String, ? extends ConfigMember>>	configMembers;

	public Explorer(Config config)
	{
		this.configMembers = config.getAllMaps();
	}

	public LinkedHashMap<String, LinkedHashMap<String, Collection<ExportedParameter>>> getMappedOptions()
	{
		LinkedHashMap<String, LinkedHashMap<String, Collection<ExportedParameter>>> mappedOptions = new LinkedHashMap<String, LinkedHashMap<String, Collection<ExportedParameter>>>();

		for (Entry<String, LinkedHashMap<String, ? extends ConfigMember>> curSection : this.configMembers.entrySet())
		{
			String sectionName = curSection.getKey();
			mappedOptions.put(sectionName, new LinkedHashMap<String, Collection<ExportedParameter>>());
			for (Entry<String, ? extends ConfigMember> curElem : curSection.getValue().entrySet())
				mappedOptions.get(sectionName).put(curElem.getKey(), Explorer.findParametersByFilter(curElem.getValue()).values());
		}
		return mappedOptions;
	}

	/*
	 * private List<ExportedView> findExportedParameter(ConfigMember input,
	 * MType filter) { List<ExportedView> exportedView = new
	 * LinkedList<ExportedView>(); for (Method curMethod :
	 * input.getClass().getDeclaredMethods()) if
	 * (curMethod.isAnnotationPresent(ExportedParam.class)) if
	 * (curMethod.getAnnotation(ExportedParam.class).methodtype() == filter)
	 * exportedView.add(new
	 * ExportedView(curMethod.getAnnotation(ExportedParam.class).key(),
	 * curMethod, input)); return exportedView; }
	 */

	/*
	 * private List<ExportedView> findExportedSetter(ConfigMember input) {
	 * List<ExportedView> exportedView = new LinkedList<ExportedView>(); for
	 * (Method curMethod : input.getClass().getDeclaredMethods()) if
	 * (curMethod.isAnnotationPresent(ExportedParam.class)) if
	 * (curMethod.getAnnotation(ExportedParam.class).methodtype() ==
	 * MType.SETTER) exportedView.add(new
	 * ExportedView(curMethod.getAnnotation(ExportedParam.class).key(),
	 * curMethod, input)); return exportedView; }
	 */

	/*
	 * private List<ExportedParameter> findExportedOptions(ConfigMember input) {
	 * LinkedHashMap<String, Method> setters = new LinkedHashMap<String,
	 * Method>(); LinkedHashMap<String, Method> getters = new
	 * LinkedHashMap<String, Method>();
	 * 
	 * for (Method curMethod : input.getClass().getDeclaredMethods()) if
	 * (curMethod.isAnnotationPresent(ExportedParam.class)) if
	 * (curMethod.getAnnotation(ExportedParam.class).methodtype() ==
	 * MType.GETTER)
	 * getters.put(curMethod.getAnnotation(ExportedParam.class).key(),
	 * curMethod); else if
	 * (curMethod.getAnnotation(ExportedParam.class).methodtype() ==
	 * MType.SETTER)
	 * setters.put(curMethod.getAnnotation(ExportedParam.class).key(),
	 * curMethod);
	 * 
	 * LinkedList<ExportedParameter> exportedOptions = new
	 * LinkedList<ExportedParameter>(); for (String curOption :
	 * getters.keySet()) if (setters.containsKey(curOption))
	 * exportedOptions.add(new ExportedOption(curOption, setters.get(curOption),
	 * getters.get(curOption), input));
	 * 
	 * return exportedOptions; }
	 */
}
