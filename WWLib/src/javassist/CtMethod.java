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

package javassist;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * An instance of <code>CtMethod</code> represents a method.
 *
 * <p>
 * See the super class <code>CtBehavior</code> since a number of useful methods
 * are in <code>CtBehavior</code>. A number of useful factory methods are in
 * <code>CtNewMethod</code>.
 *
 * @see CtClass#getDeclaredMethods()
 * @see CtNewMethod
 */
public final class CtMethod extends CtBehavior
{
	/**
	 * Instances of this class represent a constant parameter. They are used to
	 * specify the parameter given to the methods created by
	 * <code>CtNewMethod.wrapped()</code>.
	 *
	 * @see CtMethod#setWrappedBody(CtMethod,CtMethod.ConstParameter)
	 * @see CtNewMethod#wrapped(CtClass,String,CtClass[],CtClass[],CtMethod,CtMethod.ConstParameter,CtClass)
	 * @see CtNewConstructor#make(CtClass[],CtClass[],int,CtMethod,CtMethod.ConstParameter,CtClass)
	 */
	public static class ConstParameter
	{
		/**
		 * Returns the default descriptor for constructors.
		 */
		static String defaultConstDescriptor()
		{
			return "([Ljava/lang/Object;)V";
		}

		/**
		 * @see CtNewWrappedMethod
		 */
		static String defaultDescriptor()
		{
			return "([Ljava/lang/Object;)Ljava/lang/Object;";
		}

		/**
		 * Makes an integer constant.
		 *
		 * @param i
		 *            the constant value.
		 */
		public static ConstParameter integer(int i)
		{
			return new IntConstParameter(i);
		}

		/**
		 * Makes a long integer constant.
		 *
		 * @param i
		 *            the constant value.
		 */
		public static ConstParameter integer(long i)
		{
			return new LongConstParameter(i);
		}

		/**
		 * Makes an <code>String</code> constant.
		 *
		 * @param s
		 *            the constant value.
		 */
		public static ConstParameter string(String s)
		{
			return new StringConstParameter(s);
		}

		ConstParameter()
		{
		}

		/**
		 * @return the size of the stack consumption.
		 */
		int compile(Bytecode code) throws CannotCompileException
		{
			return 0;
		}

		/**
		 * Returns the descriptor for constructors.
		 *
		 * @see CtNewWrappedConstructor
		 */
		String constDescriptor()
		{
			return ConstParameter.defaultConstDescriptor();
		}

		String descriptor()
		{
			return ConstParameter.defaultDescriptor();
		}
	}

	static class IntConstParameter extends ConstParameter
	{
		int	param;

		IntConstParameter(int i)
		{
			this.param = i;
		}

		@Override
		int compile(Bytecode code) throws CannotCompileException
		{
			code.addIconst(this.param);
			return 1;
		}

		@Override
		String constDescriptor()
		{
			return "([Ljava/lang/Object;I)V";
		}

		@Override
		String descriptor()
		{
			return "([Ljava/lang/Object;I)Ljava/lang/Object;";
		}
	}

	static class LongConstParameter extends ConstParameter
	{
		long	param;

		LongConstParameter(long l)
		{
			this.param = l;
		}

		@Override
		int compile(Bytecode code) throws CannotCompileException
		{
			code.addLconst(this.param);
			return 2;
		}

		@Override
		String constDescriptor()
		{
			return "([Ljava/lang/Object;J)V";
		}

		@Override
		String descriptor()
		{
			return "([Ljava/lang/Object;J)Ljava/lang/Object;";
		}
	}

	static class StringConstParameter extends ConstParameter
	{
		String	param;

		StringConstParameter(String s)
		{
			this.param = s;
		}

		@Override
		int compile(Bytecode code) throws CannotCompileException
		{
			code.addLdc(this.param);
			return 1;
		}

		@Override
		String constDescriptor()
		{
			return "([Ljava/lang/Object;Ljava/lang/String;)V";
		}

		@Override
		String descriptor()
		{
			return "([Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";
		}
	}

	/**
	 * Creates a method from a <code>MethodInfo</code> object.
	 *
	 * @param declaring
	 *            the class declaring the method.
	 * @throws CannotCompileException
	 *             if the the <code>MethodInfo</code> object and the declaring
	 *             class have different <code>ConstPool</code> objects
	 * @since 3.6
	 */
	public static CtMethod make(MethodInfo minfo, CtClass declaring) throws CannotCompileException
	{
		if (declaring.getClassFile2().getConstPool() != minfo.getConstPool())
			throw new CannotCompileException("bad declaring class");

		return new CtMethod(minfo, declaring);
	}

	/**
	 * Compiles the given source code and creates a method. This method simply
	 * delegates to <code>make()</code> in <code>CtNewMethod</code>. See it for
	 * more details. <code>CtNewMethod</code> has a number of useful factory
	 * methods.
	 *
	 * @param src
	 *            the source text.
	 * @param declaring
	 *            the class to which the created method is added.
	 * @see CtNewMethod#make(String, CtClass)
	 */
	public static CtMethod make(String src, CtClass declaring) throws CannotCompileException
	{
		return CtNewMethod.make(src, declaring);
	}

	protected String	cachedStringRep;

	/**
	 * Creates a public abstract method. The created method must be added to a
	 * class with <code>CtClass.addMethod()</code>.
	 *
	 * @param declaring
	 *            the class to which the created method is added.
	 * @param returnType
	 *            the type of the returned value
	 * @param mname
	 *            the method name
	 * @param parameters
	 *            a list of the parameter types
	 *
	 * @see CtClass#addMethod(CtMethod)
	 */
	public CtMethod(CtClass returnType, String mname, CtClass[] parameters, CtClass declaring)
	{
		this(null, declaring);
		ConstPool cp = declaring.getClassFile2().getConstPool();
		String desc = Descriptor.ofMethod(returnType, parameters);
		this.methodInfo = new MethodInfo(cp, mname, desc);
		this.setModifiers(Modifier.PUBLIC | Modifier.ABSTRACT);
	}

	/**
	 * Creates a copy of a <code>CtMethod</code> object. The created method must
	 * be added to a class with <code>CtClass.addMethod()</code>.
	 *
	 * <p>
	 * All occurrences of class names in the created method are replaced with
	 * names specified by <code>map</code> if <code>map</code> is not
	 * <code>null</code>.
	 *
	 * <p>
	 * For example, suppose that a method <code>at()</code> is as follows:
	 *
	 * <pre>
	 * public X at(int i)
	 * {
	 * 	return (X) super.elementAt(i);
	 * }
	 * </pre>
	 *
	 * <p>
	 * (<code>X</code> is a class name.) If <code>map</code> substitutes
	 * <code>String</code> for <code>X</code>, then the created method is:
	 *
	 * <pre>
	 * public String at(int i)
	 * {
	 * 	return (String) super.elementAt(i);
	 * }
	 * </pre>
	 *
	 * <p>
	 * By default, all the occurrences of the names of the class declaring
	 * <code>at()</code> and the superclass are replaced with the name of the
	 * class and the superclass that the created method is added to. This is
	 * done whichever <code>map</code> is null or not. To prevent this
	 * replacement, call <code>ClassMap.fix()</code> or <code>put()</code> to
	 * explicitly specify replacement.
	 *
	 * <p>
	 * <b>Note:</b> if the <code>.class</code> notation (for example,
	 * <code>String.class</code>) is included in an expression, the Javac
	 * compiler may produce a helper method. Since this constructor never copies
	 * this helper method, the programmers have the responsiblity of copying it.
	 * Otherwise, use <code>Class.forName()</code> in the expression.
	 *
	 * @param src
	 *            the source method.
	 * @param declaring
	 *            the class to which the created method is added.
	 * @param map
	 *            the hashtable associating original class names with
	 *            substituted names. It can be <code>null</code>.
	 *
	 * @see CtClass#addMethod(CtMethod)
	 * @see ClassMap#fix(String)
	 */
	public CtMethod(CtMethod src, CtClass declaring, ClassMap map) throws CannotCompileException
	{
		this(null, declaring);
		this.copy(src, false, map);
	}

	/**
	 * @see #make(MethodInfo minfo, CtClass declaring)
	 */
	CtMethod(MethodInfo minfo, CtClass declaring)
	{
		super(declaring, minfo);
		this.cachedStringRep = null;
	}

	/**
	 * Indicates whether <code>obj</code> has the same name and the same
	 * signature as this method.
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj != null && obj instanceof CtMethod && ((CtMethod) obj).getStringRep().equals(this.getStringRep());
	}

	/**
	 * Returns the method name followed by parameter types such as
	 * <code>javassist.CtMethod.setBody(String)</code>.
	 *
	 * @since 3.5
	 */
	@Override
	public String getLongName()
	{
		return this.getDeclaringClass().getName() + "." + this.getName() + Descriptor.toString(this.getSignature());
	}

	/**
	 * Obtains the name of this method.
	 */
	@Override
	public String getName()
	{
		return this.methodInfo.getName();
	}

	/**
	 * Obtains the type of the returned value.
	 */
	public CtClass getReturnType() throws NotFoundException
	{
		return this.getReturnType0();
	}

	/*
	 * This method is also called by CtClassType.getMethods0().
	 */
	final String getStringRep()
	{
		if (this.cachedStringRep == null)
			this.cachedStringRep = this.methodInfo.getName() + Descriptor.getParamDescriptor(this.methodInfo.getDescriptor());

		return this.cachedStringRep;
	}

	/**
	 * Returns a hash code value for the method. If two methods have the same
	 * name and signature, then the hash codes for the two methods are equal.
	 */
	@Override
	public int hashCode()
	{
		return this.getStringRep().hashCode();
	}

	/**
	 * Returns true if the method body is empty, that is, <code>{}</code>. It
	 * also returns true if the method is an abstract method.
	 */
	@Override
	public boolean isEmpty()
	{
		CodeAttribute ca = this.getMethodInfo2().getCodeAttribute();
		if (ca == null) // abstract or native
			return (this.getModifiers() & Modifier.ABSTRACT) != 0;

		CodeIterator it = ca.iterator();
		try
		{
			return it.hasNext() && it.byteAt(it.next()) == Opcode.RETURN && !it.hasNext();
		} catch (BadBytecode e)
		{
		}
		return false;
	}

	// inner classes

	/**
	 * This method is invoked when setName() or replaceClassName() in CtClass is
	 * called.
	 */
	@Override
	void nameReplaced()
	{
		this.cachedStringRep = null;
	}

	/**
	 * Copies a method body from another method. If this method is abstract, the
	 * abstract modifier is removed after the method body is copied.
	 *
	 * <p>
	 * All occurrences of the class names in the copied method body are replaced
	 * with the names specified by <code>map</code> if <code>map</code> is not
	 * <code>null</code>.
	 *
	 * @param src
	 *            the method that the body is copied from.
	 * @param map
	 *            the hashtable associating original class names with
	 *            substituted names. It can be <code>null</code>.
	 */
	public void setBody(CtMethod src, ClassMap map) throws CannotCompileException
	{
		CtBehavior.setBody0(src.declaringClass, src.methodInfo, this.declaringClass, this.methodInfo, map);
	}

	/**
	 * Changes the name of this method.
	 */
	public void setName(String newname)
	{
		this.declaringClass.checkModify();
		this.methodInfo.setName(newname);
	}

	/**
	 * Replace a method body with a new method body wrapping the given method.
	 *
	 * @param mbody
	 *            the wrapped method
	 * @param constParam
	 *            the constant parameter given to the wrapped method (maybe
	 *            <code>null</code>).
	 *
	 * @see CtNewMethod#wrapped(CtClass,String,CtClass[],CtClass[],CtMethod,CtMethod.ConstParameter,CtClass)
	 */
	public void setWrappedBody(CtMethod mbody, ConstParameter constParam) throws CannotCompileException
	{
		this.declaringClass.checkModify();

		CtClass clazz = this.getDeclaringClass();
		CtClass[] params;
		CtClass retType;
		try
		{
			params = this.getParameterTypes();
			retType = this.getReturnType();
		} catch (NotFoundException e)
		{
			throw new CannotCompileException(e);
		}

		Bytecode code = CtNewWrappedMethod.makeBody(clazz, clazz.getClassFile2(), mbody, params, retType, constParam);
		CodeAttribute cattr = code.toCodeAttribute();
		this.methodInfo.setCodeAttribute(cattr);
		this.methodInfo.setAccessFlags(this.methodInfo.getAccessFlags() & ~AccessFlag.ABSTRACT);
		// rebuilding a stack map table is not needed.
	}
}
