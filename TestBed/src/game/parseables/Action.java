package game.parseables;

import java.util.List;
import java.util.Map;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;
import game.core.Atomic;

@ConfigMember(sectionKey = "actions")
public class Action
{
	@ExportedParam(storetype = SType.LIST, dataType = "val", key = "tags", sortVal = 1)
	private List<String> tags;

	@ExportedParam(storetype = SType.SINGLE, dataType = "val", key = "priority", sortVal = 2)
	private double priority;

	@ExportedParam(storetype = SType.MAP, dataType = "decode:Atomic", key = "appliedAtomics", sortVal = 3)
	private Map<String, Atomic> appliedAtomics;

	@ExportedParam(storetype = SType.LIST, dataType = "ref:actions", key = "appliedActions", sortVal = 4)
	private List<Action> appliedActions;

	public String toString()
	{
		return "{" + tags + ";" + priority + ";" + appliedAtomics + ";" + appliedActions + "}";
	}
}
