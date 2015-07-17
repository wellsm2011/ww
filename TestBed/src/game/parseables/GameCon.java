package game.parseables;

import java.util.Map;

import config.core.ConfigMember;
import config.core.ExportedParam;
import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;

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
