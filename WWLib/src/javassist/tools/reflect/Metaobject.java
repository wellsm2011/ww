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
import java.lang.reflect.Method;

/**
 * A runtime metaobject.
 *
 * <p>
 * A <code>Metaobject</code> is created for every object at the base level. A
 * different reflective object is associated with a different metaobject.
 *
 * <p>
 * The metaobject intercepts method calls on the reflective object at the
 * base-level. To change the behavior of the method calls, a subclass of
 * <code>Metaobject</code> should be defined.
 *
 * <p>
 * To obtain a metaobject, calls <code>_getMetaobject()</code> on a reflective
 * object. For example,
 *
 * <pre>
 * Metaobject	m	= ((Metalevel) reflectiveObject)._getMetaobject();
 * </pre>
 *
 * @see javassist.tools.reflect.ClassMetaobject
 * @see javassist.tools.reflect.Metalevel
 */
public class Metaobject implements Serializable
{
	protected ClassMetaobject	classmetaobject;
	protected Metalevel			baseobject;
	protected Method[]			methods;

	/**
	 * Constructs a <code>Metaobject</code> without initialization. If calling
	 * this constructor, a subclass should be responsible for initialization.
	 */
	protected Metaobject()
	{
		this.baseobject = null;
		this.classmetaobject = null;
		this.methods = null;
	}

	/**
	 * Constructs a <code>Metaobject</code>. The metaobject is constructed
	 * before the constructor is called on the base-level object.
	 *
	 * @param self
	 *            the object that this metaobject is associated with.
	 * @param args
	 *            the parameters passed to the constructor of <code>self</code>.
	 */
	public Metaobject(Object self, Object[] args)
	{
		this.baseobject = (Metalevel) self;
		this.classmetaobject = this.baseobject._getClass();
		this.methods = this.classmetaobject.getReflectiveMethods();
	}

	/**
	 * Obtains the class metaobject associated with this metaobject.
	 *
	 * @see javassist.tools.reflect.ClassMetaobject
	 */
	public final ClassMetaobject getClassMetaobject()
	{
		return this.classmetaobject;
	}

	/**
	 * Returns the name of the method specified by <code>identifier</code>.
	 */
	public final String getMethodName(int identifier)
	{
		String mname = this.methods[identifier].getName();
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
	 * Obtains the object controlled by this metaobject.
	 */
	public final Object getObject()
	{
		return this.baseobject;
	}

	/**
	 * Returns an array of <code>Class</code> objects representing the formal
	 * parameter types of the method specified by <code>identifier</code>.
	 */
	public final Class[] getParameterTypes(int identifier)
	{
		return this.methods[identifier].getParameterTypes();
	}

	/**
	 * Returns a <code>Class</code> objects representing the return type of the
	 * method specified by <code>identifier</code>.
	 */
	public final Class getReturnType(int identifier)
	{
		return this.methods[identifier].getReturnType();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		this.baseobject = (Metalevel) in.readObject();
		this.classmetaobject = this.baseobject._getClass();
		this.methods = this.classmetaobject.getReflectiveMethods();
	}

	/**
	 * Changes the object controlled by this metaobject.
	 *
	 * @param self
	 *            the object
	 */
	public final void setObject(Object self)
	{
		this.baseobject = (Metalevel) self;
		this.classmetaobject = this.baseobject._getClass();
		this.methods = this.classmetaobject.getReflectiveMethods();

		// call _setMetaobject() after the metaobject is settled.
		this.baseobject._setMetaobject(this);
	}

	/**
	 * Is invoked when public fields of the base-level class are read and the
	 * runtime system intercepts it. This method simply returns the value of the
	 * field.
	 *
	 * <p>
	 * Every subclass of this class should redefine this method.
	 */
	public Object trapFieldRead(String name)
	{
		Class jc = this.getClassMetaobject().getJavaClass();
		try
		{
			return jc.getField(name).get(this.getObject());
		} catch (NoSuchFieldException e)
		{
			throw new RuntimeException(e.toString());
		} catch (IllegalAccessException e)
		{
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Is invoked when public fields of the base-level class are modified and
	 * the runtime system intercepts it. This method simply sets the field to
	 * the given value.
	 *
	 * <p>
	 * Every subclass of this class should redefine this method.
	 */
	public void trapFieldWrite(String name, Object value)
	{
		Class jc = this.getClassMetaobject().getJavaClass();
		try
		{
			jc.getField(name).set(this.getObject(), value);
		} catch (NoSuchFieldException e)
		{
			throw new RuntimeException(e.toString());
		} catch (IllegalAccessException e)
		{
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Is invoked when base-level method invocation is intercepted. This method
	 * simply executes the intercepted method invocation with the original
	 * parameters and returns the resulting value.
	 *
	 * <p>
	 * Every subclass of this class should redefine this method.
	 *
	 * <p>
	 * Note: this method is not invoked if the base-level method is invoked by a
	 * constructor in the super class. For example,
	 *
	 * <pre>
	 * abstract class A
	 * {
	 * 	abstract void initialize();
	 * 
	 * 	A()
	 * 	{
	 * 		initialize(); // not intercepted
	 * 	}
	 * }
	 * 
	 * class B extends A
	 * {
	 * 	void initialize()
	 * 	{
	 * 		System.out.println(&quot;initialize()&quot;);
	 * 	}
	 * 
	 * 	B()
	 * 	{
	 * 		super();
	 * 		initialize(); // intercepted
	 * 	}
	 * }
	 * </pre>
	 *
	 * <p>
	 * if an instance of B is created, the invocation of initialize() in B is
	 * intercepted only once. The first invocation by the constructor in A is
	 * not intercepted. This is because the link between a base-level object and
	 * a metaobject is not created until the execution of a constructor of the
	 * super class finishes.
	 */
	public Object trapMethodcall(int identifier, Object[] args) throws Throwable
	{
		try
		{
			return this.methods[identifier].invoke(this.getObject(), args);
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
		out.writeObject(this.baseobject);
	}
}
