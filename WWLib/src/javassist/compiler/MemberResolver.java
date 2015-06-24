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

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.compiler.ast.ASTList;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.Declarator;
import javassist.compiler.ast.Keyword;
import javassist.compiler.ast.Symbol;

/* Code generator methods depending on javassist.* classes.
 */
public class MemberResolver implements TokenId
{
	public static class Method
	{
		public CtClass		declaring;
		public MethodInfo	info;
		public int			notmatch;

		public Method(CtClass c, MethodInfo i, int n)
		{
			this.declaring = c;
			this.info = i;
			this.notmatch = n;
		}

		/**
		 * Returns true if the invoked method is static.
		 */
		public boolean isStatic()
		{
			int acc = this.info.getAccessFlags();
			return (acc & AccessFlag.STATIC) != 0;
		}
	}

	private static final int	YES				= 0;

	private static final int	NO				= -1;

	private static final String	INVALID			= "<invalid>";

	private static WeakHashMap	invalidNamesMap	= new WeakHashMap();

	public static int descToType(char c) throws CompileError
	{
		switch (c)
		{
			case 'Z':
				return TokenId.BOOLEAN;
			case 'C':
				return TokenId.CHAR;
			case 'B':
				return TokenId.BYTE;
			case 'S':
				return TokenId.SHORT;
			case 'I':
				return TokenId.INT;
			case 'J':
				return TokenId.LONG;
			case 'F':
				return TokenId.FLOAT;
			case 'D':
				return TokenId.DOUBLE;
			case 'V':
				return TokenId.VOID;
			case 'L':
			case '[':
				return TokenId.CLASS;
			default:
				MemberResolver.fatal();
				return TokenId.VOID; // never reach here
		}
	}

	private static void fatal() throws CompileError
	{
		throw new CompileError("fatal");
	}

	// for unit tests
	public static int getInvalidMapSize()
	{
		return MemberResolver.invalidNamesMap.size();
	}

	public static int getModifiers(ASTList mods)
	{
		int m = 0;
		while (mods != null)
		{
			Keyword k = (Keyword) mods.head();
			mods = mods.tail();
			switch (k.get())
			{
				case STATIC:
					m |= Modifier.STATIC;
					break;
				case FINAL:
					m |= Modifier.FINAL;
					break;
				case SYNCHRONIZED:
					m |= Modifier.SYNCHRONIZED;
					break;
				case ABSTRACT:
					m |= Modifier.ABSTRACT;
					break;
				case PUBLIC:
					m |= Modifier.PUBLIC;
					break;
				case PROTECTED:
					m |= Modifier.PROTECTED;
					break;
				case PRIVATE:
					m |= Modifier.PRIVATE;
					break;
				case VOLATILE:
					m |= Modifier.VOLATILE;
					break;
				case TRANSIENT:
					m |= Modifier.TRANSIENT;
					break;
				case STRICT:
					m |= Modifier.STRICT;
					break;
			}
		}

		return m;
	}

	public static CtClass getSuperclass(CtClass c) throws CompileError
	{
		try
		{
			CtClass sc = c.getSuperclass();
			if (sc != null)
				return sc;
		} catch (NotFoundException e)
		{
		}
		throw new CompileError("cannot find the super class of " + c.getName());
	}

	public static CtClass getSuperInterface(CtClass c, String interfaceName) throws CompileError
	{
		try
		{
			CtClass[] intfs = c.getInterfaces();
			for (int i = 0; i < intfs.length; i++)
				if (intfs[i].getName().equals(interfaceName))
					return intfs[i];
		} catch (NotFoundException e)
		{
		}
		throw new CompileError("cannot find the super inetrface " + interfaceName + " of " + c.getName());
	}

	/*
	 * type cannot be CLASS
	 */
	static String getTypeName(int type) throws CompileError
	{
		String cname = "";
		switch (type)
		{
			case BOOLEAN:
				cname = "boolean";
				break;
			case CHAR:
				cname = "char";
				break;
			case BYTE:
				cname = "byte";
				break;
			case SHORT:
				cname = "short";
				break;
			case INT:
				cname = "int";
				break;
			case LONG:
				cname = "long";
				break;
			case FLOAT:
				cname = "float";
				break;
			case DOUBLE:
				cname = "double";
				break;
			case VOID:
				cname = "void";
				break;
			default:
				MemberResolver.fatal();
		}

		return cname;
	}

	public static String javaToJvmName(String classname)
	{
		return classname.replace('.', '/');
	}

	public static String jvmToJavaName(String classname)
	{
		return classname.replace('/', '.');
	}

	private ClassPool	classPool;

	private Hashtable	invalidNames	= null;

	public MemberResolver(ClassPool cp)
	{
		this.classPool = cp;
	}

	/*
	 * Returns YES if actual parameter types matches the given signature.
	 * 
	 * argTypes, argDims, and argClassNames represent actual parameters.
	 * 
	 * This method does not correctly implement the Java method dispatch
	 * algorithm.
	 * 
	 * If some of the parameter types exactly match but others are subtypes of
	 * the corresponding type in the signature, this method returns the number
	 * of parameter types that do not exactly match.
	 */
	private int compareSignature(String desc, int[] argTypes, int[] argDims, String[] argClassNames) throws CompileError
	{
		int result = MemberResolver.YES;
		int i = 1;
		int nArgs = argTypes.length;
		if (nArgs != Descriptor.numOfParameters(desc))
			return MemberResolver.NO;

		int len = desc.length();
		for (int n = 0; i < len; ++n)
		{
			char c = desc.charAt(i++);
			if (c == ')')
				return n == nArgs ? result : MemberResolver.NO;
			else if (n >= nArgs)
				return MemberResolver.NO;

			int dim = 0;
			while (c == '[')
			{
				++dim;
				c = desc.charAt(i++);
			}

			if (argTypes[n] == TokenId.NULL)
			{
				if (dim == 0 && c != 'L')
					return MemberResolver.NO;

				if (c == 'L')
					i = desc.indexOf(';', i) + 1;
			} else if (argDims[n] != dim)
			{
				if (!(dim == 0 && c == 'L' && desc.startsWith("java/lang/Object;", i)))
					return MemberResolver.NO;

				// if the thread reaches here, c must be 'L'.
				i = desc.indexOf(';', i) + 1;
				result++;
				if (i <= 0)
					return MemberResolver.NO; // invalid descriptor?
			} else if (c == 'L')
			{ // not compare
				int j = desc.indexOf(';', i);
				if (j < 0 || argTypes[n] != TokenId.CLASS)
					return MemberResolver.NO;

				String cname = desc.substring(i, j);
				if (!cname.equals(argClassNames[n]))
				{
					CtClass clazz = this.lookupClassByJvmName(argClassNames[n]);
					try
					{
						if (clazz.subtypeOf(this.lookupClassByJvmName(cname)))
							result++;
						else
							return MemberResolver.NO;
					} catch (NotFoundException e)
					{
						result++; // should be NO?
					}
				}

				i = j + 1;
			} else
			{
				int t = MemberResolver.descToType(c);
				int at = argTypes[n];
				if (t != at)
					if (t == TokenId.INT && (at == TokenId.SHORT || at == TokenId.BYTE || at == TokenId.CHAR))
						result++;
					else
						return MemberResolver.NO;
			}
		}

		return MemberResolver.NO;
	}

	public ClassPool getClassPool()
	{
		return this.classPool;
	}

	private Hashtable getInvalidNames()
	{
		Hashtable ht = this.invalidNames;
		if (ht == null)
		{
			synchronized (MemberResolver.class)
			{
				WeakReference ref = (WeakReference) MemberResolver.invalidNamesMap.get(this.classPool);
				if (ref != null)
					ht = (Hashtable) ref.get();

				if (ht == null)
				{
					ht = new Hashtable();
					MemberResolver.invalidNamesMap.put(this.classPool, new WeakReference(ht));
				}
			}

			this.invalidNames = ht;
		}

		return ht;
	}

	public CtClass lookupClass(Declarator decl) throws CompileError
	{
		return this.lookupClass(decl.getType(), decl.getArrayDim(), decl.getClassName());
	}

	/**
	 * @param classname
	 *            jvm class name.
	 */
	public CtClass lookupClass(int type, int dim, String classname) throws CompileError
	{
		String cname = "";
		CtClass clazz;
		if (type == TokenId.CLASS)
		{
			clazz = this.lookupClassByJvmName(classname);
			if (dim > 0)
				cname = clazz.getName();
			else
				return clazz;
		} else
			cname = MemberResolver.getTypeName(type);

		while (dim-- > 0)
			cname += "[]";

		return this.lookupClass(cname, false);
	}

	/**
	 * @param name
	 *            a qualified class name. e.g. java.lang.String
	 */
	public CtClass lookupClass(String name, boolean notCheckInner) throws CompileError
	{
		Hashtable cache = this.getInvalidNames();
		Object found = cache.get(name);
		if (found == MemberResolver.INVALID)
			throw new CompileError("no such class: " + name);
		else if (found != null)
			try
			{
				return this.classPool.get((String) found);
			} catch (NotFoundException e)
			{
			}

		CtClass cc = null;
		try
		{
			cc = this.lookupClass0(name, notCheckInner);
		} catch (NotFoundException e)
		{
			cc = this.searchImports(name);
		}

		cache.put(name, cc.getName());
		return cc;
	}

	private CtClass lookupClass0(String classname, boolean notCheckInner) throws NotFoundException
	{
		CtClass cc = null;
		do
			try
			{
				cc = this.classPool.get(classname);
			} catch (NotFoundException e)
			{
				int i = classname.lastIndexOf('.');
				if (notCheckInner || i < 0)
					throw e;
				else
				{
					StringBuffer sbuf = new StringBuffer(classname);
					sbuf.setCharAt(i, '$');
					classname = sbuf.toString();
				}
			}
		while (cc == null);
		return cc;
	}

	public CtClass lookupClassByJvmName(String jvmName) throws CompileError
	{
		return this.lookupClass(MemberResolver.jvmToJavaName(jvmName), false);
	}

	public CtClass lookupClassByName(ASTList name) throws CompileError
	{
		return this.lookupClass(Declarator.astToClassName(name, '.'), false);
	}

	/**
	 * @param className
	 *            a qualified class name. e.g. java.lang.String
	 */
	public CtField lookupField(String className, Symbol fieldName) throws CompileError
	{
		CtClass cc = this.lookupClass(className, false);
		try
		{
			return cc.getField(fieldName.get());
		} catch (NotFoundException e)
		{
		}
		throw new CompileError("no such field: " + fieldName.get());
	}

	/**
	 * @param jvmClassName
	 *            a JVM class name. e.g. java/lang/String
	 */
	public CtField lookupFieldByJvmName(String jvmClassName, Symbol fieldName) throws CompileError
	{
		return this.lookupField(MemberResolver.jvmToJavaName(jvmClassName), fieldName);
	}

	/**
	 * Only used by fieldAccess() in MemberCodeGen and TypeChecker.
	 *
	 * @param jvmClassName
	 *            a JVM class name. e.g. java/lang/String
	 * @see #lookupClass(String, boolean)
	 */
	public CtField lookupFieldByJvmName2(String jvmClassName, Symbol fieldSym, ASTree expr) throws NoFieldException
	{
		String field = fieldSym.get();
		CtClass cc = null;
		try
		{
			cc = this.lookupClass(MemberResolver.jvmToJavaName(jvmClassName), true);
		} catch (CompileError e)
		{
			// EXPR might be part of a qualified class name.
			throw new NoFieldException(jvmClassName + "/" + field, expr);
		}

		try
		{
			return cc.getField(field);
		} catch (NotFoundException e)
		{
			// maybe an inner class.
			jvmClassName = MemberResolver.javaToJvmName(cc.getName());
			throw new NoFieldException(jvmClassName + "$" + field, expr);
		}
	}

	public Method lookupMethod(CtClass clazz, CtClass currentClass, MethodInfo current, String methodName, int[] argTypes, int[] argDims, String[] argClassNames) throws CompileError
	{
		Method maybe = null;
		// to enable the creation of a recursively called method
		if (current != null && clazz == currentClass)
			if (current.getName().equals(methodName))
			{
				int res = this.compareSignature(current.getDescriptor(), argTypes, argDims, argClassNames);
				if (res != MemberResolver.NO)
				{
					Method r = new Method(clazz, current, res);
					if (res == MemberResolver.YES)
						return r;
					else
						maybe = r;
				}
			}

		Method m = this.lookupMethod(clazz, methodName, argTypes, argDims, argClassNames, maybe != null);
		if (m != null)
			return m;
		else
			return maybe;
	}

	private Method lookupMethod(CtClass clazz, String methodName, int[] argTypes, int[] argDims, String[] argClassNames, boolean onlyExact) throws CompileError
	{
		Method maybe = null;
		ClassFile cf = clazz.getClassFile2();
		// If the class is an array type, the class file is null.
		// If so, search the super class java.lang.Object for clone() etc.
		if (cf != null)
		{
			List list = cf.getMethods();
			int n = list.size();
			for (int i = 0; i < n; ++i)
			{
				MethodInfo minfo = (MethodInfo) list.get(i);
				if (minfo.getName().equals(methodName))
				{
					int res = this.compareSignature(minfo.getDescriptor(), argTypes, argDims, argClassNames);
					if (res != MemberResolver.NO)
					{
						Method r = new Method(clazz, minfo, res);
						if (res == MemberResolver.YES)
							return r;
						else if (maybe == null || maybe.notmatch > res)
							maybe = r;
					}
				}
			}
		}

		if (onlyExact)
			maybe = null;
		else
			onlyExact = maybe != null;

		int mod = clazz.getModifiers();
		boolean isIntf = Modifier.isInterface(mod);
		try
		{
			// skip searching java.lang.Object if clazz is an interface type.
			if (!isIntf)
			{
				CtClass pclazz = clazz.getSuperclass();
				if (pclazz != null)
				{
					Method r = this.lookupMethod(pclazz, methodName, argTypes, argDims, argClassNames, onlyExact);
					if (r != null)
						return r;
				}
			}
		} catch (NotFoundException e)
		{
		}

		try
		{
			CtClass[] ifs = clazz.getInterfaces();
			int size = ifs.length;
			for (int i = 0; i < size; ++i)
			{
				Method r = this.lookupMethod(ifs[i], methodName, argTypes, argDims, argClassNames, onlyExact);
				if (r != null)
					return r;
			}

			if (isIntf)
			{
				// finally search java.lang.Object.
				CtClass pclazz = clazz.getSuperclass();
				if (pclazz != null)
				{
					Method r = this.lookupMethod(pclazz, methodName, argTypes, argDims, argClassNames, onlyExact);
					if (r != null)
						return r;
				}
			}
		} catch (NotFoundException e)
		{
		}

		return maybe;
	}

	/*
	 * Converts a class name into a JVM-internal representation.
	 * 
	 * It may also expand a simple class name to java.lang.*. For example, this
	 * converts Object into java/lang/Object.
	 */
	public String resolveClassName(ASTList name) throws CompileError
	{
		if (name == null)
			return null;
		else
			return MemberResolver.javaToJvmName(this.lookupClassByName(name).getName());
	}

	/*
	 * Expands a simple class name to java.lang.*. For example, this converts
	 * Object into java/lang/Object.
	 */
	public String resolveJvmClassName(String jvmName) throws CompileError
	{
		if (jvmName == null)
			return null;
		else
			return MemberResolver.javaToJvmName(this.lookupClassByJvmName(jvmName).getName());
	}

	private CtClass searchImports(String orgName) throws CompileError
	{
		if (orgName.indexOf('.') < 0)
		{
			Iterator it = this.classPool.getImportedPackages();
			while (it.hasNext())
			{
				String pac = (String) it.next();
				String fqName = pac + '.' + orgName;
				try
				{
					return this.classPool.get(fqName);
				} catch (NotFoundException e)
				{
					try
					{
						if (pac.endsWith("." + orgName))
							return this.classPool.get(pac);
					} catch (NotFoundException e2)
					{
					}
				}
			}
		}

		this.getInvalidNames().put(orgName, MemberResolver.INVALID);
		throw new CompileError("no such class: " + orgName);
	}
}
