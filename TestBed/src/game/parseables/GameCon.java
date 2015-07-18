package game.parseables;

import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.MType;
import config.core.annotations.ExportedParam.SType;

@ConfigMember(sectionKey = "gameCons")
public class GameCon
{
	private Map<String, String>	triggerData;
	private Map<String, String>	resultAtomics;

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "resultAtomics", methodtype = MType.GETTER, sortVal = 2)
	public Map<String, String> getAppliedAtomics()
	{
		return this.resultAtomics;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", methodtype = MType.GETTER, sortVal = 1)
	public Map<String, String> getTriggerData()
	{
		return this.triggerData;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "resultAtomics", methodtype = MType.SETTER, sortVal = 2)
	public void setAppliedAtomics(Map<String, String> input)
	{
		this.resultAtomics = input;
	}

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", methodtype = MType.SETTER, sortVal = 1)
	public void setTriggerData(Map<String, String> input)
	{
		this.triggerData = input;
	}
}
