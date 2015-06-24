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
 * Expression.
 */
public class Expr extends ASTList implements TokenId
{
	/*
	 * operator must be either of: (unary) +, (unary) -, ++, --, !, ~, ARRAY, .
	 * (dot), MEMBER (static member access). Otherwise, the object should be an
	 * instance of a subclass.
	 */

	public static Expr make(int op, ASTree oprand1)
	{
		return new Expr(op, oprand1);
	}

	public static Expr make(int op, ASTree oprand1, ASTree oprand2)
	{
		return new Expr(op, oprand1, new ASTList(oprand2));
	}

	protected int	operatorId;

	Expr(int op, ASTree _head)
	{
		super(_head);
		this.operatorId = op;
	}

	Expr(int op, ASTree _head, ASTList _tail)
	{
		super(_head, _tail);
		this.operatorId = op;
	}

	@Override
	public void accept(Visitor v) throws CompileError
	{
		v.atExpr(this);
	}

	public String getName()
	{
		int id = this.operatorId;
		if (id < 128)
			return String.valueOf((char) id);
		else if (TokenId.NEQ <= id && id <= TokenId.ARSHIFT_E)
			return TokenId.opNames[id - TokenId.NEQ];
		else if (id == TokenId.INSTANCEOF)
			return "instanceof";
		else
			return String.valueOf(id);
	}

	public int getOperator()
	{
		return this.operatorId;
	}

	@Override
	protected String getTag()
	{
		return "op:" + this.getName();
	}

	public ASTree oprand1()
	{
		return this.getLeft();
	}

	public ASTree oprand2()
	{
		return this.getRight().getLeft();
	}

	public void setOperator(int op)
	{
		this.operatorId = op;
	}

	public void setOprand1(ASTree expr)
	{
		this.setLeft(expr);
	}

	public void setOprand2(ASTree expr)
	{
		this.getRight().setLeft(expr);
	}
}
