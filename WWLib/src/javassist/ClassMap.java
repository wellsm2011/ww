/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist;

import javassist.bytecode.Descriptor;

/**
 * A hash table associating class names with different names.
 * <p>
 * This hashtable is used for replacing class names in a class definition or a
 * method body. Define a subclass of this class if a more complex mapping
 * algorithm is needed. For example,
 *
 * <pre>
 * class MyClassMap extends ClassMap
 * {
 * 	public Object get(Object jvmClassName)
 * 	{
 * 		String name = toJavaName((String) jvmClassName);
 * 		if (name.startsWith(&quot;java.&quot;))
 * 			return toJvmName(&quot;java2.&quot; + name.substring(5));
 * 		else
 * 			return super.get(jvmClassName);
 * 	}
 * }
 * </pre>
 * <p>
 * This subclass maps <code>java.lang.String</code> to
 * <code>java2.lang.String</code>. Note that <code>get()</code> receives and
 * returns the internal representation of a class name. For example, the
 * internal representation of <code>java.lang.String</code> is
 * <code>java/lang/String</code>.
 * <p>
 * Note that this is a map from <code>String</code> to <code>String</code>.
 *
 * @see #get(Object)
 * @see CtClass#replaceClassName(ClassMap)
 * @see CtNewMethod#copy(CtMethod,String,CtClass,ClassMap)
 */
public class ClassMap extends java.util.HashMap
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * Converts a class name from the internal representation used in the JVM to
	 * the normal one used in Java.
	 */
	public static String toJavaName(String classname)
	{
		return Descriptor.toJavaName(classname);
	}

	/**
	 * Converts a class name into the internal representation used in the JVM.
	 */
	public static String toJvmName(String classname)
	{
		return Descriptor.toJvmName(classname);
	}

	private ClassMap	parent;

	/**
	 * Constructs a hash table.
	 */
	public ClassMap()
	{
		this.parent = null;
	}

	ClassMap(ClassMap map)
	{
		this.parent = map;
	}

	/**
	 * Prevents a mapping from the specified class name to another name.
	 */
	public void fix(CtClass clazz)
	{
		this.fix(clazz.getName());
	}

	/**
	 * Prevents a mapping from the specified class name to another name.
	 */
	public void fix(String name)
	{
		String name2 = ClassMap.toJvmName(name);
		super.put(name2, name2);
	}

	/**
	 * Returns the class name to wihch the given <code>jvmClassName</code> is
	 * mapped. A subclass of this class should override this method.
	 * <p>
	 * This method receives and returns the internal representation of class
	 * name used in the JVM.
	 *
	 * @see #toJvmName(String)
	 * @see #toJavaName(String)
	 */
	@Override
	public Object get(Object jvmClassName)
	{
		Object found = super.get(jvmClassName);
		if (found == null && this.parent != null)
			return this.parent.get(jvmClassName);
		else
			return found;
	}

	/**
	 * Maps a class name to another name in this hashtable. The names are
	 * obtained with calling <code>Class.getName()</code>. This method
	 * translates the given class names into the internal form used in the JVM
	 * before putting it in the hashtable.
	 *
	 * @param oldname
	 *            the original class name
	 * @param newname
	 *            the substituted class name.
	 */
	public void put(CtClass oldname, CtClass newname)
	{
		this.put(oldname.getName(), newname.getName());
	}

	/**
	 * Maps a class name to another name in this hashtable. If the hashtable
	 * contains another mapping from the same class name, the old mapping is
	 * replaced. This method translates the given class names into the internal
	 * form used in the JVM before putting it in the hashtable.
	 * <p>
	 * If <code>oldname</code> is identical to <code>newname</code>, then this
	 * method does not perform anything; it does not record the mapping from
	 * <code>oldname</code> to <code>newname</code>. See <code>fix</code>
	 * method.
	 *
	 * @param oldname
	 *            the original class name.
	 * @param newname
	 *            the substituted class name.
	 * @see #fix(String)
	 */
	public void put(String oldname, String newname)
	{
		if (oldname == newname)
			return;

		String oldname2 = ClassMap.toJvmName(oldname);
		String s = (String) this.get(oldname2);
		if (s == null || !s.equals(oldname2))
			super.put(oldname2, ClassMap.toJvmName(newname));
	}

	protected final void put0(Object oldname, Object newname)
	{
		super.put(oldname, newname);
	}

	/**
	 * Is equivalent to <code>put()</code> except that the given mapping is not
	 * recorded into the hashtable if another mapping from <code>oldname</code>
	 * is already included.
	 *
	 * @param oldname
	 *            the original class name.
	 * @param newname
	 *            the substituted class name.
	 */
	public void putIfNone(String oldname, String newname)
	{
		if (oldname == newname)
			return;

		String oldname2 = ClassMap.toJvmName(oldname);
		String s = (String) this.get(oldname2);
		if (s == null)
			super.put(oldname2, ClassMap.toJvmName(newname));
	}
}
