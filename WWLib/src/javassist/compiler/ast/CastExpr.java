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
 * Cast expression.
 */
public class CastExpr extends ASTList implements TokenId
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	protected int				castType;
	protected int				arrayDim;

	public CastExpr(ASTList className, int dim, ASTree expr)
	{
		super(className, new ASTList(expr));
		this.castType = TokenId.CLASS;
		this.arrayDim = dim;
	}

	public CastExpr(int type, int dim, ASTree expr)
	{
		super(null, new ASTList(expr));
		this.castType = type;
		this.arrayDim = dim;
	}

	@Override
	public void accept(Visitor v) throws CompileError
	{
		v.atCastExpr(this);
	}

	public int getArrayDim()
	{
		return this.arrayDim;
	}

	public ASTList getClassName()
	{
		return (ASTList) this.getLeft();
	}

	public ASTree getOprand()
	{
		return this.getRight().getLeft();
	}

	@Override
	public String getTag()
	{
		return "cast:" + this.castType + ":" + this.arrayDim;
	}

	/*
	 * Returns CLASS, BOOLEAN, INT, or ...
	 */
	public int getType()
	{
		return this.castType;
	}

	public void setOprand(ASTree t)
	{
		this.getRight().setLeft(t);
	}
}
