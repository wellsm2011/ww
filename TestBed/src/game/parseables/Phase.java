package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.MType;
import config.core.annotations.ExportedParam.SType;

@ConfigMember(sectionKey = "phases")
public class Phase
{
	private List<String>		triggeredActions;
	private Map<String, String>	triggeredAtomics;
	private List<String>		activeMods;
	private List<String>		continueReqs;

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "activeMods", methodtype = MType.GETTER, sortVal = 3)
	public List<String> getActiveMods()
	{
		return this.activeMods;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "continueReqs", methodtype = MType.GETTER, sortVal = 4)
	public List<String> getContinueReqs()
	{
		return this.continueReqs;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "triggeredActions", methodtype = MType.GETTER, sortVal = 1)
	public List<String> getTriggeredActions()
	{
		return this.triggeredActions;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "triggeredAtomics", methodtype = MType.GETTER, sortVal = 2)
	public Map<String, String> getTriggeredAtomics()
	{
		return this.triggeredAtomics;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "activeMods", methodtype = MType.SETTER, sortVal = 3)
	public void setActiveMods(List<String> input)
	{
		this.activeMods = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "continueReqs", methodtype = MType.SETTER, sortVal = 4)
	public void setContinueReqs(List<String> input)
	{
		this.continueReqs = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "triggeredActions", methodtype = MType.SETTER, sortVal = 1)
	public void setTriggeredActions(List<String> input)
	{
		this.triggeredActions = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "triggeredAtomics", methodtype = MType.SETTER, sortVal = 2)
	public void setTriggeredAtomics(Map<String, String> input)
	{
		this.triggeredAtomics = input;
	}
}
