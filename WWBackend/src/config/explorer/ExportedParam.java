package config.explorer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// can use in method only.
public @interface ExportedParam
{
	public enum DType
	{
		NUM, STRING, NUMLIST, STRLIST, NUMMAP, STRMAP
	}

	public enum MType
	{
		SETTER, GETTER
	}

	DType datatype() default DType.STRING;

	boolean jsonStored() default true;

	String key() default "";

	MType methodtype() default MType.GETTER;

	int sortVal() default 1000;
}
