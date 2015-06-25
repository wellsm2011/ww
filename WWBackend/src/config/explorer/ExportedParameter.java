package config.explorer;

import java.lang.reflect.Method;
import java.util.Map;

import backend.U;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class ExportedParameter
{
	private String				paramName;
	private Map<MType, Method>	methods;
	private DType				datatype;

	public ExportedParameter(String name, Map<MType, Method> methods, DType datatype)
	{
		this.paramName = name;
		this.methods = methods;
		this.datatype = datatype;
	}

	public <T> T call(Object target, MType targMethod, Object... params)
	{
		return U.carefulCall(this.methods.get(targMethod), target, params);
	}

	public DType getDatatype()
	{
		return this.datatype;
	}

	public String getGettableVal(Object input)
	{
		if (this.methods.containsKey(MType.GETTER))
			return "[" + this.call(input, MType.GETTER) + "]";
		return "[]";
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
