package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "statuses")
public class Status
{
	@ExportedParam(storetype = SType.LIST, dataType = "ref:actionMods", key = "actionMods", sortVal = 3)
	private List<ActionModifier> actionMods;

	@ExportedParam(storetype = SType.MAP, dataType = "string", key = "application", sortVal = 4)
	private Map<String, String> application;

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "onEndActions", sortVal = 5)
	private List<Action> onEndActions;

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "onEndAtomics", sortVal = 6)
	private Map<String, Atomic> onEndAtomics;

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "tags", sortVal = 2)
	private List<String> tags;

	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "access", sortVal = 1)
	private String access;

}
