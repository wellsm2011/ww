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
 * New Expression.
 */
public class NewExpr extends ASTList implements TokenId
{
	public static NewExpr makeObjectArray(ASTList className, ASTList arraySize, ArrayInit init)
	{
		NewExpr e = new NewExpr(className, arraySize);
		e.newArray = true;
		if (init != null)
			ASTList.append(e, init);

		return e;
	}

	protected boolean	newArray;

	protected int		arrayType;

	public NewExpr(ASTList className, ASTList args)
	{
		super(className, new ASTList(args));
		this.newArray = false;
		this.arrayType = TokenId.CLASS;
	}

	public NewExpr(int type, ASTList arraySize, ArrayInit init)
	{
		super(null, new ASTList(arraySize));
		this.newArray = true;
		this.arrayType = type;
		if (init != null)
			ASTList.append(this, init);
	}

	@Override
	public void accept(Visitor v) throws CompileError
	{
		v.atNewExpr(this);
	}

	public ASTList getArguments()
	{
		return (ASTList) this.getRight().getLeft();
	}

	public ASTList getArraySize()
	{
		return this.getArguments();
	}

	/*
	 * TokenId.CLASS, TokenId.INT, ...
	 */
	public int getArrayType()
	{
		return this.arrayType;
	}

	public ASTList getClassName()
	{
		return (ASTList) this.getLeft();
	}

	public ArrayInit getInitializer()
	{
		ASTree t = this.getRight().getRight();
		if (t == null)
			return null;
		else
			return (ArrayInit) t.getLeft();
	}

	@Override
	protected String getTag()
	{
		return this.newArray ? "new[]" : "new";
	}

	public boolean isArray()
	{
		return this.newArray;
	}
}
