package config.explorer;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import backend.U;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

public class ExportedParameter
{
	private String							paramName;
	private Object							target;
	private LinkedHashMap<MType, Method>	methods;
	private DType							datatype;

	public ExportedParameter(String name, Object target, LinkedHashMap<MType, Method> methods, DType datatype)
	{
		this.paramName = name;
		this.methods = methods;
		this.target = target;
		this.datatype = datatype;
	}

	public <T> T call(MType targMethod, Object... params)
	{
		return U.carefulCall(this.methods.get(targMethod), this.target, params);
	}

	public DType getDatatype()
	{
		return this.datatype;
	}

	public String getGettableVal()
	{
		if (this.methods.containsKey(MType.GETTER))
			return "[" + this.call(MType.GETTER) + "]";
		return "[]";
	}

	public String getParamName()
	{
		return this.paramName;
	}

	@Override
	public String toString()
	{
		return this.paramName + " - [" + this.call(MType.GETTER) + "]";
	}
}
