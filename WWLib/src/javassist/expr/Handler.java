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
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;

/**
 * A <code>catch</code> clause or a <code>finally</code> block.
 */
public class Handler extends Expr
{
	private static String	EXCEPTION_NAME	= "$1";
	private ExceptionTable	etable;
	private int				index;

	/**
	 * Undocumented constructor. Do not use; internal-use only.
	 */
	protected Handler(ExceptionTable et, int nth, CodeIterator it, CtClass declaring, MethodInfo m)
	{
		super(et.handlerPc(nth), it, declaring, m);
		this.etable = et;
		this.index = nth;
	}

	/**
	 * Returns the source file containing the catch clause.
	 *
	 * @return null if this information is not available.
	 */
	@Override
	public String getFileName()
	{
		return super.getFileName();
	}

	/**
	 * Returns the source line number of the catch clause.
	 *
	 * @return -1 if this information is not available.
	 */
	@Override
	public int getLineNumber()
	{
		return super.getLineNumber();
	}

	/**
	 * Returns the type handled by the catch clause. If this is a
	 * <code>finally</code> block, <code>null</code> is returned.
	 */
	public CtClass getType() throws NotFoundException
	{
		int type = this.etable.catchType(this.index);
		if (type == 0)
			return null;
		else
		{
			ConstPool cp = this.getConstPool();
			String name = cp.getClassInfo(type);
			return this.thisClass.getClassPool().getCtClass(name);
		}
	}

	/**
	 * Inserts bytecode at the beginning of the catch clause. The caught
	 * exception is stored in <code>$1</code>.
	 *
	 * @param src
	 *            the source code representing the inserted bytecode. It must be
	 *            a single statement or block.
	 */
	public void insertBefore(String src) throws CannotCompileException
	{
		this.edited = true;

		ConstPool cp = this.getConstPool();
		CodeAttribute ca = this.iterator.get();
		Javac jv = new Javac(this.thisClass);
		Bytecode b = jv.getBytecode();
		b.setStackDepth(1);
		b.setMaxLocals(ca.getMaxLocals());

		try
		{
			CtClass type = this.getType();
			int var = jv.recordVariable(type, Handler.EXCEPTION_NAME);
			jv.recordReturnType(type, false);
			b.addAstore(var);
			jv.compileStmnt(src);
			b.addAload(var);

			int oldHandler = this.etable.handlerPc(this.index);
			b.addOpcode(Opcode.GOTO);
			b.addIndex(oldHandler - this.iterator.getCodeLength() - b.currentPc() + 1);

			this.maxStack = b.getMaxStack();
			this.maxLocals = b.getMaxLocals();

			int pos = this.iterator.append(b.get());
			this.iterator.append(b.getExceptionTable(), pos);
			this.etable.setHandlerPc(this.index, pos);
		} catch (NotFoundException e)
		{
			throw new CannotCompileException(e);
		} catch (CompileError e)
		{
			throw new CannotCompileException(e);
		}
	}

	/**
	 * Returns true if this is a <code>finally</code> block.
	 */
	public boolean isFinally()
	{
		return this.etable.catchType(this.index) == 0;
	}

	/**
	 * Returns the list of exceptions that the catch clause may throw.
	 */
	@Override
	public CtClass[] mayThrow()
	{
		return super.mayThrow();
	}

	/**
	 * This method has not been implemented yet.
	 *
	 * @param statement
	 *            a Java statement except try-catch.
	 */
	@Override
	public void replace(String statement) throws CannotCompileException
	{
		throw new RuntimeException("not implemented yet");
	}

	/**
	 * Returns the method or constructor containing the catch clause.
	 */
	@Override
	public CtBehavior where()
	{
		return super.where();
	}
}
