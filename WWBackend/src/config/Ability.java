package config;

import java.util.List;
import java.util.Map;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class Ability implements ConfigMember
{
	private Map<String, String>	appliedAtomics;
	private List<String>		appliedActions;
	private List<String>		tags;
	private Map<String, String>	triggerData;
	private Map<String, String>	targetingData;

	@ExportedParam(datatype = DType.STRLIST, key = "appliedActions", methodtype = MType.GETTER, sortVal = 4)
	public List<String> getAppliedActions()
	{
		return this.appliedActions;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "appliedAtomics", methodtype = MType.GETTER, sortVal = 5)
	public Map<String, String> getAppliedAtomics()
	{
		return this.appliedAtomics;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "tags", methodtype = MType.GETTER, sortVal = 1)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "targeting", methodtype = MType.GETTER, sortVal = 3)
	public Map<String, String> getTargetingData()
	{
		return this.targetingData;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "trigger", methodtype = MType.GETTER, sortVal = 2)
	public Map<String, String> getTriggerData()
	{
		return this.triggerData;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "appliedActions", methodtype = MType.SETTER, sortVal = 4)
	public void setAppliedActions(List<String> input)
	{
		this.appliedActions = input;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "appliedAtomics", methodtype = MType.SETTER, sortVal = 5)
	public void setAppliedAtomics(Map<String, String> input)
	{
		this.appliedAtomics = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "tags", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "targeting", methodtype = MType.SETTER, sortVal = 3)
	public void setTargetingData(Map<String, String> input)
	{
		this.targetingData = input;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "trigger", methodtype = MType.SETTER, sortVal = 2)
	public void setTriggerData(Map<String, String> input)
	{
		this.triggerData = input;
	}
}
