package editor.backend;

import java.lang.reflect.Method;

import backend.U;

public class ExportedOption
{
	private Method	getter;
	private Method	setter;
	private String	paramName;
	private Object	target;

	public ExportedOption(String name, Method setter, Method getter, Object target)
	{
		this.paramName = name;
		this.setter = setter;
		this.getter = getter;
		this.target = target;
	}

	public <T> T get()
	{
		return U.carefulCall(this.getter, this.target);
	}

	public void set(Object... input)
	{
		U.carefulCall(this.setter, this.target, input);
	}

	@Override
	public String toString()
	{
		return this.paramName + " - [" + this.setter.getName() + " " + this.getter.getName() + "]";
	}
}
