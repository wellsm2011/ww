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
import javassist.CtMethod;
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

/**
 * Method invocation (caller-side expression).
 */
public class MethodCall extends Expr
{
	/**
	 * Undocumented constructor. Do not use; internal-use only.
	 */
	protected MethodCall(int pos, CodeIterator i, CtClass declaring, MethodInfo m)
	{
		super(pos, i, declaring, m);
	}

	/**
	 * Returns the class name of the target object, which the method is called
	 * on.
	 */
	public String getClassName()
	{
		String cname;

		ConstPool cp = this.getConstPool();
		int pos = this.currentPos;
		int c = this.iterator.byteAt(pos);
		int index = this.iterator.u16bitAt(pos + 1);

		if (c == Opcode.INVOKEINTERFACE)
			cname = cp.getInterfaceMethodrefClassName(index);
		else
			cname = cp.getMethodrefClassName(index);

		if (cname.charAt(0) == '[')
			cname = Descriptor.toClassName(cname);

		return cname;
	}

	/**
	 * Returns the class of the target object, which the method is called on.
	 */
	protected CtClass getCtClass() throws NotFoundException
	{
		return this.thisClass.getClassPool().get(this.getClassName());
	}

	/**
	 * Returns the source file containing the method call.
	 *
	 * @return null if this information is not available.
	 */
	@Override
	public String getFileName()
	{
		return super.getFileName();
	}

	/**
	 * Returns the line number of the source line containing the method call.
	 *
	 * @return -1 if this information is not available.
	 */
	@Override
	public int getLineNumber()
	{
		return super.getLineNumber();
	}

	/**
	 * Returns the called method.
	 */
	public CtMethod getMethod() throws NotFoundException
	{
		return this.getCtClass().getMethod(this.getMethodName(), this.getSignature());
	}

	/**
	 * Returns the name of the called method.
	 */
	public String getMethodName()
	{
		ConstPool cp = this.getConstPool();
		int nt = this.getNameAndType(cp);
		return cp.getUtf8Info(cp.getNameAndTypeName(nt));
	}

	private int getNameAndType(ConstPool cp)
	{
		int pos = this.currentPos;
		int c = this.iterator.byteAt(pos);
		int index = this.iterator.u16bitAt(pos + 1);

		if (c == Opcode.INVOKEINTERFACE)
			return cp.getInterfaceMethodrefNameAndType(index);
		else
			return cp.getMethodrefNameAndType(index);
	}

	/**
	 * Returns the method signature (the parameter types and the return type).
	 * The method signature is represented by a character string called method
	 * descriptor, which is defined in the JVM specification.
	 *
	 * @see javassist.CtBehavior#getSignature()
	 * @see javassist.bytecode.Descriptor
	 * @since 3.1
	 */
	public String getSignature()
	{
		ConstPool cp = this.getConstPool();
		int nt = this.getNameAndType(cp);
		return cp.getUtf8Info(cp.getNameAndTypeDescriptor(nt));
	}

	/**
	 * Returns true if the called method is of a superclass of the current
	 * class.
	 */
	public boolean isSuper()
	{
		return this.iterator.byteAt(this.currentPos) == Opcode.INVOKESPECIAL && !this.where().getDeclaringClass().getName().equals(this.getClassName());
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
	 *
	 * <p>
	 * $0 is available even if the called method is static.
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

		String classname, methodname, signature;
		int opcodeSize;
		int c = this.iterator.byteAt(pos);
		if (c == Opcode.INVOKEINTERFACE)
		{
			opcodeSize = 5;
			classname = constPool.getInterfaceMethodrefClassName(index);
			methodname = constPool.getInterfaceMethodrefName(index);
			signature = constPool.getInterfaceMethodrefType(index);
		} else if (c == Opcode.INVOKESTATIC || c == Opcode.INVOKESPECIAL || c == Opcode.INVOKEVIRTUAL)
		{
			opcodeSize = 3;
			classname = constPool.getMethodrefClassName(index);
			methodname = constPool.getMethodrefName(index);
			signature = constPool.getMethodrefType(index);
		} else
			throw new CannotCompileException("not method invocation");

		Javac jc = new Javac(this.thisClass);
		ClassPool cp = this.thisClass.getClassPool();
		CodeAttribute ca = this.iterator.get();
		try
		{
			CtClass[] params = Descriptor.getParameterTypes(signature, cp);
			CtClass retType = Descriptor.getReturnType(signature, cp);
			int paramVar = ca.getMaxLocals();
			jc.recordParams(classname, params, true, paramVar, this.withinStatic());
			int retVar = jc.recordReturnType(retType, true);
			if (c == Opcode.INVOKESTATIC)
				jc.recordStaticProceed(classname, methodname);
			else if (c == Opcode.INVOKESPECIAL)
				jc.recordSpecialProceed(Javac.param0Name, classname, methodname, signature);
			else
				jc.recordProceed(Javac.param0Name, methodname);

			/*
			 * Is $_ included in the source code?
			 */
			Expr.checkResultValue(retType, statement);

			Bytecode bytecode = jc.getBytecode();
			Expr.storeStack(params, c == Opcode.INVOKESTATIC, paramVar, bytecode);
			jc.recordLocalVariables(ca, pos);

			if (retType != CtClass.voidType)
			{
				bytecode.addConstZero(retType);
				bytecode.addStore(retVar, retType); // initialize $_
			}

			jc.compileStmnt(statement);
			if (retType != CtClass.voidType)
				bytecode.addLoad(retVar, retType);

			this.replace0(pos, bytecode, opcodeSize);
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

	/*
	 * Returns the parameter types of the called method.
	 * 
	 * public CtClass[] getParameterTypes() throws NotFoundException { return
	 * Descriptor.getParameterTypes(getMethodDesc(), thisClass.getClassPool());
	 * }
	 */

	/*
	 * Returns the return type of the called method.
	 * 
	 * public CtClass getReturnType() throws NotFoundException { return
	 * Descriptor.getReturnType(getMethodDesc(), thisClass.getClassPool()); }
	 */

	/**
	 * Returns the method or constructor containing the method-call expression
	 * represented by this object.
	 */
	@Override
	public CtBehavior where()
	{
		return super.where();
	}
}
