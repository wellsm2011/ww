package config;

import java.util.Map;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class GameCon implements ConfigMember
{
	private Map<String, String>	triggerData;
	private Map<String, String>	resultAtomics;

	@ExportedParam(datatype = DType.STRMAP, key = "resultAtomics", methodtype = MType.GETTER, sortVal = 2)
	public Map<String, String> getAppliedAtomics()
	{
		return this.resultAtomics;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "trigger", methodtype = MType.GETTER, sortVal = 1)
	public Map<String, String> getTriggerData()
	{
		return this.triggerData;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "resultAtomics", methodtype = MType.SETTER, sortVal = 2)
	public void setAppliedAtomics(Map<String, String> input)
	{
		this.resultAtomics = input;
	}

	@ExportedParam(datatype = DType.STRMAP, key = "trigger", methodtype = MType.SETTER, sortVal = 1)
	public void setTriggerData(Map<String, String> input)
	{
		this.triggerData = input;
	}
}
