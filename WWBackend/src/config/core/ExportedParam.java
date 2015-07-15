package config.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// can use in method only.
/**
 * An annotation for marking up data-type classes for use in JSON config storage and loading, also adds support to being able to edit the memory-resident datastructures with the same functionality.
 *
 * @author Andrew Binns
 */
public @interface ExportedParam
{
	/**
	 * //TODO
	 */
	public enum SType
	{
		SINGLE, LIST, MAP// , OBJ
	}
	
	public enum DType
	{
		VAL, REF, ENUM
	}

	/**
	 * The method types that are currently expected to be supported.
	 */
	public enum MType
	{
		SETTER, GETTER
	}

	/**
	 * The storage type for the currently annotated method, whether a single val, list, or a map.
	 */
	SType storetype();

	/**
	 * The JSON key for this field.
	 */
	String key();

	/**
	 * The type this particular method is of, defaults to GETTER.
	 */
	MType methodtype();
	
	/**
	 * What type of thing is stored here
	 * @return
	 */
	DType datatype();

	/**
	 * How this field should be sorted in comparasion to the other fields in the
	 * json file, which determines how fields are sorted in the editor.
	 */
	int sortVal();
	
	/**
	 * Optional, determines what name of type to reference
	 * @return
	 */
	String valRef() default "";
}
