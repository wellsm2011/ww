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

/**
 * Nested annotation.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 */
public class AnnotationMemberValue extends MemberValue
{
	Annotation value;

	/**
	 * Constructs an annotation member. The initial value is specified by the
	 * first parameter.
	 */
	public AnnotationMemberValue(Annotation a, ConstPool cp)
	{
		super('@', cp);
		this.value = a;
	}

	/**
	 * Constructs an annotation member. The initial value is not specified.
	 */
	public AnnotationMemberValue(ConstPool cp)
	{
		this(null, cp);
	}

	/**
	 * Accepts a visitor.
	 */
	@Override
	public void accept(MemberValueVisitor visitor)
	{
		visitor.visitAnnotationMemberValue(this);
	}

	@Override
	Class getType(ClassLoader cl) throws ClassNotFoundException
	{
		if (this.value == null)
			throw new ClassNotFoundException("no type specified");
		else
			return MemberValue.loadClass(cl, this.value.getTypeName());
	}

	/**
	 * Obtains the value.
	 */
	public Annotation getValue()
	{
		return this.value;
	}

	@Override
	Object getValue(ClassLoader cl, ClassPool cp, Method m) throws ClassNotFoundException
	{
		return AnnotationImpl.make(cl, this.getType(cl), cp, this.value);
	}

	/**
	 * Sets the value of this member.
	 */
	public void setValue(Annotation newValue)
	{
		this.value = newValue;
	}

	/**
	 * Obtains the string representation of this object.
	 */
	@Override
	public String toString()
	{
		return this.value.toString();
	}

	/**
	 * Writes the value.
	 */
	@Override
	public void write(AnnotationsWriter writer) throws IOException
	{
		writer.annotationValue();
		this.value.write(writer);
	}
}
