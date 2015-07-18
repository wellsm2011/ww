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

package javassist.bytecode.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationDefaultAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

/**
 * Internal-use only. This is a helper class internally used for implementing
 * <code>toAnnotationType()</code> in <code>Annotation</code>.
 *
 * @author Shigeru Chiba
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 */
public class AnnotationImpl implements InvocationHandler
{
	private static final String	JDK_ANNOTATION_CLASS_NAME	= "java.lang.annotation.Annotation";
	private static Method		JDK_ANNOTATION_TYPE_METHOD	= null;

	static
	{
		// Try to resolve the JDK annotation type method
		try
		{
			Class clazz = Class.forName(AnnotationImpl.JDK_ANNOTATION_CLASS_NAME);
			AnnotationImpl.JDK_ANNOTATION_TYPE_METHOD = clazz.getMethod("annotationType", (Class[]) null);
		} catch (Exception ignored)
		{
			// Probably not JDK5+
		}
	}

	/**
	 * Calculates the hashCode of an array using the same algorithm as
	 * java.util.Arrays.hashCode()
	 *
	 * @param object
	 *            the object
	 * @return the hashCode
	 */
	private static int arrayHashCode(Object object)
	{
		if (object == null)
			return 0;

		int result = 1;

		Object[] array = (Object[]) object;
		for (int i = 0; i < array.length; ++i)
		{
			int elementHashCode = 0;
			if (array[i] != null)
				elementHashCode = array[i].hashCode();
			result = 31 * result + elementHashCode;
		}
		return result;
	}

	/**
	 * Constructs an annotation object.
	 *
	 * @param cl
	 *            class loader for obtaining annotation types.
	 * @param clazz
	 *            the annotation type.
	 * @param cp
	 *            class pool for containing an annotation type (or null).
	 * @param anon
	 *            the annotation.
	 * @return the annotation
	 */
	public static Object make(ClassLoader cl, Class clazz, ClassPool cp, Annotation anon)
	{
		AnnotationImpl handler = new AnnotationImpl(anon, cp, cl);
		return Proxy.newProxyInstance(cl, new Class[]
		{ clazz }, handler);
	}

	private Annotation	annotation;
	private ClassPool	pool;

	private ClassLoader classLoader;

	private transient Class annotationType;

	private transient int cachedHashCode = Integer.MIN_VALUE;

	private AnnotationImpl(Annotation a, ClassPool cp, ClassLoader loader)
	{
		this.annotation = a;
		this.pool = cp;
		this.classLoader = loader;
	}

	/**
	 * Check that another annotation equals ourselves.
	 *
	 * @param obj
	 *            the other annotation
	 * @return the true when equals false otherwise
	 * @throws Exception
	 *             for any problem
	 */
	private boolean checkEquals(Object obj) throws Exception
	{
		if (obj == null)
			return false;

		// Optimization when the other is one of ourselves
		if (obj instanceof Proxy)
		{
			InvocationHandler ih = Proxy.getInvocationHandler(obj);
			if (ih instanceof AnnotationImpl)
			{
				AnnotationImpl other = (AnnotationImpl) ih;
				return this.annotation.equals(other.annotation);
			}
		}

		Class otherAnnotationType = (Class) AnnotationImpl.JDK_ANNOTATION_TYPE_METHOD.invoke(obj, (Object[]) null);
		if (this.getAnnotationType().equals(otherAnnotationType) == false)
			return false;

		Method[] methods = this.annotationType.getDeclaredMethods();
		for (int i = 0; i < methods.length; ++i)
		{
			String name = methods[i].getName();

			// Get the value
			MemberValue mv = this.annotation.getMemberValue(name);
			Object value = null;
			Object otherValue = null;
			try
			{
				if (mv != null)
					value = mv.getValue(this.classLoader, this.pool, methods[i]);
				if (value == null)
					value = this.getDefault(name, methods[i]);
				otherValue = methods[i].invoke(obj, (Object[]) null);
			} catch (RuntimeException e)
			{
				throw e;
			} catch (Exception e)
			{
				throw new RuntimeException("Error retrieving value " + name + " for annotation " + this.annotation.getTypeName(), e);
			}

			if (value == null && otherValue != null)
				return false;
			if (value != null && value.equals(otherValue) == false)
				return false;
		}

		return true;
	}

	/**
	 * Obtains the internal data structure representing the annotation.
	 *
	 * @return the annotation
	 */
	public Annotation getAnnotation()
	{
		return this.annotation;
	}

	/**
	 * Get the annotation type
	 *
	 * @return the annotation class
	 * @throws NoClassDefFoundError
	 *             when the class could not loaded
	 */
	private Class getAnnotationType()
	{
		if (this.annotationType == null)
		{
			String typeName = this.annotation.getTypeName();
			try
			{
				this.annotationType = this.classLoader.loadClass(typeName);
			} catch (ClassNotFoundException e)
			{
				NoClassDefFoundError error = new NoClassDefFoundError("Error loading annotation class: " + typeName);
				error.setStackTrace(e.getStackTrace());
				throw error;
			}
		}
		return this.annotationType;
	}

	private Object getDefault(String name, Method method) throws ClassNotFoundException, RuntimeException
	{
		String classname = this.annotation.getTypeName();
		if (this.pool != null)
			try
			{
				CtClass cc = this.pool.get(classname);
				ClassFile cf = cc.getClassFile2();
				MethodInfo minfo = cf.getMethod(name);
				if (minfo != null)
				{
					AnnotationDefaultAttribute ainfo = (AnnotationDefaultAttribute) minfo.getAttribute(AnnotationDefaultAttribute.tag);
					if (ainfo != null)
					{
						MemberValue mv = ainfo.getDefaultValue();
						return mv.getValue(this.classLoader, this.pool, method);
					}
				}
			} catch (NotFoundException e)
			{
				throw new RuntimeException("cannot find a class file: " + classname);
			}

		throw new RuntimeException("no default value: " + classname + "." + name + "()");
	}

	/**
	 * Obtains the name of the annotation type.
	 *
	 * @return the type name
	 */
	public String getTypeName()
	{
		return this.annotation.getTypeName();
	}

	/**
	 * Returns a hash code value for this object.
	 */
	@Override
	public int hashCode()
	{
		if (this.cachedHashCode == Integer.MIN_VALUE)
		{
			int hashCode = 0;

			// Load the annotation class
			this.getAnnotationType();

			Method[] methods = this.annotationType.getDeclaredMethods();
			for (int i = 0; i < methods.length; ++i)
			{
				String name = methods[i].getName();
				int valueHashCode = 0;

				// Get the value
				MemberValue mv = this.annotation.getMemberValue(name);
				Object value = null;
				try
				{
					if (mv != null)
						value = mv.getValue(this.classLoader, this.pool, methods[i]);
					if (value == null)
						value = this.getDefault(name, methods[i]);
				} catch (RuntimeException e)
				{
					throw e;
				} catch (Exception e)
				{
					throw new RuntimeException("Error retrieving value " + name + " for annotation " + this.annotation.getTypeName(), e);
				}

				// Calculate the hash code
				if (value != null)
					if (value.getClass().isArray())
						valueHashCode = AnnotationImpl.arrayHashCode(value);
					else
						valueHashCode = value.hashCode();
				hashCode += 127 * name.hashCode() ^ valueHashCode;
			}

			this.cachedHashCode = hashCode;
		}
		return this.cachedHashCode;
	}

	/**
	 * Executes a method invocation on a proxy instance. The implementations of
	 * <code>toString()</code>, <code>equals()</code>, and
	 * <code>hashCode()</code> are directly supplied by the
	 * <code>AnnotationImpl</code>. The <code>annotationType()</code> method is
	 * also available on the proxy instance.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		String name = method.getName();
		if (Object.class == method.getDeclaringClass())
		{
			if ("equals".equals(name))
			{
				Object obj = args[0];
				return new Boolean(this.checkEquals(obj));
			} else if ("toString".equals(name))
				return this.annotation.toString();
			else if ("hashCode".equals(name))
				return new Integer(this.hashCode());
		} else if ("annotationType".equals(name) && method.getParameterTypes().length == 0)
			return this.getAnnotationType();

		MemberValue mv = this.annotation.getMemberValue(name);
		if (mv == null)
			return this.getDefault(name, method);
		else
			return mv.getValue(this.classLoader, this.pool, method);
	}
}
