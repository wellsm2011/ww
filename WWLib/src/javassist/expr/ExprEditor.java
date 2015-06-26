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
import javassist.CtClass;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * A translator of method bodies.
 * <p>
 * The users can define a subclass of this class to customize how to modify a
 * method body. The overall architecture is similar to the strategy pattern.
 * <p>
 * If <code>instrument()</code> is called in <code>CtMethod</code>, the method
 * body is scanned from the beginning to the end. Whenever an expression, such
 * as a method call and a <tt>new</tt> expression (object creation), is found,
 * <code>edit()</code> is called in <code>ExprEdit</code>. <code>edit()</code>
 * can inspect and modify the given expression. The modification is reflected on
 * the original method body. If <code>edit()</code> does nothing, the original
 * method body is not changed.
 * <p>
 * The following code is an example:
 *
 * <pre>
 * CtMethod cm = ...;
 * cm.instrument(new ExprEditor() {
 *     public void edit(MethodCall m) throws CannotCompileException {
 *         if (m.getClassName().equals("Point")) {
 *             System.out.println(m.getMethodName() + " line: "
 *                                + m.getLineNumber());
 *     }
 * });
 * </pre>
 * <p>
 * This code inspects all method calls appearing in the method represented by
 * <code>cm</code> and it prints the names and the line numbers of the methods
 * declared in class <code>Point</code>. This code does not modify the body of
 * the method represented by <code>cm</code>. If the method body must be
 * modified, call <code>replace()</code> in <code>MethodCall</code>.
 *
 * @see javassist.CtClass#instrument(ExprEditor)
 * @see javassist.CtMethod#instrument(ExprEditor)
 * @see javassist.CtConstructor#instrument(ExprEditor)
 * @see MethodCall
 * @see NewExpr
 * @see FieldAccess
 * @see javassist.CodeConverter
 */
public class ExprEditor
{
	final static class LoopContext
	{
		NewOp	newList;
		int		maxLocals;
		int		maxStack;

		LoopContext(int locals)
		{
			this.maxLocals = locals;
			this.maxStack = 0;
			this.newList = null;
		}

		void updateMax(int locals, int stack)
		{
			if (this.maxLocals < locals)
				this.maxLocals = locals;

			if (this.maxStack < stack)
				this.maxStack = stack;
		}
	}

	final static class NewOp
	{
		NewOp	next;
		int		pos;
		String	type;

		NewOp(NewOp n, int p, String t)
		{
			this.next = n;
			this.pos = p;
			this.type = t;
		}
	}

	/**
	 * Default constructor. It does nothing.
	 */
	public ExprEditor()
	{
	}

	/**
	 * Undocumented method. Do not use; internal-use only.
	 */
	public boolean doit(CtClass clazz, MethodInfo minfo) throws CannotCompileException
	{
		CodeAttribute codeAttr = minfo.getCodeAttribute();
		if (codeAttr == null)
			return false;

		CodeIterator iterator = codeAttr.iterator();
		boolean edited = false;
		LoopContext context = new LoopContext(codeAttr.getMaxLocals());

		while (iterator.hasNext())
			if (this.loopBody(iterator, clazz, minfo, context))
				edited = true;

		ExceptionTable et = codeAttr.getExceptionTable();
		int n = et.size();
		for (int i = 0; i < n; ++i)
		{
			Handler h = new Handler(et, i, iterator, clazz, minfo);
			this.edit(h);
			if (h.edited())
			{
				edited = true;
				context.updateMax(h.locals(), h.stack());
			}
		}

		// codeAttr might be modified by other partiess
		// so I check the current value of max-locals.
		if (codeAttr.getMaxLocals() < context.maxLocals)
			codeAttr.setMaxLocals(context.maxLocals);

		codeAttr.setMaxStack(codeAttr.getMaxStack() + context.maxStack);
		try
		{
			if (edited)
				minfo.rebuildStackMapIf6(clazz.getClassPool(), clazz.getClassFile2());
		} catch (BadBytecode b)
		{
			throw new CannotCompileException(b.getMessage(), b);
		}

		return edited;
	}

	/**
	 * Visits each bytecode in the given range.
	 */
	boolean doit(CtClass clazz, MethodInfo minfo, LoopContext context, CodeIterator iterator, int endPos) throws CannotCompileException
	{
		boolean edited = false;
		while (iterator.hasNext() && iterator.lookAhead() < endPos)
		{
			int size = iterator.getCodeLength();
			if (this.loopBody(iterator, clazz, minfo, context))
			{
				edited = true;
				int size2 = iterator.getCodeLength();
				if (size != size2) // the body was modified.
					endPos += size2 - size;
			}
		}

		return edited;
	}

	/**
	 * Edits an expression for explicit type casting (overridable). The default
	 * implementation performs nothing.
	 */
	public void edit(Cast c) throws CannotCompileException
	{
	}

	/**
	 * Edits a constructor call (overridable). The constructor call is either
	 * <code>super()</code> or <code>this()</code> included in a constructor
	 * body. The default implementation performs nothing.
	 *
	 * @see #edit(NewExpr)
	 */
	public void edit(ConstructorCall c) throws CannotCompileException
	{
	}

	/**
	 * Edits a field-access expression (overridable). Field access means both
	 * read and write. The default implementation performs nothing.
	 */
	public void edit(FieldAccess f) throws CannotCompileException
	{
	}

	/**
	 * Edits a catch clause (overridable). The default implementation performs
	 * nothing.
	 */
	public void edit(Handler h) throws CannotCompileException
	{
	}

	/**
	 * Edits an instanceof expression (overridable). The default implementation
	 * performs nothing.
	 */
	public void edit(Instanceof i) throws CannotCompileException
	{
	}

	/**
	 * Edits a method call (overridable). The default implementation performs
	 * nothing.
	 */
	public void edit(MethodCall m) throws CannotCompileException
	{
	}

	/**
	 * Edits an expression for array creation (overridable). The default
	 * implementation performs nothing.
	 *
	 * @param a
	 *            the <tt>new</tt> expression for creating an array.
	 * @throws CannotCompileException
	 */
	public void edit(NewArray a) throws CannotCompileException
	{
	}

	/**
	 * Edits a <tt>new</tt> expression (overridable). The default implementation
	 * performs nothing.
	 *
	 * @param e
	 *            the <tt>new</tt> expression creating an object.
	 */
	public void edit(NewExpr e) throws CannotCompileException
	{
	}

	final boolean loopBody(CodeIterator iterator, CtClass clazz, MethodInfo minfo, LoopContext context) throws CannotCompileException
	{
		try
		{
			Expr expr = null;
			int pos = iterator.next();
			int c = iterator.byteAt(pos);

			if (c < Opcode.GETSTATIC) // c < 178
				/* skip */;
			else if (c < Opcode.NEWARRAY)
			{ // c < 188
				if (c == Opcode.INVOKESTATIC || c == Opcode.INVOKEINTERFACE || c == Opcode.INVOKEVIRTUAL)
				{
					expr = new MethodCall(pos, iterator, clazz, minfo);
					this.edit((MethodCall) expr);
				} else if (c == Opcode.GETFIELD || c == Opcode.GETSTATIC || c == Opcode.PUTFIELD || c == Opcode.PUTSTATIC)
				{
					expr = new FieldAccess(pos, iterator, clazz, minfo, c);
					this.edit((FieldAccess) expr);
				} else if (c == Opcode.NEW)
				{
					int index = iterator.u16bitAt(pos + 1);
					context.newList = new NewOp(context.newList, pos, minfo.getConstPool().getClassInfo(index));
				} else if (c == Opcode.INVOKESPECIAL)
				{
					NewOp newList = context.newList;
					if (newList != null && minfo.getConstPool().isConstructor(newList.type, iterator.u16bitAt(pos + 1)) > 0)
					{
						expr = new NewExpr(pos, iterator, clazz, minfo, newList.type, newList.pos);
						this.edit((NewExpr) expr);
						context.newList = newList.next;
					} else
					{
						MethodCall mcall = new MethodCall(pos, iterator, clazz, minfo);
						if (mcall.getMethodName().equals(MethodInfo.nameInit))
						{
							ConstructorCall ccall = new ConstructorCall(pos, iterator, clazz, minfo);
							expr = ccall;
							this.edit(ccall);
						} else
						{
							expr = mcall;
							this.edit(mcall);
						}
					}
				}
			} else if (c == Opcode.NEWARRAY || c == Opcode.ANEWARRAY || c == Opcode.MULTIANEWARRAY)
			{
				expr = new NewArray(pos, iterator, clazz, minfo, c);
				this.edit((NewArray) expr);
			} else if (c == Opcode.INSTANCEOF)
			{
				expr = new Instanceof(pos, iterator, clazz, minfo);
				this.edit((Instanceof) expr);
			} else if (c == Opcode.CHECKCAST)
			{
				expr = new Cast(pos, iterator, clazz, minfo);
				this.edit((Cast) expr);
			}

			if (expr != null && expr.edited())
			{
				context.updateMax(expr.locals(), expr.stack());
				return true;
			} else
				return false;
		} catch (BadBytecode e)
		{
			throw new CannotCompileException(e);
		}
	}
}
