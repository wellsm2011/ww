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

package javassist.compiler;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.compiler.ast.ASTList;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.CallExpr;
import javassist.compiler.ast.CastExpr;
import javassist.compiler.ast.Expr;
import javassist.compiler.ast.Member;
import javassist.compiler.ast.Symbol;

/* Type checker accepting extended Java syntax for Javassist.
 */

public class JvstTypeChecker extends TypeChecker
{
	private JvstCodeGen codeGen;

	public JvstTypeChecker(CtClass cc, ClassPool cp, JvstCodeGen gen)
	{
		super(cc, cp);
		this.codeGen = gen;
	}

	/*
	 * If the type of the expression compiled last is void, add ACONST_NULL and
	 * change exprType, arrayDim, className.
	 */
	public void addNullIfVoid()
	{
		if (this.exprType == TokenId.VOID)
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = TypeChecker.jvmJavaLangObject;
		}
	}

	/*
	 * Delegates to a ProcHandler object if the method call is $proceed(). It
	 * may process $cflow().
	 */
	@Override
	public void atCallExpr(CallExpr expr) throws CompileError
	{
		ASTree method = expr.oprand1();
		if (method instanceof Member)
		{
			String name = ((Member) method).get();
			if (this.codeGen.procHandler != null && name.equals(this.codeGen.proceedName))
			{
				this.codeGen.procHandler.setReturnType(this, (ASTList) expr.oprand2());
				return;
			} else if (name.equals(JvstCodeGen.cflowName))
			{
				this.atCflow((ASTList) expr.oprand2());
				return;
			}
		}

		super.atCallExpr(expr);
	}

	@Override
	public void atCastExpr(CastExpr expr) throws CompileError
	{
		ASTList classname = expr.getClassName();
		if (classname != null && expr.getArrayDim() == 0)
		{
			ASTree p = classname.head();
			if (p instanceof Symbol && classname.tail() == null)
			{
				String typename = ((Symbol) p).get();
				if (typename.equals(this.codeGen.returnCastName))
				{
					this.atCastToRtype(expr);
					return;
				} else if (typename.equals(JvstCodeGen.wrapperCastName))
				{
					this.atCastToWrapper(expr);
					return;
				}
			}
		}

		super.atCastExpr(expr);
	}

	/**
	 * Inserts a cast operator to the return type. If the return type is void,
	 * this does nothing.
	 */
	protected void atCastToRtype(CastExpr expr) throws CompileError
	{
		CtClass returnType = this.codeGen.returnType;
		expr.getOprand().accept(this);
		if (this.exprType == TokenId.VOID || CodeGen.isRefType(this.exprType) || this.arrayDim > 0)
			this.compileUnwrapValue(returnType);
		else if (returnType instanceof CtPrimitiveType)
		{
			CtPrimitiveType pt = (CtPrimitiveType) returnType;
			int destType = MemberResolver.descToType(pt.getDescriptor());
			this.exprType = destType;
			this.arrayDim = 0;
			this.className = null;
		}
	}

	protected void atCastToWrapper(CastExpr expr) throws CompileError
	{
		expr.getOprand().accept(this);
		if (CodeGen.isRefType(this.exprType) || this.arrayDim > 0)
			return; // Object type. do nothing.

		CtClass clazz = this.resolver.lookupClass(this.exprType, this.arrayDim, this.className);
		if (clazz instanceof CtPrimitiveType)
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = TypeChecker.jvmJavaLangObject;
		}
	}

	/*
	 * To support $cflow().
	 */
	protected void atCflow(ASTList cname) throws CompileError
	{
		this.exprType = TokenId.INT;
		this.arrayDim = 0;
		this.className = null;
	}

	@Override
	protected void atFieldAssign(Expr expr, int op, ASTree left, ASTree right) throws CompileError
	{
		if (left instanceof Member && ((Member) left).get().equals(this.codeGen.paramArrayName))
		{
			right.accept(this);
			CtClass[] params = this.codeGen.paramTypeList;
			if (params == null)
				return;

			int n = params.length;
			for (int i = 0; i < n; ++i)
				this.compileUnwrapValue(params[i]);
		} else
			super.atFieldAssign(expr, op, left, right);
	}

	/*
	 * To support $args, $sig, and $type. $args is an array of parameter list.
	 */
	@Override
	public void atMember(Member mem) throws CompileError
	{
		String name = mem.get();
		if (name.equals(this.codeGen.paramArrayName))
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = 1;
			this.className = TypeChecker.jvmJavaLangObject;
		} else if (name.equals(JvstCodeGen.sigName))
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = 1;
			this.className = "java/lang/Class";
		} else if (name.equals(JvstCodeGen.dollarTypeName) || name.equals(JvstCodeGen.clazzName))
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = "java/lang/Class";
		} else
			super.atMember(mem);
	}

	@Override
	public void atMethodArgs(ASTList args, int[] types, int[] dims, String[] cnames) throws CompileError
	{
		CtClass[] params = this.codeGen.paramTypeList;
		String pname = this.codeGen.paramListName;
		int i = 0;
		while (args != null)
		{
			ASTree a = args.head();
			if (a instanceof Member && ((Member) a).get().equals(pname))
			{
				if (params != null)
				{
					int n = params.length;
					for (int k = 0; k < n; ++k)
					{
						CtClass p = params[k];
						this.setType(p);
						types[i] = this.exprType;
						dims[i] = this.arrayDim;
						cnames[i] = this.className;
						++i;
					}
				}
			} else
			{
				a.accept(this);
				types[i] = this.exprType;
				dims[i] = this.arrayDim;
				cnames[i] = this.className;
				++i;
			}

			args = args.tail();
		}
	}

	/*
	 * called by Javac#recordSpecialProceed().
	 */
	void compileInvokeSpecial(ASTree target, String classname, String methodname, String descriptor, ASTList args) throws CompileError
	{
		target.accept(this);
		int nargs = this.getMethodArgsLength(args);
		this.atMethodArgs(args, new int[nargs], new int[nargs], new String[nargs]);
		this.setReturnType(descriptor);
		this.addNullIfVoid();
	}

	protected void compileUnwrapValue(CtClass type) throws CompileError
	{
		if (type == CtClass.voidType)
			this.addNullIfVoid();
		else
			this.setType(type);
	}

	@Override
	public int getMethodArgsLength(ASTList args)
	{
		String pname = this.codeGen.paramListName;
		int n = 0;
		while (args != null)
		{
			ASTree a = args.head();
			if (a instanceof Member && ((Member) a).get().equals(pname))
			{
				if (this.codeGen.paramTypeList != null)
					n += this.codeGen.paramTypeList.length;
			} else
				++n;

			args = args.tail();
		}

		return n;
	}

	/*
	 * To support $$. ($$) is equivalent to ($1, ..., $n). It can be used only
	 * as a parameter list of method call.
	 */
	public boolean isParamListName(ASTList args)
	{
		if (this.codeGen.paramTypeList != null && args != null && args.tail() == null)
		{
			ASTree left = args.head();
			return left instanceof Member && ((Member) left).get().equals(this.codeGen.paramListName);
		} else
			return false;
	}

	/*
	 * Sets exprType, arrayDim, and className; If type is void, then this method
	 * does nothing.
	 */
	public void setType(CtClass type) throws CompileError
	{
		this.setType(type, 0);
	}

	private void setType(CtClass type, int dim) throws CompileError
	{
		if (type.isPrimitive())
		{
			CtPrimitiveType pt = (CtPrimitiveType) type;
			this.exprType = MemberResolver.descToType(pt.getDescriptor());
			this.arrayDim = dim;
			this.className = null;
		} else if (type.isArray())
			try
			{
				this.setType(type.getComponentType(), dim + 1);
			} catch (NotFoundException e)
			{
				throw new CompileError("undefined type: " + type.getName());
			}
		else
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = dim;
			this.className = MemberResolver.javaToJvmName(type.getName());
		}
	}
}
