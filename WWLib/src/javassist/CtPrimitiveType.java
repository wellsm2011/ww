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

/**
 * An instance of <code>CtPrimitiveType</code> represents a primitive type. It
 * is obtained from <code>CtClass</code>.
 */
public final class CtPrimitiveType extends CtClass
{
	private char	descriptor;
	private String	wrapperName;
	private String	getMethodName;
	private String	mDescriptor;
	private int		returnOp;
	private int		arrayType;
	private int		dataSize;

	CtPrimitiveType(String name, char desc, String wrapper, String methodName, String mDesc, int opcode, int atype, int size)
	{
		super(name);
		this.descriptor = desc;
		this.wrapperName = wrapper;
		this.getMethodName = methodName;
		this.mDescriptor = mDesc;
		this.returnOp = opcode;
		this.arrayType = atype;
		this.dataSize = size;
	}

	/**
	 * Returns the array-type code representing the type. It is used for the
	 * newarray instruction. For example, if the type is int, then this method
	 * returns <code>javassit.bytecode.Opcode.T_INT</code>.
	 */
	public int getArrayType()
	{
		return this.arrayType;
	}

	/**
	 * Returns the data size of the primitive type. If the type is long or
	 * double, this method returns 2. Otherwise, it returns 1.
	 */
	public int getDataSize()
	{
		return this.dataSize;
	}

	/**
	 * Returns the descriptor representing this type. For example, if the type
	 * is int, then the descriptor is I.
	 */
	public char getDescriptor()
	{
		return this.descriptor;
	}

	/**
	 * Returns the descriptor of the method for retrieving the value from the
	 * wrapper object. For example, if the type is int, then the method
	 * descriptor is <code>()I</code>.
	 */
	public String getGetMethodDescriptor()
	{
		return this.mDescriptor;
	}

	/**
	 * Returns the name of the method for retrieving the value from the wrapper
	 * object. For example, if the type is int, then the method name is
	 * <code>intValue</code>.
	 */
	public String getGetMethodName()
	{
		return this.getMethodName;
	}

	/**
	 * Returns the modifiers for this type. For decoding, use
	 * <code>javassist.Modifier</code>.
	 *
	 * @see Modifier
	 */
	@Override
	public int getModifiers()
	{
		return Modifier.PUBLIC | Modifier.FINAL;
	}

	/**
	 * Returns the opcode for returning a value of the type. For example, if the
	 * type is int, then the returned opcode is
	 * <code>javassit.bytecode.Opcode.IRETURN</code>.
	 */
	public int getReturnOp()
	{
		return this.returnOp;
	}

	/**
	 * Returns the name of the wrapper class. For example, if the type is int,
	 * then the wrapper class is <code>java.lang.Integer</code>.
	 */
	public String getWrapperName()
	{
		return this.wrapperName;
	}

	/**
	 * Returns <code>true</code> if this object represents a primitive Java
	 * type: boolean, byte, char, short, int, long, float, double, or void.
	 */
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
}
