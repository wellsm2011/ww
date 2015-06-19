package editor.explorer;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import config.core.Config;
import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.MType;

public class Explorer
{
	private LinkedHashMap<String, LinkedHashMap<String, ? extends ConfigMember>>	configMembers;

	public Explorer(Config config)
	{
		this.configMembers = config.getAllMaps();
	}

	private List<ExportedParameter> findMethodsByFilter(ConfigMember input, MType... filter)
	{
		LinkedList<ExportedParameter> res = new LinkedList<ExportedParameter>();
		LinkedHashMap<String, LinkedHashMap<MType, Method>> map = new LinkedHashMap<String, LinkedHashMap<MType, Method>>();
		for (Method curMethod : input.getClass().getDeclaredMethods())
			if (curMethod.isAnnotationPresent(ExportedParam.class))
			{
				ExportedParam paramData = curMethod.getAnnotation(ExportedParam.class);
				if (!map.containsKey(paramData.key()))
					map.put(paramData.key(), new LinkedHashMap<MType, Method>());
				map.get(paramData.key()).put(paramData.methodtype(), curMethod);
			}

		if (filter.length <= 0)
			for (Entry<String, LinkedHashMap<MType, Method>> curParam : map.entrySet())
				res.add(new ExportedParameter(curParam.getKey(), input, curParam.getValue()));
		if (filter.length > 0)
			for (Entry<String, LinkedHashMap<MType, Method>> curParam : map.entrySet())
			{
				LinkedHashMap<MType, Method> methods = new LinkedHashMap<MType, Method>();
				for (MType curFilter : filter)
					methods.put(curFilter, curParam.getValue().get(curFilter));
				res.add(new ExportedParameter(curParam.getKey(), input, methods));
			}
		return res;
	}

	public LinkedHashMap<String, LinkedHashMap<String, List<ExportedParameter>>> getMappedOptions()
	{
		LinkedHashMap<String, LinkedHashMap<String, List<ExportedParameter>>> mappedOptions = new LinkedHashMap<String, LinkedHashMap<String, List<ExportedParameter>>>();

		for (Entry<String, LinkedHashMap<String, ? extends ConfigMember>> curSection : this.configMembers.entrySet())
		{
			String sectionName = curSection.getKey();
			mappedOptions.put(sectionName, new LinkedHashMap<String, List<ExportedParameter>>());
			for (Entry<String, ? extends ConfigMember> curElem : curSection.getValue().entrySet())
				mappedOptions.get(sectionName).put(curElem.getKey(), this.findMethodsByFilter(curElem.getValue()));
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
