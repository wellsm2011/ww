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
import javassist.CtField;
import javassist.CtPrimitiveType;
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
 * Expression for accessing a field.
 */
public class FieldAccess extends Expr
{
	/*
	 * <field type> $proceed()
	 */
	static class ProceedForRead implements ProceedHandler
	{
		CtClass	fieldType;
		int		opcode;
		int		targetVar, index;

		ProceedForRead(CtClass type, int op, int i, int var)
		{
			this.fieldType = type;
			this.targetVar = var;
			this.opcode = op;
			this.index = i;
		}

		@Override
		public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args) throws CompileError
		{
			if (args != null && !gen.isParamListName(args))
				throw new CompileError(Javac.proceedName + "() cannot take a parameter for field reading");

			int stack;
			if (FieldAccess.isStatic(this.opcode))
				stack = 0;
			else
			{
				stack = -1;
				bytecode.addAload(this.targetVar);
			}

			if (this.fieldType instanceof CtPrimitiveType)
				stack += ((CtPrimitiveType) this.fieldType).getDataSize();
			else
				++stack;

			bytecode.add(this.opcode);
			bytecode.addIndex(this.index);
			bytecode.growStack(stack);
			gen.setType(this.fieldType);
		}

		@Override
		public void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError
		{
			c.setType(this.fieldType);
		}
	}

	/*
	 * void $proceed(<field type>) the return type is not the field type but
	 * void.
	 */
	static class ProceedForWrite implements ProceedHandler
	{
		CtClass	fieldType;
		int		opcode;
		int		targetVar, index;

		ProceedForWrite(CtClass type, int op, int i, int var)
		{
			this.fieldType = type;
			this.targetVar = var;
			this.opcode = op;
			this.index = i;
		}

		@Override
		public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args) throws CompileError
		{
			if (gen.getMethodArgsLength(args) != 1)
				throw new CompileError(Javac.proceedName + "() cannot take more than one parameter " + "for field writing");

			int stack;
			if (FieldAccess.isStatic(this.opcode))
				stack = 0;
			else
			{
				stack = -1;
				bytecode.addAload(this.targetVar);
			}

			gen.atMethodArgs(args, new int[1], new int[1], new String[1]);
			gen.doNumCast(this.fieldType);
			if (this.fieldType instanceof CtPrimitiveType)
				stack -= ((CtPrimitiveType) this.fieldType).getDataSize();
			else
				--stack;

			bytecode.add(this.opcode);
			bytecode.addIndex(this.index);
			bytecode.growStack(stack);
			gen.setType(CtClass.voidType);
			gen.addNullIfVoid();
		}

		@Override
		public void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError
		{
			c.atMethodArgs(args, new int[1], new int[1], new String[1]);
			c.setType(CtClass.voidType);
			c.addNullIfVoid();
		}
	}

	static boolean isStatic(int c)
	{
		return c == Opcode.GETSTATIC || c == Opcode.PUTSTATIC;
	}

	int opcode;

	protected FieldAccess(int pos, CodeIterator i, CtClass declaring, MethodInfo m, int op)
	{
		super(pos, i, declaring, m);
		this.opcode = op;
	}

	/**
	 * Returns the name of the class in which the field is declared.
	 */
	public String getClassName()
	{
		int index = this.iterator.u16bitAt(this.currentPos + 1);
		return this.getConstPool().getFieldrefClassName(index);
	}

	/**
	 * Returns the class in which the field is declared.
	 */
	private CtClass getCtClass() throws NotFoundException
	{
		return this.thisClass.getClassPool().get(this.getClassName());
	}

	/**
	 * Returns the field accessed by this expression.
	 */
	public CtField getField() throws NotFoundException
	{
		CtClass cc = this.getCtClass();
		int index = this.iterator.u16bitAt(this.currentPos + 1);
		ConstPool cp = this.getConstPool();
		return cc.getField(cp.getFieldrefName(index), cp.getFieldrefType(index));
	}

	/**
	 * Returns the name of the field.
	 */
	public String getFieldName()
	{
		int index = this.iterator.u16bitAt(this.currentPos + 1);
		return this.getConstPool().getFieldrefName(index);
	}

	/**
	 * Returns the source file containing the field access.
	 *
	 * @return null if this information is not available.
	 */
	@Override
	public String getFileName()
	{
		return super.getFileName();
	}

	/**
	 * Returns the line number of the source line containing the field access.
	 *
	 * @return -1 if this information is not available.
	 */
	@Override
	public int getLineNumber()
	{
		return super.getLineNumber();
	}

	/**
	 * Returns the signature of the field type. The signature is represented by
	 * a character string called field descriptor, which is defined in the JVM
	 * specification.
	 *
	 * @see javassist.bytecode.Descriptor#toCtClass(String, ClassPool)
	 * @since 3.1
	 */
	public String getSignature()
	{
		int index = this.iterator.u16bitAt(this.currentPos + 1);
		return this.getConstPool().getFieldrefType(index);
	}

	/**
	 * Returns true if the field is read.
	 */
	public boolean isReader()
	{
		return this.opcode == Opcode.GETFIELD || this.opcode == Opcode.GETSTATIC;
	}

	/**
	 * Returns true if the field is static.
	 */
	public boolean isStatic()
	{
		return FieldAccess.isStatic(this.opcode);
	}

	/**
	 * Returns true if the field is written in.
	 */
	public boolean isWriter()
	{
		return this.opcode == Opcode.PUTFIELD || this.opcode == Opcode.PUTSTATIC;
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
	 * Replaces the method call with the bytecode derived from the given source
	 * text.
	 * <p>
	 * $0 is available even if the called method is static. If the field access
	 * is writing, $_ is available but the value of $_ is ignored.
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
		CodeAttribute ca = this.iterator.get();
		try
		{
			CtClass[] params;
			CtClass retType;
			CtClass fieldType = Descriptor.toCtClass(constPool.getFieldrefType(index), this.thisClass.getClassPool());
			boolean read = this.isReader();
			if (read)
			{
				params = new CtClass[0];
				retType = fieldType;
			} else
			{
				params = new CtClass[1];
				params[0] = fieldType;
				retType = CtClass.voidType;
			}

			int paramVar = ca.getMaxLocals();
			jc.recordParams(constPool.getFieldrefClassName(index), params, true, paramVar, this.withinStatic());

			/*
			 * Is $_ included in the source code?
			 */
			boolean included = Expr.checkResultValue(retType, statement);
			if (read)
				included = true;

			int retVar = jc.recordReturnType(retType, included);
			if (read)
				jc.recordProceed(new ProceedForRead(retType, this.opcode, index, paramVar));
			else
			{
				// because $type is not the return type...
				jc.recordType(fieldType);
				jc.recordProceed(new ProceedForWrite(params[0], this.opcode, index, paramVar));
			}

			Bytecode bytecode = jc.getBytecode();
			Expr.storeStack(params, this.isStatic(), paramVar, bytecode);
			jc.recordLocalVariables(ca, pos);

			if (included)
				if (retType == CtClass.voidType)
				{
					bytecode.addOpcode(Opcode.ACONST_NULL);
					bytecode.addAstore(retVar);
				} else
				{
					bytecode.addConstZero(retType);
					bytecode.addStore(retVar, retType); // initialize $_
				}

			jc.compileStmnt(statement);
			if (read)
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
	 * Returns the method or constructor containing the field-access expression
	 * represented by this object.
	 */
	@Override
	public CtBehavior where()
	{
		return super.where();
	}
}
