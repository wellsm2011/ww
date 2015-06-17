package editor.backend;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import config.Config;
import config.ConfigMember;

public class Explorer
{
	private static final String													SETTER_EXPORT_DEF	= "setOpt";
	private static final String													GETTER_EXPORT_DEF	= "getOpt";
	private Config																config;
	private LinkedHashMap<String, LinkedHashMap<String, List<ExportedOption>>>	mappedInfo;

	public Explorer(Config config)
	{
		this.config = config;
		this.mappedInfo = new LinkedHashMap<String, LinkedHashMap<String, List<ExportedOption>>>();

		for (Entry<String, LinkedHashMap<String, ? extends ConfigMember>> curSection : this.config.getAllMaps().entrySet())
		{
			String sectionName = curSection.getKey();
			this.mappedInfo.put(sectionName, new LinkedHashMap<String, List<ExportedOption>>());
			for (Entry<String, ? extends ConfigMember> curElem : curSection.getValue().entrySet())
				this.mappedInfo.get(sectionName).put(curElem.getKey(), this.findExportedOptions(curElem.getValue()));
		}
	}

	private List<ExportedOption> findExportedOptions(ConfigMember input)
	{
		LinkedHashMap<String, Method> setters = new LinkedHashMap<String, Method>();
		LinkedHashMap<String, Method> getters = new LinkedHashMap<String, Method>();

		for (Method curMethod : input.getClass().getDeclaredMethods())
			if (curMethod.getName().contains(Explorer.GETTER_EXPORT_DEF))
				getters.put(curMethod.getName().substring(Explorer.GETTER_EXPORT_DEF.length()), curMethod);
			else if (curMethod.getName().contains(Explorer.SETTER_EXPORT_DEF))
				setters.put(curMethod.getName().substring(Explorer.SETTER_EXPORT_DEF.length()), curMethod);

		LinkedList<ExportedOption> exportedOptions = new LinkedList<ExportedOption>();
		for (String curOption : setters.keySet())
			if (getters.containsKey(curOption))
				exportedOptions.add(new ExportedOption(curOption, setters.get(curOption), getters.get(curOption), input));

		return exportedOptions;
	}

	public LinkedHashMap<String, LinkedHashMap<String, List<ExportedOption>>> getMappedInfo()
	{
		return this.mappedInfo;
	}
}
