package config;

import java.util.Map;
import java.util.List;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class Rounds implements ConfigMember
{
	private List<String>		triggeredActions;
	private Map<String, String>	triggeredAtomics;
	private List<String>		activeMods;
	private List<String>		continueReqs;

	@ExportedParam(datatype = DType.STRLIST, key = "triggeredActions", methodtype = MType.GETTER, sortVal = 1)
	public List<String> getTriggeredActions()
	{
		return this.triggeredActions;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "triggeredAtomics", methodtype = MType.GETTER, sortVal = 2)
	public Map<String, String> getTriggeredAtomics()
	{
		return this.triggeredAtomics;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "activeMods", methodtype = MType.GETTER, sortVal = 3)
	public List<String> getactiveMods()
	{
		return this.activeMods;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "continueReqs", methodtype = MType.GETTER, sortVal = 4)
	public List<String> getcontinueReqs()
	{
		return this.continueReqs;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "triggeredActions", methodtype = MType.SETTER, sortVal = 1)
	public void setTriggeredActions(List<String> input)
	{
		this.triggeredActions = input;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "triggeredAtomics", methodtype = MType.SETTER, sortVal = 2)
	public void setTriggeredAtomics(Map<String, String> input)
	{
		this.triggeredAtomics = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "activeMods", methodtype = MType.SETTER, sortVal = 3)
	public void setActiveMods(List<String> input)
	{
		this.activeMods = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "continueReqs", methodtype = MType.SETTER, sortVal = 4)
	public void setContinueReqs(List<String> input)
	{
		this.continueReqs = input;
	}
}
