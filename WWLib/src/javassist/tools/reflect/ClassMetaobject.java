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

package javassist.tools.reflect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A runtime class metaobject.
 * <p>
 * A <code>ClassMetaobject</code> is created for every class of reflective
 * objects. It can be used to hold values shared among the reflective objects of
 * the same class.
 * <p>
 * To obtain a class metaobject, calls <code>_getClass()</code> on a reflective
 * object. For example,
 *
 * <pre>
 * ClassMetaobject cm = ((Metalevel) reflectiveObject)._getClass();
 * </pre>
 *
 * @see javassist.tools.reflect.Metaobject
 * @see javassist.tools.reflect.Metalevel
 */
public class ClassMetaobject implements Serializable
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	/**
	 * The base-level methods controlled by a metaobject are renamed so that
	 * they begin with <code>methodPrefix "_m_"</code>.
	 */
	static final String			methodPrefix		= "_m_";
	static final int			methodPrefixLen		= 3;

	/**
	 * Specifies how a <code>java.lang.Class</code> object is loaded.
	 * <p>
	 * If true, it is loaded by:
	 *
	 * <pre>
	 * Thread.currentThread().getContextClassLoader().loadClass()
	 * </pre>
	 * <p>
	 * If false, it is loaded by <code>Class.forName()</code>. The default value
	 * is false.
	 */
	public static boolean useContextClassLoader = false;

	/**
	 * Invokes a method whose name begins with <code>methodPrefix "_m_"</code>
	 * and the identifier.
	 *
	 * @exception CannotInvokeException
	 *                if the invocation fails.
	 */
	static public Object invoke(Object target, int identifier, Object[] args) throws Throwable
	{
		Method[] allmethods = target.getClass().getMethods();
		int n = allmethods.length;
		String head = ClassMetaobject.methodPrefix + identifier;
		for (int i = 0; i < n; ++i)
			if (allmethods[i].getName().startsWith(head))
				try
				{
					return allmethods[i].invoke(target, args);
				} catch (java.lang.reflect.InvocationTargetException e)
				{
					throw e.getTargetException();
				} catch (java.lang.IllegalAccessException e)
				{
					throw new CannotInvokeException(e);
				}

		throw new CannotInvokeException("cannot find a method");
	}

	private Class javaClass;

	private Constructor[] constructors;

	private Method[] methods;

	/**
	 * Constructs a <code>ClassMetaobject</code>.
	 *
	 * @param params
	 *            <code>params[0]</code> is the name of the class of the
	 *            reflective objects.
	 */
	public ClassMetaobject(String[] params)
	{
		try
		{
			this.javaClass = this.getClassObject(params[0]);
		} catch (ClassNotFoundException e)
		{
			throw new RuntimeException("not found: " + params[0] + ", useContextClassLoader: " + Boolean.toString(ClassMetaobject.useContextClassLoader), e);
		}

		this.constructors = this.javaClass.getConstructors();
		this.methods = null;
	}

	private Class getClassObject(String name) throws ClassNotFoundException
	{
		if (ClassMetaobject.useContextClassLoader)
			return Thread.currentThread().getContextClassLoader().loadClass(name);
		else
			return Class.forName(name);
	}

	/**
	 * Obtains the <code>java.lang.Class</code> representing this class.
	 */
	public final Class getJavaClass()
	{
		return this.javaClass;
	}

	/**
	 * Returns the <code>java.lang.reflect.Method</code> object representing the
	 * method specified by <code>identifier</code>.
	 * <p>
	 * Note that the actual method returned will be have an altered, reflective
	 * name i.e. <code>_m_2_..</code>.
	 *
	 * @param identifier
	 *            the identifier index given to <code>trapMethodcall()</code>
	 *            etc.
	 * @see Metaobject#trapMethodcall(int,Object[])
	 * @see #trapMethodcall(int,Object[])
	 */
	public final Method getMethod(int identifier)
	{
		return this.getReflectiveMethods()[identifier];
	}

	/**
	 * Returns the identifier index of the method, as identified by its original
	 * name.
	 * <p>
	 * This method is useful, in conjuction with
	 * {@link ClassMetaobject#getMethod(int)}, to obtain a quick reference to
	 * the original method in the reflected class (i.e. not the proxy method),
	 * using the original name of the method.
	 * <p>
	 * Written by Brett Randall and Shigeru Chiba.
	 *
	 * @param originalName
	 *            The original name of the reflected method
	 * @param argTypes
	 *            array of Class specifying the method signature
	 * @return the identifier index of the original method
	 * @throws NoSuchMethodException
	 *             if the method does not exist
	 * @see ClassMetaobject#getMethod(int)
	 */
	public final int getMethodIndex(String originalName, Class[] argTypes) throws NoSuchMethodException
	{
		Method[] mthds = this.getReflectiveMethods();
		for (int i = 0; i < mthds.length; i++)
		{
			if (mthds[i] == null)
				continue;

			// check name and parameter types match
			if (this.getMethodName(i).equals(originalName) && Arrays.equals(argTypes, mthds[i].getParameterTypes()))
				return i;
		}

		throw new NoSuchMethodException("Method " + originalName + " not found");
	}

	/**
	 * Returns the name of the method specified by <code>identifier</code>.
	 */
	public final String getMethodName(int identifier)
	{
		String mname = this.getReflectiveMethods()[identifier].getName();
		int j = ClassMetaobject.methodPrefixLen;
		for (;;)
		{
			char c = mname.charAt(j++);
			if (c < '0' || '9' < c)
				break;
		}

		return mname.substring(j);
	}

	/**
	 * Obtains the name of this class.
	 */
	public final String getName()
	{
		return this.javaClass.getName();
	}

	/**
	 * Returns an array of <code>Class</code> objects representing the formal
	 * parameter types of the method specified by <code>identifier</code>.
	 */
	public final Class[] getParameterTypes(int identifier)
	{
		return this.getReflectiveMethods()[identifier].getParameterTypes();
	}

	/**
	 * Returns an array of the methods defined on the given reflective object.
	 * This method is for the internal use only.
	 */
	public final Method[] getReflectiveMethods()
	{
		if (this.methods != null)
			return this.methods;

		Class baseclass = this.getJavaClass();
		Method[] allmethods = baseclass.getDeclaredMethods();
		int n = allmethods.length;
		int[] index = new int[n];
		int max = 0;
		for (int i = 0; i < n; ++i)
		{
			Method m = allmethods[i];
			String mname = m.getName();
			if (mname.startsWith(ClassMetaobject.methodPrefix))
			{
				int k = 0;
				for (int j = ClassMetaobject.methodPrefixLen;; ++j)
				{
					char c = mname.charAt(j);
					if ('0' <= c && c <= '9')
						k = k * 10 + c - '0';
					else
						break;
				}

				index[i] = ++k;
				if (k > max)
					max = k;
			}
		}

		this.methods = new Method[max];
		for (int i = 0; i < n; ++i)
			if (index[i] > 0)
				this.methods[index[i] - 1] = allmethods[i];

		return this.methods;
	}

	/**
	 * Returns a <code>Class</code> objects representing the return type of the
	 * method specified by <code>identifier</code>.
	 */
	public final Class getReturnType(int identifier)
	{
		return this.getReflectiveMethods()[identifier].getReturnType();
	}

	/**
	 * Returns true if <code>obj</code> is an instance of this class.
	 */
	public final boolean isInstance(Object obj)
	{
		return this.javaClass.isInstance(obj);
	}

	/**
	 * Creates a new instance of the class.
	 *
	 * @param args
	 *            the arguments passed to the constructor.
	 */
	public final Object newInstance(Object[] args) throws CannotCreateException
	{
		int n = this.constructors.length;
		for (int i = 0; i < n; ++i)
			try
			{
				return this.constructors[i].newInstance(args);
			} catch (IllegalArgumentException e)
			{
				// try again
			} catch (InstantiationException e)
			{
				throw new CannotCreateException(e);
			} catch (IllegalAccessException e)
			{
				throw new CannotCreateException(e);
			} catch (InvocationTargetException e)
			{
				throw new CannotCreateException(e);
			}

		throw new CannotCreateException("no constructor matches");
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		this.javaClass = this.getClassObject(in.readUTF());
		this.constructors = this.javaClass.getConstructors();
		this.methods = null;
	}

	/**
	 * Is invoked when <code>static</code> fields of the base-level class are
	 * read and the runtime system intercepts it. This method simply returns the
	 * value of the field.
	 * <p>
	 * Every subclass of this class should redefine this method.
	 */
	public Object trapFieldRead(String name)
	{
		Class jc = this.getJavaClass();
		try
		{
			return jc.getField(name).get(null);
		} catch (NoSuchFieldException e)
		{
			throw new RuntimeException(e.toString());
		} catch (IllegalAccessException e)
		{
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Is invoked when <code>static</code> fields of the base-level class are
	 * modified and the runtime system intercepts it. This method simply sets
	 * the field to the given value.
	 * <p>
	 * Every subclass of this class should redefine this method.
	 */
	public void trapFieldWrite(String name, Object value)
	{
		Class jc = this.getJavaClass();
		try
		{
			jc.getField(name).set(null, value);
		} catch (NoSuchFieldException e)
		{
			throw new RuntimeException(e.toString());
		} catch (IllegalAccessException e)
		{
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Is invoked when <code>static</code> methods of the base-level class are
	 * called and the runtime system intercepts it. This method simply executes
	 * the intercepted method invocation with the original parameters and
	 * returns the resulting value.
	 * <p>
	 * Every subclass of this class should redefine this method.
	 */
	public Object trapMethodcall(int identifier, Object[] args) throws Throwable
	{
		try
		{
			Method[] m = this.getReflectiveMethods();
			return m[identifier].invoke(null, args);
		} catch (java.lang.reflect.InvocationTargetException e)
		{
			throw e.getTargetException();
		} catch (java.lang.IllegalAccessException e)
		{
			throw new CannotInvokeException(e);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeUTF(this.javaClass.getName());
	}
}
