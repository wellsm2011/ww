package config.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Simple marker annotation, marks a given class to be used when parseing from
 * the JSON config file. Stores the particular key to store elements of this
 * type under in the config file.
 * 
 * @author Andrew Binns
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigMember
{
	String sectionKey() default "";
}
