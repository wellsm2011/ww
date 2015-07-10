package parseables;

import java.util.List;
import java.util.Map;

import config.core.ConfigMember;
import config.core.ExportedParam;
import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;

@ConfigMember(sectionKey = "statuses")
public class Status
{
	private List<String>		actionMods;
	private Map<String, String>	application;
	private List<String>		onEndActions;
	private Map<String, String>	onEndAtomics;
	private List<String>		tags;
	private String				access;

	@ExportedParam(storetype = SType.SINGLE, key = "access", methodtype = MType.GETTER, sortVal = 1)
	public String getAccess()
	{
		return this.access;
	}

	@ExportedParam(storetype = SType.LIST, key = "actionMods", methodtype = MType.GETTER, sortVal = 3)
	public List<String> getActionModifierList()
	{
		return this.actionMods;
	}

	@ExportedParam(storetype = SType.MAP, key = "application", methodtype = MType.GETTER, sortVal = 4)
	public Map<String, String> getApplicationData()
	{
		return this.application;
	}

	@ExportedParam(storetype = SType.LIST, key = "onEndActions", methodtype = MType.GETTER, sortVal = 5)
	public List<String> getOnEndActionList()
	{
		return this.onEndActions;
	}

	@ExportedParam(storetype = SType.MAP, key = "onEndAtomics", methodtype = MType.GETTER, sortVal = 6)
	public Map<String, String> getOnEndAtomicsData()
	{
		return this.onEndAtomics;
	}

	@ExportedParam(storetype = SType.LIST, key = "tags", methodtype = MType.GETTER, sortVal = 2)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(storetype = SType.SINGLE, key = "access", methodtype = MType.SETTER, sortVal = 1)
	public void setAccess(String input)
	{
		this.access = input;
	}

	@ExportedParam(storetype = SType.LIST, key = "actionMods", methodtype = MType.SETTER, sortVal = 3)
	public void setActionModifierList(List<String> input)
	{
		this.actionMods = input;
	}

	@ExportedParam(storetype = SType.MAP, key = "application", methodtype = MType.SETTER, sortVal = 4)
	public void setApplicationData(Map<String, String> input)
	{
		this.application = input;
	}

	@ExportedParam(storetype = SType.LIST, key = "onEndActions", methodtype = MType.SETTER, sortVal = 5)
	public void setOnEndActionList(List<String> input)
	{
		this.onEndActions = input;
	}

	@ExportedParam(storetype = SType.MAP, key = "onEndAtomics", methodtype = MType.SETTER, sortVal = 6)
	public void setOnEndAtomicsData(Map<String, String> input)
	{
		this.onEndAtomics = input;
	}

	@ExportedParam(storetype = SType.LIST, key = "tags", methodtype = MType.SETTER, sortVal = 2)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}

}
