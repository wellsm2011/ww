package config.core;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import backend.U;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;
import config.explorer.ExportedParameter;

public class SectionManager
{
	private static MType[]	stdParamFilter;
	static
	{
		SectionManager.stdParamFilter = new MType[]
		{ MType.GETTER, MType.SETTER };
	}

	public static Map<String, ExportedParameter> findParametersByFilter(Class<?> input, MType... filter)
	{
		LinkedHashMap<String, ExportedParameter> res = new LinkedHashMap<String, ExportedParameter>();
		LinkedHashMap<String, LinkedHashMap<MType, Method>> map = new LinkedHashMap<String, LinkedHashMap<MType, Method>>();
		LinkedHashMap<String, DType> keyToDataTypeMap = new LinkedHashMap<String, DType>();

		for (Method curMethod : SectionManager.getSortedMethods(input))
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
				res.put(curParam.getKey(), new ExportedParameter(curParam.getKey(), curParam.getValue(), keyToDataTypeMap.get(curParam.getKey())));

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

	public SectionManager(Class<?> type)
	{
		this.type = type;
		this.dataItems = new LinkedHashMap<String, Object>();
	}

	public Map<String, Object> getEntries()
	{
		return this.dataItems;
	}

	public Map<String, ExportedParameter> getParamMappings()
	{
		if (this.paramMappings == null)
			this.paramMappings = SectionManager.findParametersByFilter(this.type, SectionManager.stdParamFilter);
		return this.paramMappings;
	}

	public void offer(String key, Object curInstance)
	{
		if (!this.type.isInstance(curInstance))
			U.e("Error, was passed " + curInstance.toString() + " for type " + this.type);
		this.dataItems.put(key, curInstance);
	}

	public Class<?> getType()
	{
		return this.type;
	}
}
