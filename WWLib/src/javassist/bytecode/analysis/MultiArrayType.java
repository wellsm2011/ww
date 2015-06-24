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
package javassist.bytecode.analysis;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Represents an array of {@link MultiType} instances.
 *
 * @author Jason T. Greene
 */
public class MultiArrayType extends Type
{
	private MultiType	component;
	private int			dims;

	public MultiArrayType(MultiType component, int dims)
	{
		super(null);
		this.component = component;
		this.dims = dims;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof MultiArrayType))
			return false;
		MultiArrayType multi = (MultiArrayType) o;

		return this.component.equals(multi.component) && this.dims == multi.dims;
	}

	@Override
	public Type getComponent()
	{
		return this.dims == 1 ? (Type) this.component : new MultiArrayType(this.component, this.dims - 1);
	}

	@Override
	public CtClass getCtClass()
	{
		CtClass clazz = this.component.getCtClass();
		if (clazz == null)
			return null;

		ClassPool pool = clazz.getClassPool();
		if (pool == null)
			pool = ClassPool.getDefault();

		String name = this.arrayName(clazz.getName(), this.dims);

		try
		{
			return pool.get(name);
		} catch (NotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getDimensions()
	{
		return this.dims;
	}

	@Override
	public int getSize()
	{
		return 1;
	}

	@Override
	public boolean isArray()
	{
		return true;
	}

	@Override
	public boolean isAssignableFrom(Type type)
	{
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean isAssignableTo(Type type)
	{
		if (Type.eq(type.getCtClass(), Type.OBJECT.getCtClass()))
			return true;

		if (Type.eq(type.getCtClass(), Type.CLONEABLE.getCtClass()))
			return true;

		if (Type.eq(type.getCtClass(), Type.SERIALIZABLE.getCtClass()))
			return true;

		if (!type.isArray())
			return false;

		Type typeRoot = this.getRootComponent(type);
		int typeDims = type.getDimensions();

		if (typeDims > this.dims)
			return false;

		if (typeDims < this.dims)
		{
			if (Type.eq(typeRoot.getCtClass(), Type.OBJECT.getCtClass()))
				return true;

			if (Type.eq(typeRoot.getCtClass(), Type.CLONEABLE.getCtClass()))
				return true;

			if (Type.eq(typeRoot.getCtClass(), Type.SERIALIZABLE.getCtClass()))
				return true;

			return false;
		}

		return this.component.isAssignableTo(typeRoot);
	}

	@Override
	public boolean isReference()
	{
		return true;
	}

	@Override
	boolean popChanged()
	{
		return this.component.popChanged();
	}

	@Override
	public String toString()
	{
		// follows the same detailed formating scheme as component
		return this.arrayName(this.component.toString(), this.dims);
	}
}
