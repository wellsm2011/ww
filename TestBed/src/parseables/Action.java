package parseables;

import java.util.List;
import java.util.Map;

import config.core.ConfigMember;
import config.core.ExportedParam;
import config.core.ExportedParam.DType;
import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;

@ConfigMember(sectionKey = "actions")
public class Action
{
	private List<String>		tags;
	private Map<String, String>	appliedAtomics;
	private List<String>		appliedActions;
	private double				priority;

	@ExportedParam(storetype = SType.LIST, key = "appliedActions", methodtype = MType.GETTER, sortVal = 4, datatype = DType.REF)
	public List<String> getAppliedActions()
	{
		return this.appliedActions;
	}

	@ExportedParam(storetype = SType.MAP, key = "appliedAtomics", methodtype = MType.GETTER, sortVal = 3, datatype = DType.REF)
	public Map<String, String> getAppliedAtomics()
	{
		return this.appliedAtomics;
	}

	@ExportedParam(storetype = SType.SINGLE, key = "priority", methodtype = MType.GETTER, sortVal = 2, datatype = DType.VAL)
	public double getPriority()
	{
		return this.priority;
	}

	@ExportedParam(storetype = SType.LIST, key = "tags", methodtype = MType.GETTER, sortVal = 1, datatype = DType.VAL)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(storetype = SType.LIST, key = "appliedActions", methodtype = MType.SETTER, sortVal = 4, datatype = DType.REF)
	public void setAppliedActions(List<String> input)
	{
		this.appliedActions = input;
	}

	@ExportedParam(storetype = SType.MAP, key = "appliedAtomics", methodtype = MType.SETTER, sortVal = 3, datatype = DType.REF)
	public void setAppliedAtomics(Map<String, String> input)
	{
		this.appliedAtomics = input;
	}

	@ExportedParam(storetype = SType.SINGLE, key = "priority", methodtype = MType.SETTER, sortVal = 2, datatype = DType.VAL)
	public void setPriority(double input)
	{
		this.priority = input;
	}

	@ExportedParam(storetype = SType.LIST, key = "tags", methodtype = MType.SETTER, sortVal = 1, datatype = DType.VAL)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}
}
