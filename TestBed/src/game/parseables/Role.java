package game.parseables;

import java.util.List;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;

@ConfigMember(sectionKey = "roles")
public class Role
{
	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "access", sortVal = 1)
	private String			access;

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "tags", sortVal = 2)
	private List<String>	tags;
	
	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "chatChannels", sortVal = 3)
	private List<String>	chatChannels;
	
	@ExportedParam(storetype = SType.LIST, dataType = "ref:gameCons", key = "winCondition", sortVal = 4)
	private List<String>	winCondition;
	
	@ExportedParam(storetype = SType.LIST, dataType = "ref:abilities", key = "grantedAbilities", sortVal = 5)
	private List<Ability>	grantedAbilities;
	
	@ExportedParam(storetype = SType.LIST, dataType = "ref:statuses", key = "grantedStatuses", sortVal = 6)
	private List<Status>	grantedStatuses;
}
