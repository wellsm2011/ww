package editor;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import backend.U;
import config.Config;
import config.ConfigMember;

public class Explorer
{
	private static final String	SETTER_EXPORT_DEF	= "setOpt";
	private static final String	GETTER_EXPORT_DEF	= "getOpt";
	private Config				config;

	public Explorer(Config config)
	{
		this.config = config;

		for (Entry<String, LinkedHashMap<String, ? extends ConfigMember>> curSection : this.config.getAllMaps().entrySet())
		{
			String sectionName = curSection.getKey();
			for (Entry<String, ? extends ConfigMember> curElem : curSection.getValue().entrySet())
				this.debug(curElem.getKey(), curElem.getValue());
		}
	}

	private void debug(String key, ConfigMember value)
	{
		U.p(key + " --- " + this.findExportedOptions(value));
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
}
