package editor.explorer;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import backend.U;
import config.explorer.ExportedParam.MType;

public class ExportedParameter
{
	private String							paramName;
	private Object							target;
	private LinkedHashMap<MType, Method>	methods;

	public ExportedParameter(String name, Object target, LinkedHashMap<MType, Method> methods)
	{
		this.paramName = name;
		this.methods = methods;
		this.target = target;
	}

	public <T> T call(MType targMethod, Object... params)
	{
		return U.carefulCall(this.methods.get(targMethod), this.target, params);
	}

	@Override
	public String toString()
	{
		return this.paramName + " - [" + this.methods + "]";
	}
}
