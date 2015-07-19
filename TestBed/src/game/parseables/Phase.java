package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "phases")
public class Phase
{
	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "triggeredActions", sortVal = 1)
	private List<Action> triggeredActions;

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "triggeredActions", sortVal = 2)
	private Map<String, Atomic> triggeredAtomics;

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actionMods", key = "activeMods", sortVal = 3)
	private List<ActionModifier> activeModifiers;

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "continueReqs", sortVal = 4)
	private List<String> continueReqs;
}
