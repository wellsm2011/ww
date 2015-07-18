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
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.JvstTypeChecker;
import javassist.compiler.ProceedHandler;
import javassist.compiler.ast.ASTList;

/**
 * Object creation (<tt>new</tt> expression).
 */
public class NewExpr extends Expr
{
	static class ProceedForNew implements ProceedHandler
	{
		CtClass	newType;
		int		newIndex, methodIndex;

		ProceedForNew(CtClass nt, int ni, int mi)
		{
			this.newType = nt;
			this.newIndex = ni;
			this.methodIndex = mi;
		}

		@Override
		public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args) throws CompileError
		{
			bytecode.addOpcode(Opcode.NEW);
			bytecode.addIndex(this.newIndex);
			bytecode.addOpcode(Opcode.DUP);
			gen.atMethodCallCore(this.newType, MethodInfo.nameInit, args, false, true, -1, null);
			gen.setType(this.newType);
		}

		@Override
		public void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError
		{
			c.atMethodCallCore(this.newType, MethodInfo.nameInit, args);
			c.setType(this.newType);
		}
	}

	String newTypeName;

	int newPos;

	/*
	 * Not used private int getNameAndType(ConstPool cp) { int pos = currentPos;
	 * int c = iterator.byteAt(pos); int index = iterator.u16bitAt(pos + 1); if
	 * (c == INVOKEINTERFACE) return cp.getInterfaceMethodrefNameAndType(index);
	 * else return cp.getMethodrefNameAndType(index); }
	 */

	/**
	 * Undocumented constructor. Do not use; internal-use only.
	 */
	protected NewExpr(int pos, CodeIterator i, CtClass declaring, MethodInfo m, String type, int np)
	{
		super(pos, i, declaring, m);
		this.newTypeName = type;
		this.newPos = np;
	}

	private int canReplace() throws CannotCompileException
	{
		int op = this.iterator.byteAt(this.newPos + 3);
		if (op == Opcode.DUP)   // Typical single DUP or Javaflow DUP DUP2_X2
  // POP2
			return this.iterator.byteAt(this.newPos + 4) == Opcode.DUP2_X2 && this.iterator.byteAt(this.newPos + 5) == Opcode.POP2 ? 6 : 4;
		else if (op == Opcode.DUP_X1 && this.iterator.byteAt(this.newPos + 4) == Opcode.SWAP)
			return 5;
		else
			return 3; // for Eclipse. The generated code may include no DUP.
		// throw new CannotCompileException(
		// "sorry, cannot edit NEW followed by no DUP");
	}

	/**
	 * Returns the class name of the created object.
	 */
	public String getClassName()
	{
		return this.newTypeName;
	}

	/**
	 * Returns the constructor called for creating the object.
	 */
	public CtConstructor getConstructor() throws NotFoundException
	{
		ConstPool cp = this.getConstPool();
		int index = this.iterator.u16bitAt(this.currentPos + 1);
		String desc = cp.getMethodrefType(index);
		return this.getCtClass().getConstructor(desc);
	}

	/**
	 * Returns the class of the created object.
	 */
	private CtClass getCtClass() throws NotFoundException
	{
		return this.thisClass.getClassPool().get(this.newTypeName);
	}

	/**
	 * Returns the source file containing the <tt>new</tt> expression.
	 *
	 * @return null if this information is not available.
	 */
	@Override
	public String getFileName()
	{
		return super.getFileName();
	}

	/**
	 * Returns the line number of the source line containing the <tt>new</tt>
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
	 * Get the signature of the constructor The signature is represented by a
	 * character string called method descriptor, which is defined in the JVM
	 * specification.
	 *
	 * @see javassist.CtBehavior#getSignature()
	 * @see javassist.bytecode.Descriptor
	 * @return the signature
	 */
	public String getSignature()
	{
		ConstPool constPool = this.getConstPool();
		int methodIndex = this.iterator.u16bitAt(this.currentPos + 1); // constructor
		return constPool.getMethodrefType(methodIndex);
	}

	/*
	 * Returns the parameter types of the constructor. public CtClass[]
	 * getParameterTypes() throws NotFoundException { ConstPool cp =
	 * getConstPool(); int index = iterator.u16bitAt(currentPos + 1); String
	 * desc = cp.getMethodrefType(index); return
	 * Descriptor.getParameterTypes(desc, thisClass.getClassPool()); }
	 */

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
	 * Replaces the <tt>new</tt> expression with the bytecode derived from the
	 * given source text.
	 * <p>
	 * $0 is available but the value is null.
	 *
	 * @param statement
	 *            a Java statement except try-catch.
	 */
	@Override
	public void replace(String statement) throws CannotCompileException
	{
		this.thisClass.getClassFile(); // to call checkModify().

		final int bytecodeSize = 3;
		int pos = this.newPos;

		int newIndex = this.iterator.u16bitAt(pos + 1);

		/*
		 * delete the preceding NEW and DUP (or DUP_X1, SWAP) instructions.
		 */
		int codeSize = this.canReplace();
		int end = pos + codeSize;
		for (int i = pos; i < end; ++i)
			this.iterator.writeByte(Opcode.NOP, i);

		ConstPool constPool = this.getConstPool();
		pos = this.currentPos;
		int methodIndex = this.iterator.u16bitAt(pos + 1); // constructor

		String signature = constPool.getMethodrefType(methodIndex);

		Javac jc = new Javac(this.thisClass);
		ClassPool cp = this.thisClass.getClassPool();
		CodeAttribute ca = this.iterator.get();
		try
		{
			CtClass[] params = Descriptor.getParameterTypes(signature, cp);
			CtClass newType = cp.get(this.newTypeName);
			int paramVar = ca.getMaxLocals();
			jc.recordParams(this.newTypeName, params, true, paramVar, this.withinStatic());
			int retVar = jc.recordReturnType(newType, true);
			jc.recordProceed(new ProceedForNew(newType, newIndex, methodIndex));

			/*
			 * Is $_ included in the source code?
			 */
			Expr.checkResultValue(newType, statement);

			Bytecode bytecode = jc.getBytecode();
			Expr.storeStack(params, true, paramVar, bytecode);
			jc.recordLocalVariables(ca, pos);

			bytecode.addConstZero(newType);
			bytecode.addStore(retVar, newType); // initialize $_

			jc.compileStmnt(statement);
			if (codeSize > 3)   // if the original code includes DUP.
				bytecode.addAload(retVar);

			this.replace0(pos, bytecode, bytecodeSize);
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
	 * Returns the method or constructor containing the <tt>new</tt> expression
	 * represented by this object.
	 */
	@Override
	public CtBehavior where()
	{
		return super.where();
	}
}
