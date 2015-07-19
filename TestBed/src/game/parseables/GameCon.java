package game.parseables;

import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "gameCons")
public class GameCon
{
	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", sortVal = 1)
	private Map<String, String> triggerData;

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "resultAtomics", sortVal = 2)
	private Map<String, Atomic> resultAtomics;
}
