package config;

import java.util.List;
import java.util.Map;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class Action implements ConfigMember
{
	private List<String>		tags;
	private Map<String, String>	appliedAtomics;
	private List<String>		appliedActions;
	private double				priority;
	private double				fred;

	@ExportedParam(datatype = DType.STRLIST, key = "appliedActions", methodtype = MType.GETTER, sortVal = 4)
	public List<String> getAppliedActions()
	{
		return this.appliedActions;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "appliedAtomics", methodtype = MType.GETTER, sortVal = 3)
	public Map<String, String> getAppliedAtomics()
	{
		return this.appliedAtomics;
	}

	@ExportedParam(datatype = DType.NUM, key = "priority", methodtype = MType.GETTER, sortVal = 2)
	public double getPriority()
	{
		return this.priority;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "tags", methodtype = MType.GETTER, sortVal = 1)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "appliedActions", methodtype = MType.SETTER, sortVal = 4)
	public void setAppliedActions(List<String> input)
	{
		this.appliedActions = input;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "appliedAtomics", methodtype = MType.SETTER, sortVal = 3)
	public void setAppliedAtomics(Map<String, String> input)
	{
		this.appliedAtomics = input;
	}

	@ExportedParam(datatype = DType.NUM, key = "priority", methodtype = MType.SETTER, sortVal = 2)
	public void setPriority(double input)
	{
		this.priority = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "tags", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}
}
