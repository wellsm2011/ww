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
import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;

/**
 * Enum constant value.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class EnumMemberValue extends MemberValue
{
	int	typeIndex, valueIndex;

	/**
	 * Constructs an enum constant value. The initial value is not specified.
	 */
	public EnumMemberValue(ConstPool cp)
	{
		super('e', cp);
		this.typeIndex = this.valueIndex = 0;
	}

	/**
	 * Constructs an enum constant value. The initial value is specified by the
	 * constant pool entries at the given indexes.
	 *
	 * @param type
	 *            the index of a CONSTANT_Utf8_info structure representing the
	 *            enum type.
	 * @param value
	 *            the index of a CONSTANT_Utf8_info structure. representing the
	 *            enum value.
	 */
	public EnumMemberValue(int type, int value, ConstPool cp)
	{
		super('e', cp);
		this.typeIndex = type;
		this.valueIndex = value;
	}

	/**
	 * Accepts a visitor.
	 */
	@Override
	public void accept(MemberValueVisitor visitor)
	{
		visitor.visitEnumMemberValue(this);
	}

	/**
	 * Obtains the enum type name.
	 *
	 * @return a fully-qualified type name.
	 */
	public String getType()
	{
		return Descriptor.toClassName(this.cp.getUtf8Info(this.typeIndex));
	}

	@Override
	Class getType(ClassLoader cl) throws ClassNotFoundException
	{
		return MemberValue.loadClass(cl, this.getType());
	}

	/**
	 * Obtains the name of the enum constant value.
	 */
	public String getValue()
	{
		return this.cp.getUtf8Info(this.valueIndex);
	}

	@Override
	Object getValue(ClassLoader cl, ClassPool cp, Method m) throws ClassNotFoundException
	{
		try
		{
			return this.getType(cl).getField(this.getValue()).get(null);
		} catch (NoSuchFieldException e)
		{
			throw new ClassNotFoundException(this.getType() + "." + this.getValue());
		} catch (IllegalAccessException e)
		{
			throw new ClassNotFoundException(this.getType() + "." + this.getValue());
		}
	}

	/**
	 * Changes the enum type name.
	 *
	 * @param typename
	 *            a fully-qualified type name.
	 */
	public void setType(String typename)
	{
		this.typeIndex = this.cp.addUtf8Info(Descriptor.of(typename));
	}

	/**
	 * Changes the name of the enum constant value.
	 */
	public void setValue(String name)
	{
		this.valueIndex = this.cp.addUtf8Info(name);
	}

	@Override
	public String toString()
	{
		return this.getType() + "." + this.getValue();
	}

	/**
	 * Writes the value.
	 */
	@Override
	public void write(AnnotationsWriter writer) throws IOException
	{
		writer.enumConstValue(this.cp.getUtf8Info(this.typeIndex), this.getValue());
	}
}
