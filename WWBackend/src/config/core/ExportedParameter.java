package config.core;

import java.lang.reflect.Field;

import backend.U;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;

public class ExportedParameter
{
	private String	paramName;
	private SType	storeType;
	private String	dataType;
	private Field	field;

	public ExportedParameter(ExportedParam paramInfo, Field curField)
	{
		this.paramName = paramInfo.key();
		this.field = curField;
		this.storeType = paramInfo.storetype();
		this.dataType = paramInfo.dataType();
	}

	public <T> T get(Object instance)
	{
		try
		{
			return U.cleanCast(this.field.get(instance));
		} catch (IllegalArgumentException | IllegalAccessException e)
		{
			U.e("Error gettiing value in " + this.field.getName() + " in object " + instance, e);
		}
		return null;
	}

	/**
	 * @see ExportedParam#dataType()
	 * @return
	 */
	public String getDataType()
	{
		return this.dataType;
	}

	public String getParamName()
	{
		return this.paramName;
	}

	public SType getStoreType()
	{
		return this.storeType;
	}

	public <T> void set(Object instance, T input)
	{
		try
		{
			this.field.set(instance, input);
		} catch (IllegalArgumentException | IllegalAccessException e)
		{
			U.e("Error setting value" + input + " in object " + instance, e);
		}
	}

	@Override
	public String toString()
	{
		return this.paramName + " - [" + this.field.getName() + "]";
	}
}
