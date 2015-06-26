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

package javassist.expr;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.JvstTypeChecker;
import javassist.compiler.ProceedHandler;
import javassist.compiler.ast.ASTList;

/**
 * Instanceof operator.
 */
public class Instanceof extends Expr
{
	/*
	 * boolean $proceed(Object obj)
	 */
	static class ProceedForInstanceof implements ProceedHandler
	{
		int	index;

		ProceedForInstanceof(int i)
		{
			this.index = i;
		}

		@Override
		public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args) throws CompileError
		{
			if (gen.getMethodArgsLength(args) != 1)
				throw new CompileError(Javac.proceedName + "() cannot take more than one parameter " + "for instanceof");

			gen.atMethodArgs(args, new int[1], new int[1], new String[1]);
			bytecode.addOpcode(Opcode.INSTANCEOF);
			bytecode.addIndex(this.index);
			gen.setType(CtClass.booleanType);
		}

		@Override
		public void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError
		{
			c.atMethodArgs(args, new int[1], new int[1], new String[1]);
			c.setType(CtClass.booleanType);
		}
	}

	/**
	 * Undocumented constructor. Do not use; internal-use only.
	 */
	protected Instanceof(int pos, CodeIterator i, CtClass declaring, MethodInfo m)
	{
		super(pos, i, declaring, m);
	}

	/**
	 * Returns the source file containing the instanceof expression.
	 *
	 * @return null if this information is not available.
	 */
	@Override
	public String getFileName()
	{
		return super.getFileName();
	}

	/**
	 * Returns the line number of the source line containing the instanceof
	 * expression.
	 *
	 * @return -1 if this information is not available.
	 */
	@Override
	public int getLineNumber()
	{
		return super.getLineNumber();
	}

	/**
	 * Returns the <code>CtClass</code> object representing the type name on the
	 * right hand side of the instanceof operator.
	 */
	public CtClass getType() throws NotFoundException
	{
		ConstPool cp = this.getConstPool();
		int pos = this.currentPos;
		int index = this.iterator.u16bitAt(pos + 1);
		String name = cp.getClassInfo(index);
		return this.thisClass.getClassPool().getCtClass(name);
	}

	/**
	 * Returns the list of exceptions that the expression may throw. This list
	 * includes both the exceptions that the try-catch statements including the
	 * expression can catch and the exceptions that the throws declaration
	 * allows the method to throw.
	 */
	@Override
	public CtClass[] mayThrow()
	{
		return super.mayThrow();
	}

	/**
	 * Replaces the instanceof operator with the bytecode derived from the given
	 * source text.
	 * <p>
	 * $0 is available but the value is <code>null</code>.
	 *
	 * @param statement
	 *            a Java statement except try-catch.
	 */
	@Override
	public void replace(String statement) throws CannotCompileException
	{
		this.thisClass.getClassFile(); // to call checkModify().
		ConstPool constPool = this.getConstPool();
		int pos = this.currentPos;
		int index = this.iterator.u16bitAt(pos + 1);

		Javac jc = new Javac(this.thisClass);
		ClassPool cp = this.thisClass.getClassPool();
		CodeAttribute ca = this.iterator.get();

		try
		{
			CtClass[] params = new CtClass[]
			{ cp.get(Expr.javaLangObject) };
			CtClass retType = CtClass.booleanType;

			int paramVar = ca.getMaxLocals();
			jc.recordParams(Expr.javaLangObject, params, true, paramVar, this.withinStatic());
			int retVar = jc.recordReturnType(retType, true);
			jc.recordProceed(new ProceedForInstanceof(index));

			// because $type is not the return type...
			jc.recordType(this.getType());

			/*
			 * Is $_ included in the source code?
			 */
			Expr.checkResultValue(retType, statement);

			Bytecode bytecode = jc.getBytecode();
			Expr.storeStack(params, true, paramVar, bytecode);
			jc.recordLocalVariables(ca, pos);

			bytecode.addConstZero(retType);
			bytecode.addStore(retVar, retType); // initialize $_

			jc.compileStmnt(statement);
			bytecode.addLoad(retVar, retType);

			this.replace0(pos, bytecode, 3);
		} catch (CompileError e)
		{
			throw new CannotCompileException(e);
		} catch (NotFoundException e)
		{
			throw new CannotCompileException(e);
		} catch (BadBytecode e)
		{
			throw new CannotCompileException("broken method");
		}
	}

	/**
	 * Returns the method or constructor containing the instanceof expression
	 * represented by this object.
	 */
	@Override
	public CtBehavior where()
	{
		return super.where();
	}
}
