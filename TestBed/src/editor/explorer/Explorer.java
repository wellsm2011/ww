package editor.explorer;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import backend.U;
import config.Config;
import config.ConfigMember;
import config.explorer.GettableParameter;
import config.explorer.SettableParameter;
import editor.explorer.interfaces.ExportedParameter;

public class Explorer
{
	private static final String														SETTER_EXPORT_DEF	= "set";
	private static final String														GETTER_EXPORT_DEF	= "get";
	private Config																	config;
	private LinkedHashMap<String, LinkedHashMap<String, List<ExportedParameter>>>	mappedInfo;

	public Explorer(Config config)
	{
		this.config = config;
		this.mappedInfo = new LinkedHashMap<String, LinkedHashMap<String, List<ExportedParameter>>>();

		for (Entry<String, LinkedHashMap<String, ? extends ConfigMember>> curSection : this.config.getAllMaps().entrySet())
		{
			String sectionName = curSection.getKey();
			this.mappedInfo.put(sectionName, new LinkedHashMap<String, List<ExportedParameter>>());
			for (Entry<String, ? extends ConfigMember> curElem : curSection.getValue().entrySet())
				this.mappedInfo.get(sectionName).put(curElem.getKey(), this.findExportedOptions(curElem.getValue()));
		}
	}

	private List<ExportedParameter> findExportedOptions(ConfigMember input)
	{
		LinkedHashMap<String, Method> setters = new LinkedHashMap<String, Method>();
		LinkedHashMap<String, Method> getters = new LinkedHashMap<String, Method>();

		for (Method curMethod : input.getClass().getDeclaredMethods())
			if (curMethod.isAnnotationPresent(GettableParameter.class))
				getters.put(curMethod.getName().substring(Explorer.GETTER_EXPORT_DEF.length()), curMethod);
			else if (curMethod.isAnnotationPresent(SettableParameter.class))
				setters.put(curMethod.getName().substring(Explorer.SETTER_EXPORT_DEF.length()), curMethod);

		U.p("Getters: " + getters);
		U.p("Setters: " + setters);
		LinkedList<ExportedParameter> exportedOptions = new LinkedList<ExportedParameter>();
		for (String curOption : getters.keySet())
			if (setters.containsKey(curOption))
				exportedOptions.add(new ExportedOption(curOption, setters.get(curOption), getters.get(curOption), input));
			else
				exportedOptions.add(new ExportedView(curOption, getters.get(curOption), input));

		return exportedOptions;
	}

	public LinkedHashMap<String, LinkedHashMap<String, List<ExportedParameter>>> getMappedInfo()
	{
		return this.mappedInfo;
	}
}
