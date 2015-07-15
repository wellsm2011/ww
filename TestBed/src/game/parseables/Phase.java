package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.ConfigMember;
import config.core.ExportedParam;
import config.core.ExportedParam.DType;
import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;

@ConfigMember(sectionKey = "phases")
public class Phase
{
	private List<String>		triggeredActions;
	private Map<String, String>	triggeredAtomics;
	private List<String>		activeMods;
	private List<String>		continueReqs;

	@ExportedParam(storetype = SType.LIST, key = "activeMods", methodtype = MType.GETTER, sortVal = 3, datatype = DType.REF)
	public List<String> getActiveMods()
	{
		return this.activeMods;
	}

	@ExportedParam(storetype = SType.LIST, key = "continueReqs", methodtype = MType.GETTER, sortVal = 4, datatype = DType.VAL)
	public List<String> getContinueReqs()
	{
		return this.continueReqs;
	}

	@ExportedParam(storetype = SType.LIST, key = "triggeredActions", methodtype = MType.GETTER, sortVal = 1, datatype = DType.REF)
	public List<String> getTriggeredActions()
	{
		return this.triggeredActions;
	}

	@ExportedParam(storetype = SType.MAP, key = "triggeredAtomics", methodtype = MType.GETTER, sortVal = 2, datatype = DType.REF)
	public Map<String, String> getTriggeredAtomics()
	{
		return this.triggeredAtomics;
	}

	@ExportedParam(storetype = SType.LIST, key = "activeMods", methodtype = MType.SETTER, sortVal = 3, datatype = DType.REF)
	public void setActiveMods(List<String> input)
	{
		this.activeMods = input;
	}

	@ExportedParam(storetype = SType.LIST, key = "continueReqs", methodtype = MType.SETTER, sortVal = 4, datatype = DType.VAL)
	public void setContinueReqs(List<String> input)
	{
		this.continueReqs = input;
	}

	@ExportedParam(storetype = SType.LIST, key = "triggeredActions", methodtype = MType.SETTER, sortVal = 1, datatype = DType.REF)
	public void setTriggeredActions(List<String> input)
	{
		this.triggeredActions = input;
	}

	@ExportedParam(storetype = SType.MAP, key = "triggeredAtomics", methodtype = MType.SETTER, sortVal = 2, datatype = DType.REF)
	public void setTriggeredAtomics(Map<String, String> input)
	{
		this.triggeredAtomics = input;
	}
}
