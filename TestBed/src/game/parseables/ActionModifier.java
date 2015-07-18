package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.MType;
import config.core.annotations.ExportedParam.SType;

@ConfigMember(sectionKey = "actionMods")
public class ActionModifier
{
	private Map<String, String>	triggerData;
	private Map<String, String>	effectData;
	private List<String>		triggeredActions;
	private Map<String, String>	triggeredAtomics;
	private Double				priority;

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "effect", methodtype = MType.GETTER, sortVal = 3)
	public Map<String, String> getEffectData()
	{
		return this.effectData;
	}

	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "priority", methodtype = MType.GETTER, sortVal = 1)
	public double getTags()
	{
		return this.priority;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", methodtype = MType.GETTER, sortVal = 2)
	public Map<String, String> getTriggerData()
	{
		return this.triggerData;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "triggeredActions", methodtype = MType.GETTER, sortVal = 4)
	public List<String> getTriggeredActions()
	{
		return this.triggeredActions;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "triggeredAtomics", methodtype = MType.GETTER, sortVal = 5)
	public Map<String, String> getTriggeredAtomics()
	{
		return this.triggeredAtomics;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "effect", methodtype = MType.SETTER, sortVal = 3)
	public void setEffectData(Map<String, String> input)
	{
		this.effectData = input;
	}

	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "priority", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(double input)
	{
		this.priority = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", methodtype = MType.SETTER, sortVal = 2)
	public void setTriggerData(Map<String, String> input)
	{
		this.triggerData = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "triggeredActions", methodtype = MType.SETTER, sortVal = 4)
	public void setTriggeredActions(List<String> input)
	{
		this.triggeredActions = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "triggeredAtomics", methodtype = MType.SETTER, sortVal = 5)
	public void setTriggeredAtomics(Map<String, String> input)
	{
		this.triggeredAtomics = input;
	}
}
