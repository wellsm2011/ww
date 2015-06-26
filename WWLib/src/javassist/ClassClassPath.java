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

import java.io.InputStream;
import java.net.URL;

/**
 * A search-path for obtaining a class file by
 * <code>getResourceAsStream()</code> in <code>java.lang.Class</code>.
 * <p>
 * Try adding a <code>ClassClassPath</code> when a program is running with a
 * user-defined class loader and any class files are not found with the default
 * <code>ClassPool</code>. For example,
 *
 * <pre>
 * ClassPool cp = ClassPool.getDefault();
 * cp.insertClassPath(new ClassClassPath(this.getClass()));
 * </pre>
 *
 * This code snippet permanently adds a <code>ClassClassPath</code> to the
 * default <code>ClassPool</code>. Note that the default <code>ClassPool</code>
 * is a singleton. The added <code>ClassClassPath</code> uses a class object
 * representing the class including the code snippet above.
 *
 * @see ClassPool#insertClassPath(ClassPath)
 * @see ClassPool#appendClassPath(ClassPath)
 * @see LoaderClassPath
 */
public class ClassClassPath implements ClassPath
{
	private Class	thisClass;

	ClassClassPath()
	{
		/*
		 * The value of thisClass was this.getClass() in early versions:
		 * thisClass = this.getClass(); However, this made openClassfile() not
		 * search all the system class paths if javassist.jar is put in
		 * jre/lib/ext/ (with JDK1.4).
		 */
		this(java.lang.Object.class);
	}

	/**
	 * Creates a search path.
	 *
	 * @param c
	 *            the <code>Class</code> object used to obtain a class file.
	 *            <code>getResourceAsStream()</code> is called on this object.
	 */
	public ClassClassPath(Class c)
	{
		this.thisClass = c;
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void close()
	{
	}

	/**
	 * Obtains the URL of the specified class file.
	 *
	 * @return null if the class file could not be found.
	 */
	@Override
	public URL find(String classname)
	{
		String jarname = "/" + classname.replace('.', '/') + ".class";
		return this.thisClass.getResource(jarname);
	}

	/**
	 * Obtains a class file by <code>getResourceAsStream()</code>.
	 */
	@Override
	public InputStream openClassfile(String classname)
	{
		String jarname = "/" + classname.replace('.', '/') + ".class";
		return this.thisClass.getResourceAsStream(jarname);
	}

	@Override
	public String toString()
	{
		return this.thisClass.getName() + ".class";
	}
}
