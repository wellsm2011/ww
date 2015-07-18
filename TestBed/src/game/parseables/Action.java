package game.parseables;

import java.util.List;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.MType;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "actions")
public class Action
{
	private List<String>	tags;
	private List<Atomic>	appliedAtomics;
	private List<Action>	appliedActions;
	private double			priority;

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "appliedActions", methodtype = MType.GETTER, sortVal = 4)
	public List<Action> getAppliedActions()
	{
		return this.appliedActions;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "decode:Atomic", key = "appliedAtomics", methodtype = MType.GETTER, sortVal = 3)
	public List<Atomic> getAppliedAtomics()
	{
		return this.appliedAtomics;
	}

	@ExportedParam(storetype = SType.SINGLE, dataType = "val", key = "priority", methodtype = MType.GETTER, sortVal = 2)
	public double getPriority()
	{
		return this.priority;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "val", key = "tags", methodtype = MType.GETTER, sortVal = 1)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "appliedActions", methodtype = MType.SETTER, sortVal = 4)
	public void setAppliedActions(List<Action> input)
	{
		this.appliedActions = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "decode:Atomic", key = "appliedAtomics", methodtype = MType.SETTER, sortVal = 3)
	public void setAppliedAtomics(List<Atomic> input)
	{
		this.appliedAtomics = input;
	}

	@ExportedParam(storetype = SType.SINGLE, dataType = "val", key = "priority", methodtype = MType.SETTER, sortVal = 2)
	public void setPriority(double input)
	{
		this.priority = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "val", key = "tags", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}
}
