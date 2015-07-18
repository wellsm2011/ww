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
 * Array creation.
 * <p>
 * This class does not provide methods for obtaining the initial values of array
 * elements.
 */
public class NewArray extends Expr
{
	/*
	 * <array type> $proceed(<dim> ..)
	 */
	static class ProceedForArray implements ProceedHandler
	{
		CtClass	arrayType;
		int		opcode;
		int		index, dimension;

		ProceedForArray(CtClass type, int op, int i, int dim)
		{
			this.arrayType = type;
			this.opcode = op;
			this.index = i;
			this.dimension = dim;
		}

		@Override
		public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args) throws CompileError
		{
			int num = gen.getMethodArgsLength(args);
			if (num != this.dimension)
				throw new CompileError(Javac.proceedName + "() with a wrong number of parameters");

			gen.atMethodArgs(args, new int[num], new int[num], new String[num]);
			bytecode.addOpcode(this.opcode);
			if (this.opcode == Opcode.ANEWARRAY)
				bytecode.addIndex(this.index);
			else if (this.opcode == Opcode.NEWARRAY)
				bytecode.add(this.index);
			else
			/* if (opcode == Opcode.MULTIANEWARRAY) */ {
				bytecode.addIndex(this.index);
				bytecode.add(this.dimension);
				bytecode.growStack(1 - this.dimension);
			}

			gen.setType(this.arrayType);
		}

		@Override
		public void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError
		{
			c.setType(this.arrayType);
		}
	}

	int opcode;

	protected NewArray(int pos, CodeIterator i, CtClass declaring, MethodInfo m, int op)
	{
		super(pos, i, declaring, m);
		this.opcode = op;
	}

	/**
	 * Returns the type of array components. If the created array is a
	 * two-dimensional array of <tt>int</tt>, the type returned by this method
	 * is not <tt>int[]</tt> but <tt>int</tt>.
	 */
	public CtClass getComponentType() throws NotFoundException
	{
		if (this.opcode == Opcode.NEWARRAY)
		{
			int atype = this.iterator.byteAt(this.currentPos + 1);
			return this.getPrimitiveType(atype);
		} else if (this.opcode == Opcode.ANEWARRAY || this.opcode == Opcode.MULTIANEWARRAY)
		{
			int index = this.iterator.u16bitAt(this.currentPos + 1);
			String desc = this.getConstPool().getClassInfo(index);
			int dim = Descriptor.arrayDimension(desc);
			desc = Descriptor.toArrayComponent(desc, dim);
			return Descriptor.toCtClass(desc, this.thisClass.getClassPool());
		} else
			throw new RuntimeException("bad opcode: " + this.opcode);
	}

	/**
	 * Returns the number of dimensions of arrays to be created. If the opcode
	 * is multianewarray, this method returns the second operand. Otherwise, it
	 * returns 1.
	 */
	public int getCreatedDimensions()
	{
		if (this.opcode == Opcode.MULTIANEWARRAY)
			return this.iterator.byteAt(this.currentPos + 3);
		else
			return 1;
	}

	/**
	 * Returns the dimension of the created array.
	 */
	public int getDimension()
	{
		if (this.opcode == Opcode.NEWARRAY)
			return 1;
		else if (this.opcode == Opcode.ANEWARRAY || this.opcode == Opcode.MULTIANEWARRAY)
		{
			int index = this.iterator.u16bitAt(this.currentPos + 1);
			String desc = this.getConstPool().getClassInfo(index);
			return Descriptor.arrayDimension(desc) + (this.opcode == Opcode.ANEWARRAY ? 1 : 0);
		} else
			throw new RuntimeException("bad opcode: " + this.opcode);
	}

	/**
	 * Returns the source file containing the array creation.
	 *
	 * @return null if this information is not available.
	 */
	@Override
	public String getFileName()
	{
		return super.getFileName();
	}

	/**
	 * Returns the line number of the source line containing the array creation.
	 *
	 * @return -1 if this information is not available.
	 */
	@Override
	public int getLineNumber()
	{
		return super.getLineNumber();
	}

	CtClass getPrimitiveType(int atype)
	{
		switch (atype)
		{
			case Opcode.T_BOOLEAN:
				return CtClass.booleanType;
			case Opcode.T_CHAR:
				return CtClass.charType;
			case Opcode.T_FLOAT:
				return CtClass.floatType;
			case Opcode.T_DOUBLE:
				return CtClass.doubleType;
			case Opcode.T_BYTE:
				return CtClass.byteType;
			case Opcode.T_SHORT:
				return CtClass.shortType;
			case Opcode.T_INT:
				return CtClass.intType;
			case Opcode.T_LONG:
				return CtClass.longType;
			default:
				throw new RuntimeException("bad atype: " + atype);
		}
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
	 * Replaces the array creation with the bytecode derived from the given
	 * source text.
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
		try
		{
			this.replace2(statement);
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

	private void replace2(String statement) throws CompileError, NotFoundException, BadBytecode, CannotCompileException
	{
		this.thisClass.getClassFile(); // to call checkModify().
		ConstPool constPool = this.getConstPool();
		int pos = this.currentPos;
		CtClass retType;
		int codeLength;
		int index = 0;
		int dim = 1;
		String desc;
		if (this.opcode == Opcode.NEWARRAY)
		{
			index = this.iterator.byteAt(this.currentPos + 1); // atype
			CtPrimitiveType cpt = (CtPrimitiveType) this.getPrimitiveType(index);
			desc = "[" + cpt.getDescriptor();
			codeLength = 2;
		} else if (this.opcode == Opcode.ANEWARRAY)
		{
			index = this.iterator.u16bitAt(pos + 1);
			desc = constPool.getClassInfo(index);
			if (desc.startsWith("["))
				desc = "[" + desc;
			else
				desc = "[L" + desc + ";";

			codeLength = 3;
		} else if (this.opcode == Opcode.MULTIANEWARRAY)
		{
			index = this.iterator.u16bitAt(this.currentPos + 1);
			desc = constPool.getClassInfo(index);
			dim = this.iterator.byteAt(this.currentPos + 3);
			codeLength = 4;
		} else
			throw new RuntimeException("bad opcode: " + this.opcode);

		retType = Descriptor.toCtClass(desc, this.thisClass.getClassPool());

		Javac jc = new Javac(this.thisClass);
		CodeAttribute ca = this.iterator.get();

		CtClass[] params = new CtClass[dim];
		for (int i = 0; i < dim; ++i)
			params[i] = CtClass.intType;

		int paramVar = ca.getMaxLocals();
		jc.recordParams(Expr.javaLangObject, params, true, paramVar, this.withinStatic());

		/*
		 * Is $_ included in the source code?
		 */
		Expr.checkResultValue(retType, statement);
		int retVar = jc.recordReturnType(retType, true);
		jc.recordProceed(new ProceedForArray(retType, this.opcode, index, dim));

		Bytecode bytecode = jc.getBytecode();
		Expr.storeStack(params, true, paramVar, bytecode);
		jc.recordLocalVariables(ca, pos);

		bytecode.addOpcode(Opcode.ACONST_NULL); // initialize $_
		bytecode.addAstore(retVar);

		jc.compileStmnt(statement);
		bytecode.addAload(retVar);

		this.replace0(pos, bytecode, codeLength);
	}

	/**
	 * Returns the method or constructor containing the array creation
	 * represented by this object.
	 */
	@Override
	public CtBehavior where()
	{
		return super.where();
	}
}
