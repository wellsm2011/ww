package config.explorer;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import config.core.Config;
import config.core.ConfigMember;
import config.core.SectionManager;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class Explorer
{
	public static Map<String, ExportedParameter> findParametersByFilter(Object input, MType... filter)
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

	private static Method[] getSortedMethods(Object input)
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

	private LinkedHashMap<String, SectionManager>	configMembers;

	public Explorer(Config config)
	{
		this.configMembers = config.getAllMaps();
	}

	public LinkedHashMap<String, SectionManager> getMappedOptions()
	{
		LinkedHashMap<String, LinkedHashMap<String, Collection<ExportedParameter>>> mappedOptions = new LinkedHashMap<String, LinkedHashMap<String, Collection<ExportedParameter>>>();

		for (Entry<String, SectionManager> curSection : this.configMembers.entrySet())
		{
			String sectionName = curSection.getKey();
			mappedOptions.put(sectionName, new LinkedHashMap<String, Collection<ExportedParameter>>());
			for (Entry<String, ? extends ConfigMember> curElem : curSection.getValue().entrySet())
				mappedOptions.get(sectionName).put(curElem.getKey(), Explorer.findParametersByFilter(curElem.getValue()).values());
		}
		return mappedOptions;
	}
}
