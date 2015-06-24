/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 2004 Bill Burke. All Rights Reserved.
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

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.bytecode.ConstPool;

/**
 * Array member.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class ArrayMemberValue extends MemberValue
{
	MemberValue		type;
	MemberValue[]	values;

	/**
	 * Constructs an array. The initial value or type are not specified.
	 */
	public ArrayMemberValue(ConstPool cp)
	{
		super('[', cp);
		this.type = null;
		this.values = null;
	}

	/**
	 * Constructs an array. The initial value is not specified.
	 *
	 * @param t
	 *            the type of the array elements.
	 */
	public ArrayMemberValue(MemberValue t, ConstPool cp)
	{
		super('[', cp);
		this.type = t;
		this.values = null;
	}

	/**
	 * Accepts a visitor.
	 */
	@Override
	public void accept(MemberValueVisitor visitor)
	{
		visitor.visitArrayMemberValue(this);
	}

	/**
	 * Obtains the type of the elements.
	 *
	 * @return null if the type is not specified.
	 */
	public MemberValue getType()
	{
		return this.type;
	}

	@Override
	Class getType(ClassLoader cl) throws ClassNotFoundException
	{
		if (this.type == null)
			throw new ClassNotFoundException("no array type specified");

		Object a = Array.newInstance(this.type.getType(cl), 0);
		return a.getClass();
	}

	/**
	 * Obtains the elements of the array.
	 */
	public MemberValue[] getValue()
	{
		return this.values;
	}

	@Override
	Object getValue(ClassLoader cl, ClassPool cp, Method method) throws ClassNotFoundException
	{
		if (this.values == null)
			throw new ClassNotFoundException("no array elements found: " + method.getName());

		int size = this.values.length;
		Class clazz;
		if (this.type == null)
		{
			clazz = method.getReturnType().getComponentType();
			if (clazz == null || size > 0)
				throw new ClassNotFoundException("broken array type: " + method.getName());
		} else
			clazz = this.type.getType(cl);

		Object a = Array.newInstance(clazz, size);
		for (int i = 0; i < size; i++)
			Array.set(a, i, this.values[i].getValue(cl, cp, method));

		return a;
	}

	/**
	 * Sets the elements of the array.
	 */
	public void setValue(MemberValue[] elements)
	{
		this.values = elements;
		if (elements != null && elements.length > 0)
			this.type = elements[0];
	}

	/**
	 * Obtains the string representation of this object.
	 */
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer("{");
		if (this.values != null)
			for (int i = 0; i < this.values.length; i++)
			{
				buf.append(this.values[i].toString());
				if (i + 1 < this.values.length)
					buf.append(", ");
			}

		buf.append("}");
		return buf.toString();
	}

	/**
	 * Writes the value.
	 */
	@Override
	public void write(AnnotationsWriter writer) throws IOException
	{
		int num = this.values.length;
		writer.arrayValue(num);
		for (int i = 0; i < num; ++i)
			this.values[i].write(writer);
	}
}
