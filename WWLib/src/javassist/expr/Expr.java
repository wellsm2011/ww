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

import java.util.Iterator;
import java.util.LinkedList;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.ExceptionsAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.compiler.Javac;

/**
 * Expression.
 */
public abstract class Expr implements Opcode
{
	static final String javaLangObject = "java.lang.Object";

	private static void addClass(LinkedList list, CtClass c)
	{
		Iterator it = list.iterator();
		while (it.hasNext())
			if (it.next() == c)
				return;

		list.add(c);
	}

	static final boolean checkResultValue(CtClass retType, String prog) throws CannotCompileException
	{
		/*
		 * Is $_ included in the source code?
		 */
		boolean hasIt = prog.indexOf(Javac.resultVarName) >= 0;
		if (!hasIt && retType != CtClass.voidType)
			throw new CannotCompileException("the resulting value is not stored in " + Javac.resultVarName);

		return hasIt;
	}

	/*
	 * If isStaticCall is true, null is assigned to $0. So $0 must be declared
	 * by calling Javac.recordParams(). After executing this method, the current
	 * stack depth might be less than 0.
	 */
	static final void storeStack(CtClass[] params, boolean isStaticCall, int regno, Bytecode bytecode)
	{
		Expr.storeStack0(0, params.length, params, regno + 1, bytecode);
		if (isStaticCall)
			bytecode.addOpcode(Opcode.ACONST_NULL);

		bytecode.addAstore(regno);
	}

	private static void storeStack0(int i, int n, CtClass[] params, int regno, Bytecode bytecode)
	{
		if (i >= n)
			return;
		else
		{
			CtClass c = params[i];
			int size;
			if (c instanceof CtPrimitiveType)
				size = ((CtPrimitiveType) c).getDataSize();
			else
				size = 1;

			Expr.storeStack0(i + 1, n, params, regno + size, bytecode);
			bytecode.addStore(regno, c);
		}
	}

	int currentPos;

	CodeIterator iterator;

	CtClass thisClass;

	MethodInfo thisMethod;

	boolean edited;

	int maxLocals, maxStack;

	/**
	 * Undocumented constructor. Do not use; internal-use only.
	 */
	protected Expr(int pos, CodeIterator i, CtClass declaring, MethodInfo m)
	{
		this.currentPos = pos;
		this.iterator = i;
		this.thisClass = declaring;
		this.thisMethod = m;
	}

	protected final boolean edited()
	{
		return this.edited;
	}

	protected final ConstPool getConstPool()
	{
		return this.thisMethod.getConstPool();
	}

	/**
	 * Returns the class that declares the method enclosing this expression.
	 *
	 * @since 3.7
	 */
	public CtClass getEnclosingClass()
	{
		return this.thisClass;
	}

	/**
	 * Returns the source file containing the expression.
	 *
	 * @return null if this information is not available.
	 */
	public String getFileName()
	{
		ClassFile cf = this.thisClass.getClassFile2();
		if (cf == null)
			return null;
		else
			return cf.getSourceFile();
	}

	/**
	 * Returns the line number of the source line containing the expression.
	 *
	 * @return -1 if this information is not available.
	 */
	public int getLineNumber()
	{
		return this.thisMethod.getLineNumber(this.currentPos);
	}

	/**
	 * Returns the index of the bytecode corresponding to the expression. It is
	 * the index into the byte array containing the Java bytecode that
	 * implements the method.
	 */
	public int indexOfBytecode()
	{
		return this.currentPos;
	}

	protected final int locals()
	{
		return this.maxLocals;
	}

	/**
	 * Returns the list of exceptions that the expression may throw. This list
	 * includes both the exceptions that the try-catch statements including the
	 * expression can catch and the exceptions that the throws declaration
	 * allows the method to throw.
	 */
	public CtClass[] mayThrow()
	{
		ClassPool pool = this.thisClass.getClassPool();
		ConstPool cp = this.thisMethod.getConstPool();
		LinkedList list = new LinkedList();
		try
		{
			CodeAttribute ca = this.thisMethod.getCodeAttribute();
			ExceptionTable et = ca.getExceptionTable();
			int pos = this.currentPos;
			int n = et.size();
			for (int i = 0; i < n; ++i)
				if (et.startPc(i) <= pos && pos < et.endPc(i))
				{
					int t = et.catchType(i);
					if (t > 0)
						try
						{
							Expr.addClass(list, pool.get(cp.getClassInfo(t)));
						} catch (NotFoundException e)
						{
						}
				}
		} catch (NullPointerException e)
		{
		}

		ExceptionsAttribute ea = this.thisMethod.getExceptionsAttribute();
		if (ea != null)
		{
			String[] exceptions = ea.getExceptions();
			if (exceptions != null)
			{
				int n = exceptions.length;
				for (int i = 0; i < n; ++i)
					try
					{
						Expr.addClass(list, pool.get(exceptions[i]));
					} catch (NotFoundException e)
					{
					}
			}
		}

		return (CtClass[]) list.toArray(new CtClass[list.size()]);
	}

	/**
	 * Replaces this expression with the bytecode derived from the given source
	 * text.
	 *
	 * @param statement
	 *            a Java statement except try-catch.
	 */
	public abstract void replace(String statement) throws CannotCompileException;

	/**
	 * Replaces this expression with the bytecode derived from the given source
	 * text and <code>ExprEditor</code>.
	 *
	 * @param statement
	 *            a Java statement except try-catch.
	 * @param recursive
	 *            if not null, the substituted bytecode is recursively processed
	 *            by the given <code>ExprEditor</code>.
	 * @since 3.1
	 */
	public void replace(String statement, ExprEditor recursive) throws CannotCompileException
	{
		this.replace(statement);
		if (recursive != null)
			this.runEditor(recursive, this.iterator);
	}

	protected void replace0(int pos, Bytecode bytecode, int size) throws BadBytecode
	{
		byte[] code = bytecode.get();
		this.edited = true;
		int gap = code.length - size;
		for (int i = 0; i < size; ++i)
			this.iterator.writeByte(Opcode.NOP, pos + i);

		if (gap > 0)
			pos = this.iterator.insertGapAt(pos, gap, false).position;

		this.iterator.write(code, pos);
		this.iterator.insert(bytecode.getExceptionTable(), pos);
		this.maxLocals = bytecode.getMaxLocals();
		this.maxStack = bytecode.getMaxStack();
	}

	// The implementation of replace() should call thisClass.checkModify()
	// so that isModify() will return true. Otherwise, thisClass.classfile
	// might be released during compilation and the compiler might generate
	// bytecode with a wrong copy of ConstPool.

	protected void runEditor(ExprEditor ed, CodeIterator oldIterator) throws CannotCompileException
	{
		CodeAttribute codeAttr = oldIterator.get();
		int orgLocals = codeAttr.getMaxLocals();
		int orgStack = codeAttr.getMaxStack();
		int newLocals = this.locals();
		codeAttr.setMaxStack(this.stack());
		codeAttr.setMaxLocals(newLocals);
		ExprEditor.LoopContext context = new ExprEditor.LoopContext(newLocals);
		int size = oldIterator.getCodeLength();
		int endPos = oldIterator.lookAhead();
		oldIterator.move(this.currentPos);
		if (ed.doit(this.thisClass, this.thisMethod, context, oldIterator, endPos))
			this.edited = true;

		oldIterator.move(endPos + oldIterator.getCodeLength() - size);
		codeAttr.setMaxLocals(orgLocals);
		codeAttr.setMaxStack(orgStack);
		this.maxLocals = context.maxLocals;
		this.maxStack += context.maxStack;
	}

	protected final int stack()
	{
		return this.maxStack;
	}

	/**
	 * Returns the constructor or method containing the expression.
	 */
	public CtBehavior where()
	{
		MethodInfo mi = this.thisMethod;
		CtBehavior[] cb = this.thisClass.getDeclaredBehaviors();
		for (int i = cb.length - 1; i >= 0; --i)
			if (cb[i].getMethodInfo2() == mi)
				return cb[i];

		CtConstructor init = this.thisClass.getClassInitializer();
		if (init != null && init.getMethodInfo2() == mi)
			return init;

		/*
		 * getDeclaredBehaviors() returns a list of methods/constructors.
		 * Although the list is cached in a CtClass object, it might be
		 * recreated for some reason. Thus, the member name and the signature
		 * must be also checked.
		 */
		for (int i = cb.length - 1; i >= 0; --i)
			if (this.thisMethod.getName().equals(cb[i].getMethodInfo2().getName()) && this.thisMethod.getDescriptor().equals(cb[i].getMethodInfo2().getDescriptor()))
				return cb[i];

		throw new RuntimeException("fatal: not found");
	}

	/**
	 * Returns true if this method is static.
	 */
	protected final boolean withinStatic()
	{
		return (this.thisMethod.getAccessFlags() & AccessFlag.STATIC) != 0;
	}
}
