package editor.explorer;

import java.lang.reflect.Method;

import backend.U;

public class ExportedView implements Gettable
{
	private String	paramName;
	private Method	getter;
	private Object	target;

	public ExportedView(String name, Method getter, Object target)
	{
		this.paramName = name;
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
	public String toString()
	{
		return this.paramName + " - [" + this.getter.getName() + ":" + this.getter.getReturnType() + "]";
	}
}
