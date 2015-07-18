package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.MType;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "abilities")
public class Ability
{
	private Map<String, Atomic>	appliedAtomics;
	private List<Action>		appliedActions;
	private List<String>		tags;
	private Map<String, String>	triggerData;
	private Map<String, String>	targetingData;

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "appliedActions", methodtype = MType.GETTER, sortVal = 4)
	public List<Action> getAppliedActions()
	{
		return this.appliedActions;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "appliedAtomics", methodtype = MType.GETTER, sortVal = 5)
	public Map<String, Atomic> getAppliedAtomics()
	{
		return this.appliedAtomics;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "tags", methodtype = MType.GETTER, sortVal = 1)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "targeting", methodtype = MType.GETTER, sortVal = 3)
	public Map<String, String> getTargetingData()
	{
		return this.targetingData;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", methodtype = MType.GETTER, sortVal = 2)
	public Map<String, String> getTriggerData()
	{
		return this.triggerData;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "appliedActions", methodtype = MType.SETTER, sortVal = 4)
	public void setAppliedActions(List<Action> input)
	{
		this.appliedActions = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "appliedAtomics", methodtype = MType.SETTER, sortVal = 5)
	public void setAppliedAtomics(Map<String, Atomic> input)
	{
		this.appliedAtomics = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "tags", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "targeting", methodtype = MType.SETTER, sortVal = 3)
	public void setTargetingData(Map<String, String> input)
	{
		this.targetingData = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", methodtype = MType.SETTER, sortVal = 2)
	public void setTriggerData(Map<String, String> input)
	{
		this.triggerData = input;
	}
}
