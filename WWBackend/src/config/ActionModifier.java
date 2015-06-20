package config;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class ActionModifier implements ConfigMember
{
	private Double	priority;

	@ExportedParam(datatype = DType.NUM, key = "priority", methodtype = MType.GETTER, sortVal = 1)
	public Double getTags()
	{
		return this.priority;
	}

	@ExportedParam(datatype = DType.NUM, key = "priority", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(Double input)
	{
		this.priority = input;
	}

}
