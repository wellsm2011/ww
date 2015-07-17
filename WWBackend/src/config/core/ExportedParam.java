package config.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// can use in method only.
/**
 * An annotation for marking up data-type classes for use in JSON config storage
 * and loading, also adds support to being able to edit the memory-resident
 * datastructures with the same functionality.
 *
 * @author Andrew Binns
 */
public @interface ExportedParam
{
	/**
	 * This denotes the number of types that are expected in this param, whether
	 * a list, map, or just a single one.
	 */
	public enum SType
	{
		SINGLE, LIST, MAP
	}

	/**
	 * The method types that are currently expected to be supported.
	 */
	public enum MType
	{
		SETTER, GETTER
	}

	/**
	 * The JSON key for this field.
	 */
	String key();

	/**
	 * The type this particular method is of, defaults to GETTER.
	 */
	MType methodtype();

	/**
	 * The storage type for the currently annotated method, whether a single
	 * val, list, or a map.
	 */
	SType storetype();

	/**
	 * <p>
	 * What type of thing is stored here. Can be a reference to another config
	 * member type, value (passed as string), one option from a list
	 * (Essentially like enumerated types, offers the user to limit the fields
	 * to specific types, similar in functionality to an enum), or encoded
	 * types. Decoders can be registered via the
	 * {@link Config#registerType(String, backend.functionInterfaces.ValDecoder)}
	 * function.
	 * </p>
	 * <p>
	 * Examples:
	 * </p>
	 * <p>
	 * To denote a string: "string" or "str" or "val" or "value" are all
	 * accepted.
	 * </p>
	 * <p>
	 * To denote a reference: "ref:&lt;Classname&gt;" where &lt;Classname&gt; is
	 * the name of the class to load. Can match the fully qualified name
	 * (my.package.Classname) or just classname.
	 * </p>
	 * <p>
	 * to denote a list of options:
	 * "enum:item1,item2,item3,somethingelse,whatever"
	 * </p>
	 * <p>
	 * to use a decoder: "decode:&lt;DecoderName!&gt;" where
	 * &lt;DecoderName!&gt; is the name of the desired decoder. This is case
	 * insensitive.
	 * </p>
	 * 
	 * @see Config#registerType(String,
	 *      backend.functionInterfaces.ValDecoder)
	 */
	String dataType();

	/**
	 * How this field should be sorted in comparasion to the other fields in the
	 * json file, which determines how fields are sorted in the editor.
	 */
	int sortVal();

}
