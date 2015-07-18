package config.core;

import java.lang.reflect.Method;
import java.util.Map;

import backend.U;
import config.core.ExportedParam.MType;
import config.core.ExportedParam.SType;

public class ExportedParameter
{
	private String				paramName;
	private Map<MType, Method>	methods;
	private SType				storeType;
	private String				dataType;

	public ExportedParameter(String name, Map<MType, Method> methods, ExportedParam paramInfo)
	{
		this.paramName = name;
		this.methods = methods;
		this.storeType = paramInfo.storetype();
		this.dataType = paramInfo.dataType();
	}

	public <T> T call(Object target, MType targMethod, Object... params)
	{
		return U.carefulCall(this.methods.get(targMethod), target, params);
	}

	public String getDataType()
	{
		return this.dataType;
	}

	public String getGettableAsString(Object input)
	{
		if (this.methods.containsKey(MType.GETTER))
			return this.call(input, MType.GETTER).toString();
		return "";
	}

	public String getParamName()
	{
		return this.paramName;
	}

	public SType getStoreType()
	{
		return this.storeType;
	}

	@Override
	public String toString()
	{
		return this.paramName + " - [" + this.methods.get(MType.GETTER).getName() + "]";
	}
}
