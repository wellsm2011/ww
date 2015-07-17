package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.ConfigMember;
import config.core.ExportedParam;
import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;

@ConfigMember(sectionKey = "actions")
public class Action
{
	private List<String>		tags;
	private Map<String, String>	appliedAtomics;
	private List<String>		appliedActions;
	private double				priority;

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "appliedActions", methodtype = MType.GETTER, sortVal = 4)
	public List<String> getAppliedActions()
	{
		return this.appliedActions;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "appliedAtomics", methodtype = MType.GETTER, sortVal = 3)
	public Map<String, String> getAppliedAtomics()
	{
		return this.appliedAtomics;
	}

	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "priority", methodtype = MType.GETTER, sortVal = 2)
	public double getPriority()
	{
		return this.priority;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "tags", methodtype = MType.GETTER, sortVal = 1)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "appliedActions", methodtype = MType.SETTER, sortVal = 4)
	public void setAppliedActions(List<String> input)
	{
		this.appliedActions = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "appliedAtomics", methodtype = MType.SETTER, sortVal = 3)
	public void setAppliedAtomics(Map<String, String> input)
	{
		this.appliedAtomics = input;
	}

	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "priority", methodtype = MType.SETTER, sortVal = 2)
	public void setPriority(double input)
	{
		this.priority = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "tags", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}
}
