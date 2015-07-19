package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "actionMods")
public class ActionModifier
{
	@ExportedParam(storetype = SType.SINGLE, dataType = "num", key = "priority", sortVal = 1)
	private double priority;

	@ExportedParam(storetype = SType.MAP, dataType = "str", key = "trigger", sortVal = 2)
	private Map<String, String> triggerData;

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "effect", sortVal = 3)
	private Map<String, String> effectData;

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "triggeredActions", sortVal = 4)
	private List<Action> triggeredActions;

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "triggeredAtomics", sortVal = 5)
	private Map<String, Atomic> triggeredAtomics;
}
