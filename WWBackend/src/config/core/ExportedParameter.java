package config.core;

import java.lang.reflect.Method;
import java.util.Map;

import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;
import backend.U;

public class ExportedParameter
{
	private String				paramName;
	private Map<MType, Method>	methods;
	private SType				datatype;

	public ExportedParameter(String name, Map<MType, Method> methods, SType datatype)
	{
		this.paramName = name;
		this.methods = methods;
		this.datatype = datatype;
	}

	public <T> T call(Object target, MType targMethod, Object... params)
	{
		return U.carefulCall(this.methods.get(targMethod), target, params);
	}

	public SType getDatatype()
	{
		return this.datatype;
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

	@Override
	public String toString()
	{
		return this.paramName + " - [" + this.methods.get(MType.GETTER).getName() + "]";
	}
}
