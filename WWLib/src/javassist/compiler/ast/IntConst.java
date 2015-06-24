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
 * Integer constant.
 */
public class IntConst extends ASTree
{
	protected long	value;
	protected int	type;

	public IntConst(long v, int tokenId)
	{
		this.value = v;
		this.type = tokenId;
	}

	@Override
	public void accept(Visitor v) throws CompileError
	{
		v.atIntConst(this);
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
		double value1 = this.value;
		double value2 = right.value;
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

		return new DoubleConst(newValue, right.type);
	}

	private IntConst compute0(int op, IntConst right)
	{
		int type1 = this.type;
		int type2 = right.type;
		int newType;
		if (type1 == TokenId.LongConstant || type2 == TokenId.LongConstant)
			newType = TokenId.LongConstant;
		else if (type1 == TokenId.CharConstant && type2 == TokenId.CharConstant)
			newType = TokenId.CharConstant;
		else
			newType = TokenId.IntConstant;

		long value1 = this.value;
		long value2 = right.value;
		long newValue;
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
			case '|':
				newValue = value1 | value2;
				break;
			case '^':
				newValue = value1 ^ value2;
				break;
			case '&':
				newValue = value1 & value2;
				break;
			case TokenId.LSHIFT:
				newValue = this.value << (int) value2;
				newType = type1;
				break;
			case TokenId.RSHIFT:
				newValue = this.value >> (int) value2;
				newType = type1;
				break;
			case TokenId.ARSHIFT:
				newValue = this.value >>> (int) value2;
				newType = type1;
				break;
			default:
				return null;
		}

		return new IntConst(newValue, newType);
	}

	public long get()
	{
		return this.value;
	}

	/*
	 * Returns IntConstant, CharConstant, or LongConstant.
	 */
	public int getType()
	{
		return this.type;
	}

	public void set(long v)
	{
		this.value = v;
	}

	@Override
	public String toString()
	{
		return Long.toString(this.value);
	}
}
