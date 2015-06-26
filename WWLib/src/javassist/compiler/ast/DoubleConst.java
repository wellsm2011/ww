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

package javassist.compiler.ast;

import javassist.compiler.CompileError;
import javassist.compiler.TokenId;

/**
 * Double constant.
 */
public class DoubleConst extends ASTree
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private static DoubleConst compute(int op, double value1, double value2, int newType)
	{
		double newValue;
		switch (op)
		{
			case '+':
				newValue = value1 + value2;
				break;
			case '-':
				newValue = value1 - value2;
				break;
			case '*':
				newValue = value1 * value2;
				break;
			case '/':
				newValue = value1 / value2;
				break;
			case '%':
				newValue = value1 % value2;
				break;
			default:
				return null;
		}

		return new DoubleConst(newValue, newType);
	}

	protected double	value;

	protected int		type;

	public DoubleConst(double v, int tokenId)
	{
		this.value = v;
		this.type = tokenId;
	}

	@Override
	public void accept(Visitor v) throws CompileError
	{
		v.atDoubleConst(this);
	}

	public ASTree compute(int op, ASTree right)
	{
		if (right instanceof IntConst)
			return this.compute0(op, (IntConst) right);
		else if (right instanceof DoubleConst)
			return this.compute0(op, (DoubleConst) right);
		else
			return null;
	}

	private DoubleConst compute0(int op, DoubleConst right)
	{
		int newType;
		if (this.type == TokenId.DoubleConstant || right.type == TokenId.DoubleConstant)
			newType = TokenId.DoubleConstant;
		else
			newType = TokenId.FloatConstant;

		return DoubleConst.compute(op, this.value, right.value, newType);
	}

	private DoubleConst compute0(int op, IntConst right)
	{
		return DoubleConst.compute(op, this.value, right.value, this.type);
	}

	public double get()
	{
		return this.value;
	}

	/*
	 * Returns DoubleConstant or FloatConstant
	 */
	public int getType()
	{
		return this.type;
	}

	public void set(double v)
	{
		this.value = v;
	}

	@Override
	public String toString()
	{
		return Double.toString(this.value);
	}
}
