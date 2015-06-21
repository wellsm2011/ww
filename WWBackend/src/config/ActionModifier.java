package config;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class ActionModifier implements ConfigMember
{
	private double	priority;

	@ExportedParam(datatype = DType.NUM, key = "priority", methodtype = MType.GETTER, sortVal = 1)
	public double getTags()
	{
		return this.priority;
	}

	@ExportedParam(datatype = DType.NUM, key = "priority", methodtype = MType.SETTER, sortVal = 1)
	public void setTags(double input)
	{
		this.priority = input;
	}

}
