package parseables;

import java.util.List;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

@ConfigMember(sectionKey = "roles")
public class Role
{
	private String			access;
	private List<String>	tags;
	private List<String>	chatChannels;
	private List<String>	winCondition;
	private List<String>	grantedAbilities;
	private List<String>	grantedStatuses;

	@ExportedParam(datatype = DType.STR, key = "access", methodtype = MType.GETTER, sortVal = 1)
	public String getAccess()
	{
		return this.access;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "chatChannels", methodtype = MType.GETTER, sortVal = 3)
	public List<String> getChatChannels()
	{
		return this.chatChannels;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "grantedAbilities", methodtype = MType.GETTER, sortVal = 5)
	public List<String> getGrantedAbilityList()
	{
		return this.grantedAbilities;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "grantedStatuses", methodtype = MType.GETTER, sortVal = 6)
	public List<String> getGrantedStatusList()
	{
		return this.grantedStatuses;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "tags", methodtype = MType.GETTER, sortVal = 2)
	public List<String> getTags()
	{
		return this.tags;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "winCondition", methodtype = MType.GETTER, sortVal = 4)
	public List<String> getWinCons()
	{
		return this.winCondition;
	}

	@ExportedParam(datatype = DType.STR, key = "access", methodtype = MType.SETTER, sortVal = 1)
	public void setAccess(String input)
	{
		this.access = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "chatChannels", methodtype = MType.SETTER, sortVal = 3)
	public void setChatChannels(List<String> input)
	{
		this.chatChannels = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "grantedAbilities", methodtype = MType.SETTER, sortVal = 5)
	public void setGrantedAbilityList(List<String> input)
	{
		this.grantedAbilities = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "grantedStatuses", methodtype = MType.SETTER, sortVal = 6)
	public void setGrantedStatusList(List<String> input)
	{
		this.grantedStatuses = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "tags", methodtype = MType.SETTER, sortVal = 2)
	public void setTags(List<String> input)
	{
		this.tags = input;
	}

	@ExportedParam(datatype = DType.STRLIST, key = "winCondition", methodtype = MType.SETTER, sortVal = 4)
	public void setWinConss(List<String> input)
	{
		this.winCondition = input;
	}

}
