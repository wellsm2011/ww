package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "abilities")
public class Ability
{
	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "tags", sortVal = 1)
	private List<String> tags;

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "trigger", sortVal = 2)
	private Map<String, String> triggerData;

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "targeting", sortVal = 3)
	private Map<String, String> targetingData;

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "appliedActions", sortVal = 4)
	private List<Action> appliedActions;

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "appliedAtomics", sortVal = 5)
	private Map<String, Atomic> appliedAtomics;
}
