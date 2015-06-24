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
 * Variable declarator.
 */
public class Declarator extends ASTList implements TokenId
{
	public static String astToClassName(ASTList name, char sep)
	{
		if (name == null)
			return null;

		StringBuffer sbuf = new StringBuffer();
		Declarator.astToClassName(sbuf, name, sep);
		return sbuf.toString();
	}

	private static void astToClassName(StringBuffer sbuf, ASTList name, char sep)
	{
		for (;;)
		{
			ASTree h = name.head();
			if (h instanceof Symbol)
				sbuf.append(((Symbol) h).get());
			else if (h instanceof ASTList)
				Declarator.astToClassName(sbuf, (ASTList) h, sep);

			name = name.tail();
			if (name == null)
				break;

			sbuf.append(sep);
		}
	}

	protected int		varType;
	protected int		arrayDim;

	protected int		localVar;

	protected String	qualifiedClass; // JVM-internal representation

	public Declarator(ASTList className, int dim)
	{
		super(null);
		this.varType = TokenId.CLASS;
		this.arrayDim = dim;
		this.localVar = -1;
		this.qualifiedClass = Declarator.astToClassName(className, '/');
	}

	public Declarator(int type, int dim)
	{
		super(null);
		this.varType = type;
		this.arrayDim = dim;
		this.localVar = -1;
		this.qualifiedClass = null;
	}

	/*
	 * For declaring a pre-defined? local variable.
	 */
	public Declarator(int type, String jvmClassName, int dim, int var, Symbol sym)
	{
		super(null);
		this.varType = type;
		this.arrayDim = dim;
		this.localVar = var;
		this.qualifiedClass = jvmClassName;
		this.setLeft(sym);
		ASTList.append(this, null); // initializer
	}

	@Override
	public void accept(Visitor v) throws CompileError
	{
		v.atDeclarator(this);
	}

	public void addArrayDim(int d)
	{
		this.arrayDim += d;
	}

	public int getArrayDim()
	{
		return this.arrayDim;
	}

	public String getClassName()
	{
		return this.qualifiedClass;
	}

	public ASTree getInitializer()
	{
		ASTList t = this.tail();
		if (t != null)
			return t.head();
		else
			return null;
	}

	public int getLocalVar()
	{
		return this.localVar;
	}

	@Override
	public String getTag()
	{
		return "decl";
	}

	/*
	 * Returns CLASS, BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, or DOUBLE
	 * (or VOID)
	 */
	public int getType()
	{
		return this.varType;
	}

	public Symbol getVariable()
	{
		return (Symbol) this.getLeft();
	}

	public Declarator make(Symbol sym, int dim, ASTree init)
	{
		Declarator d = new Declarator(this.varType, this.arrayDim + dim);
		d.qualifiedClass = this.qualifiedClass;
		d.setLeft(sym);
		ASTList.append(d, init);
		return d;
	}

	public void setClassName(String s)
	{
		this.qualifiedClass = s;
	}

	public void setLocalVar(int n)
	{
		this.localVar = n;
	}

	public void setVariable(Symbol sym)
	{
		this.setLeft(sym);
	}
}
