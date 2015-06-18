package editor.explorer;

import java.lang.reflect.Method;

import backend.U;

public class ExportedOption implements Gettable, Settable
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

	@Override
	public <T> T get()
	{
		if (this.getter != null)
			return U.carefulCall(this.getter, this.target);
		else
			return null;
	}

	@Override
	public void set(Object... input)
	{
		U.carefulCall(this.setter, this.target, input);
	}

	@Override
	public String toString()
	{
		return this.paramName + " - [" + this.setter.getName() + " " + this.getter.getName() + ":" + this.getter.getReturnType() + "]";
	}
}
