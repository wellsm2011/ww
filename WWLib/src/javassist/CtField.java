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
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.SignatureAttribute;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.SymbolTable;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.DoubleConst;
import javassist.compiler.ast.IntConst;
import javassist.compiler.ast.StringL;

/**
 * An instance of CtField represents a field.
 *
 * @see CtClass#getDeclaredFields()
 */
public class CtField extends CtMember
{
	static class ArrayInitializer extends Initializer
	{
		CtClass	type;
		int		size;

		ArrayInitializer(CtClass t, int s)
		{
			this.type = t;
			this.size = s;
		}

		private void addNewarray(Bytecode code)
		{
			if (this.type.isPrimitive())
				code.addNewarray(((CtPrimitiveType) this.type).getArrayType(), this.size);
			else
				code.addAnewarray(this.type, this.size);
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			code.addAload(0);
			this.addNewarray(code);
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return 2; // stack size
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			this.addNewarray(code);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return 1; // stack size
		}
	}

	static class CodeInitializer extends CodeInitializer0
	{
		private String	expression;

		CodeInitializer(String expr)
		{
			this.expression = expr;
		}

		@Override
		void compileExpr(Javac drv) throws CompileError
		{
			drv.compileExpr(this.expression);
		}

		@Override
		int getConstantValue(ConstPool cp, CtClass type)
		{
			try
			{
				ASTree t = Javac.parseExpr(this.expression, new SymbolTable());
				return this.getConstantValue2(cp, type, t);
			} catch (CompileError e)
			{
				return 0;
			}
		}
	}

	static abstract class CodeInitializer0 extends Initializer
	{
		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			try
			{
				code.addAload(0);
				this.compileExpr(drv);
				code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
				return code.getMaxStack();
			} catch (CompileError e)
			{
				throw new CannotCompileException(e);
			}
		}

		abstract void compileExpr(Javac drv) throws CompileError;

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			try
			{
				this.compileExpr(drv);
				code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
				return code.getMaxStack();
			} catch (CompileError e)
			{
				throw new CannotCompileException(e);
			}
		}

		int getConstantValue2(ConstPool cp, CtClass type, ASTree tree)
		{
			if (type.isPrimitive())
			{
				if (tree instanceof IntConst)
				{
					long value = ((IntConst) tree).get();
					if (type == CtClass.doubleType)
						return cp.addDoubleInfo(value);
					else if (type == CtClass.floatType)
						return cp.addFloatInfo(value);
					else if (type == CtClass.longType)
						return cp.addLongInfo(value);
					else if (type != CtClass.voidType)
						return cp.addIntegerInfo((int) value);
				} else if (tree instanceof DoubleConst)
				{
					double value = ((DoubleConst) tree).get();
					if (type == CtClass.floatType)
						return cp.addFloatInfo((float) value);
					else if (type == CtClass.doubleType)
						return cp.addDoubleInfo(value);
				}
			} else if (tree instanceof StringL && type.getName().equals(CtField.javaLangString))
				return cp.addStringInfo(((StringL) tree).get());

			return 0;
		}
	}

	static class DoubleInitializer extends Initializer
	{
		double	value;

		DoubleInitializer(double v)
		{
			this.value = v;
		}

		@Override
		void check(String desc) throws CannotCompileException
		{
			if (!desc.equals("D"))
				throw new CannotCompileException("type mismatch");
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			code.addAload(0);
			code.addLdc2w(this.value);
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return 3; // stack size
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			code.addLdc2w(this.value);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return 2; // stack size
		}

		@Override
		int getConstantValue(ConstPool cp, CtClass type)
		{
			if (type == CtClass.doubleType)
				return cp.addDoubleInfo(this.value);
			else
				return 0;
		}
	}

	static class FloatInitializer extends Initializer
	{
		float	value;

		FloatInitializer(float v)
		{
			this.value = v;
		}

		@Override
		void check(String desc) throws CannotCompileException
		{
			if (!desc.equals("F"))
				throw new CannotCompileException("type mismatch");
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			code.addAload(0);
			code.addFconst(this.value);
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return 3; // stack size
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			code.addFconst(this.value);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return 2; // stack size
		}

		@Override
		int getConstantValue(ConstPool cp, CtClass type)
		{
			if (type == CtClass.floatType)
				return cp.addFloatInfo(this.value);
			else
				return 0;
		}
	}

	/**
	 * Instances of this class specify how to initialize a field.
	 * <code>Initializer</code> is passed to <code>CtClass.addField()</code>
	 * with a <code>CtField</code>.
	 * <p>
	 * This class cannot be instantiated with the <code>new</code> operator.
	 * Factory methods such as <code>byParameter()</code> and <code>byNew</code>
	 * must be used for the instantiation. They create a new instance with the
	 * given parameters and return it.
	 *
	 * @see CtClass#addField(CtField,CtField.Initializer)
	 */
	public static abstract class Initializer
	{
		/**
		 * Makes an initializer calling a static method.
		 * <p>
		 * This initializer calls a static method and uses the returned value as
		 * the initial value of the field. The called method receives the
		 * parameters:
		 * <p>
		 * <code>Object obj</code> - the object including the field.
		 * <p>
		 * If the initialized field is static, then the method does not receive
		 * any parameters.
		 * <p>
		 * The type of the returned value must be the same as the field type.
		 *
		 * @param methodClass
		 *            the class that the static method is declared in.
		 * @param methodName
		 *            the name of the satic method.
		 */
		public static Initializer byCall(CtClass methodClass, String methodName)
		{
			MethodInitializer i = new MethodInitializer();
			i.objectType = methodClass;
			i.methodName = methodName;
			i.stringParams = null;
			i.withConstructorParams = false;
			return i;
		}

		/**
		 * Makes an initializer calling a static method.
		 * <p>
		 * This initializer calls a static method and uses the returned value as
		 * the initial value of the field. The called method receives the
		 * parameters:
		 * <p>
		 * <code>Object obj</code> - the object including the field.<br>
		 * <code>String[] strs</code> - the character strings specified by
		 * <code>stringParams</code><br>
		 * <p>
		 * If the initialized field is static, then the method receive only
		 * <code>strs</code>.
		 * <p>
		 * The type of the returned value must be the same as the field type.
		 *
		 * @param methodClass
		 *            the class that the static method is declared in.
		 * @param methodName
		 *            the name of the satic method.
		 * @param stringParams
		 *            the array of strings passed to the static method.
		 */
		public static Initializer byCall(CtClass methodClass, String methodName, String[] stringParams)
		{
			MethodInitializer i = new MethodInitializer();
			i.objectType = methodClass;
			i.methodName = methodName;
			i.stringParams = stringParams;
			i.withConstructorParams = false;
			return i;
		}

		/**
		 * Makes an initializer calling a static method.
		 * <p>
		 * This initializer calls a static method and uses the returned value as
		 * the initial value of the field. The called method receives the
		 * parameters:
		 * <p>
		 * <code>Object obj</code> - the object including the field.<br>
		 * <code>Object[] args</code> - the parameters passed to the constructor
		 * of the object including the filed.
		 * <p>
		 * If the initialized field is static, then the method does not receive
		 * any parameters.
		 * <p>
		 * The type of the returned value must be the same as the field type.
		 *
		 * @param methodClass
		 *            the class that the static method is declared in.
		 * @param methodName
		 *            the name of the satic method.
		 */
		public static Initializer byCallWithParams(CtClass methodClass, String methodName)
		{
			MethodInitializer i = new MethodInitializer();
			i.objectType = methodClass;
			i.methodName = methodName;
			i.stringParams = null;
			i.withConstructorParams = true;
			return i;
		}

		/**
		 * Makes an initializer calling a static method.
		 * <p>
		 * This initializer calls a static method and uses the returned value as
		 * the initial value of the field. The called method receives the
		 * parameters:
		 * <p>
		 * <code>Object obj</code> - the object including the field.<br>
		 * <code>String[] strs</code> - the character strings specified by
		 * <code>stringParams</code><br>
		 * <code>Object[] args</code> - the parameters passed to the constructor
		 * of the object including the filed.
		 * <p>
		 * If the initialized field is static, then the method receive only
		 * <code>strs</code>.
		 * <p>
		 * The type of the returned value must be the same as the field type.
		 *
		 * @param methodClass
		 *            the class that the static method is declared in.
		 * @param methodName
		 *            the name of the satic method.
		 * @param stringParams
		 *            the array of strings passed to the static method.
		 */
		public static Initializer byCallWithParams(CtClass methodClass, String methodName, String[] stringParams)
		{
			MethodInitializer i = new MethodInitializer();
			i.objectType = methodClass;
			i.methodName = methodName;
			i.stringParams = stringParams;
			i.withConstructorParams = true;
			return i;
		}

		static Initializer byExpr(ASTree source)
		{
			return new PtreeInitializer(source);
		}

		/**
		 * Makes an initializer.
		 *
		 * @param source
		 *            initializer expression.
		 */
		public static Initializer byExpr(String source)
		{
			return new CodeInitializer(source);
		}

		/**
		 * Makes an initializer creating a new object.
		 * <p>
		 * This initializer creates a new object and uses it as the initial
		 * value of the field. The constructor of the created object receives
		 * the parameter:
		 * <p>
		 * <code>Object obj</code> - the object including the field.
		 * <p>
		 * If the initialized field is static, then the constructor does not
		 * receive any parameters.
		 *
		 * @param objectType
		 *            the class instantiated for the initial value.
		 */
		public static Initializer byNew(CtClass objectType)
		{
			NewInitializer i = new NewInitializer();
			i.objectType = objectType;
			i.stringParams = null;
			i.withConstructorParams = false;
			return i;
		}

		/**
		 * Makes an initializer creating a new object.
		 * <p>
		 * This initializer creates a new object and uses it as the initial
		 * value of the field. The constructor of the created object receives
		 * the parameters:
		 * <p>
		 * <code>Object obj</code> - the object including the field.<br>
		 * <code>String[] strs</code> - the character strings specified by
		 * <code>stringParams</code><br>
		 * <p>
		 * If the initialized field is static, then the constructor receives
		 * only <code>strs</code>.
		 *
		 * @param objectType
		 *            the class instantiated for the initial value.
		 * @param stringParams
		 *            the array of strings passed to the constructor.
		 */
		public static Initializer byNew(CtClass objectType, String[] stringParams)
		{
			NewInitializer i = new NewInitializer();
			i.objectType = objectType;
			i.stringParams = stringParams;
			i.withConstructorParams = false;
			return i;
		}

		/**
		 * Makes an initializer creating a new array.
		 *
		 * @param type
		 *            the type of the array.
		 * @param size
		 *            the size of the array.
		 * @throws NotFoundException
		 *             if the type of the array components is not found.
		 */
		public static Initializer byNewArray(CtClass type, int size) throws NotFoundException
		{
			return new ArrayInitializer(type.getComponentType(), size);
		}

		/**
		 * Makes an initializer creating a new multi-dimensional array.
		 *
		 * @param type
		 *            the type of the array.
		 * @param sizes
		 *            an <code>int</code> array of the size in every dimension.
		 *            The first element is the size in the first dimension. The
		 *            second is in the second, etc.
		 */
		public static Initializer byNewArray(CtClass type, int[] sizes)
		{
			return new MultiArrayInitializer(type, sizes);
		}

		/**
		 * Makes an initializer creating a new object.
		 * <p>
		 * This initializer creates a new object and uses it as the initial
		 * value of the field. The constructor of the created object receives
		 * the parameters:
		 * <p>
		 * <code>Object obj</code> - the object including the field.<br>
		 * <code>Object[] args</code> - the parameters passed to the constructor
		 * of the object including the filed.
		 * <p>
		 * If the initialized field is static, then the constructor does not
		 * receive any parameters.
		 *
		 * @param objectType
		 *            the class instantiated for the initial value.
		 * @see javassist.CtField.Initializer#byNewArray(CtClass,int)
		 * @see javassist.CtField.Initializer#byNewArray(CtClass,int[])
		 */
		public static Initializer byNewWithParams(CtClass objectType)
		{
			NewInitializer i = new NewInitializer();
			i.objectType = objectType;
			i.stringParams = null;
			i.withConstructorParams = true;
			return i;
		}

		/**
		 * Makes an initializer creating a new object.
		 * <p>
		 * This initializer creates a new object and uses it as the initial
		 * value of the field. The constructor of the created object receives
		 * the parameters:
		 * <p>
		 * <code>Object obj</code> - the object including the field.<br>
		 * <code>String[] strs</code> - the character strings specified by
		 * <code>stringParams</code><br>
		 * <code>Object[] args</code> - the parameters passed to the constructor
		 * of the object including the filed.
		 * <p>
		 * If the initialized field is static, then the constructor receives
		 * only <code>strs</code>.
		 *
		 * @param objectType
		 *            the class instantiated for the initial value.
		 * @param stringParams
		 *            the array of strings passed to the constructor.
		 */
		public static Initializer byNewWithParams(CtClass objectType, String[] stringParams)
		{
			NewInitializer i = new NewInitializer();
			i.objectType = objectType;
			i.stringParams = stringParams;
			i.withConstructorParams = true;
			return i;
		}

		/**
		 * Makes an initializer using a constructor parameter.
		 * <p>
		 * The initial value is the N-th parameter given to the constructor of
		 * the object including the field. If the constructor takes less than N
		 * parameters, the field is not initialized. If the field is static, it
		 * is never initialized.
		 *
		 * @param nth
		 *            the n-th (&gt;= 0) parameter is used as the initial value.
		 *            If nth is 0, then the first parameter is used.
		 */
		public static Initializer byParameter(int nth)
		{
			ParamInitializer i = new ParamInitializer();
			i.nthParam = nth;
			return i;
		}

		/**
		 * Makes an initializer that assigns a constant boolean value. The field
		 * must be boolean type.
		 */
		public static Initializer constant(boolean b)
		{
			return new IntInitializer(b ? 1 : 0);
		}

		/**
		 * Makes an initializer that assigns a constant double value. The field
		 * must be double type.
		 */
		public static Initializer constant(double d)
		{
			return new DoubleInitializer(d);
		}

		/**
		 * Makes an initializer that assigns a constant float value. The field
		 * must be float type.
		 */
		public static Initializer constant(float l)
		{
			return new FloatInitializer(l);
		}

		/**
		 * Makes an initializer that assigns a constant integer value. The field
		 * must be integer, short, char, or byte type.
		 */
		public static Initializer constant(int i)
		{
			return new IntInitializer(i);
		}

		/**
		 * Makes an initializer that assigns a constant long value. The field
		 * must be long type.
		 */
		public static Initializer constant(long l)
		{
			return new LongInitializer(l);
		}

		/**
		 * Makes an initializer that assigns a constant string value. The field
		 * must be <code>java.lang.String</code> type.
		 */
		public static Initializer constant(String s)
		{
			return new StringInitializer(s);
		}

		// Check whether this initializer is valid for the field type.
		// If it is invaild, this method throws an exception.
		void check(String desc) throws CannotCompileException
		{
		}

		// produce codes for initialization
		abstract int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException;

		// produce codes for initialization
		abstract int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException;

		// returns the index of CONSTANT_Integer_info etc
		// if the value is constant. Otherwise, 0.
		int getConstantValue(ConstPool cp, CtClass type)
		{
			return 0;
		}
	}

	static class IntInitializer extends Initializer
	{
		int	value;

		IntInitializer(int v)
		{
			this.value = v;
		}

		@Override
		void check(String desc) throws CannotCompileException
		{
			char c = desc.charAt(0);
			if (c != 'I' && c != 'S' && c != 'B' && c != 'C' && c != 'Z')
				throw new CannotCompileException("type mismatch");
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			code.addAload(0);
			code.addIconst(this.value);
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return 2; // stack size
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			code.addIconst(this.value);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return 1; // stack size
		}

		@Override
		int getConstantValue(ConstPool cp, CtClass type)
		{
			return cp.addIntegerInfo(this.value);
		}
	}

	static class LongInitializer extends Initializer
	{
		long	value;

		LongInitializer(long v)
		{
			this.value = v;
		}

		@Override
		void check(String desc) throws CannotCompileException
		{
			if (!desc.equals("J"))
				throw new CannotCompileException("type mismatch");
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			code.addAload(0);
			code.addLdc2w(this.value);
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return 3; // stack size
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			code.addLdc2w(this.value);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return 2; // stack size
		}

		@Override
		int getConstantValue(ConstPool cp, CtClass type)
		{
			if (type == CtClass.longType)
				return cp.addLongInfo(this.value);
			else
				return 0;
		}
	}

	/**
	 * A field initialized with the result of a static method call.
	 */
	static class MethodInitializer extends NewInitializer
	{
		String	methodName;

		// the method class is specified by objectType.

		MethodInitializer()
		{
		}

		/**
		 * Produces codes in which a new object is created and assigned to the
		 * field as the initial value.
		 */
		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			int stacksize;

			code.addAload(0);
			code.addAload(0);

			if (this.stringParams == null)
				stacksize = 2;
			else
				stacksize = this.compileStringParameter(code) + 2;

			if (this.withConstructorParams)
				stacksize += CtNewWrappedMethod.compileParameterList(code, parameters, 1);

			String typeDesc = Descriptor.of(type);
			String mDesc = this.getDescriptor() + typeDesc;
			code.addInvokestatic(this.objectType, this.methodName, mDesc);
			code.addPutfield(Bytecode.THIS, name, typeDesc);
			return stacksize;
		}

		/**
		 * Produces codes for a static field.
		 */
		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			String desc;

			int stacksize = 1;
			if (this.stringParams == null)
				desc = "()";
			else
			{
				desc = "([Ljava/lang/String;)";
				stacksize += this.compileStringParameter(code);
			}

			String typeDesc = Descriptor.of(type);
			code.addInvokestatic(this.objectType, this.methodName, desc + typeDesc);
			code.addPutstatic(Bytecode.THIS, name, typeDesc);
			return stacksize;
		}

		private String getDescriptor()
		{
			final String desc3 = "(Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)";

			if (this.stringParams == null)
				if (this.withConstructorParams)
					return "(Ljava/lang/Object;[Ljava/lang/Object;)";
				else
					return "(Ljava/lang/Object;)";
			else if (this.withConstructorParams)
				return desc3;
			else
				return "(Ljava/lang/Object;[Ljava/lang/String;)";
		}
	}

	static class MultiArrayInitializer extends Initializer
	{
		CtClass	type;
		int[]	dim;

		MultiArrayInitializer(CtClass t, int[] d)
		{
			this.type = t;
			this.dim = d;
		}

		@Override
		void check(String desc) throws CannotCompileException
		{
			if (desc.charAt(0) != '[')
				throw new CannotCompileException("type mismatch");
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			code.addAload(0);
			int s = code.addMultiNewarray(type, this.dim);
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return s + 1; // stack size
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			int s = code.addMultiNewarray(type, this.dim);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return s; // stack size
		}
	}

	/**
	 * A field initialized with an object created by the new operator.
	 */
	static class NewInitializer extends Initializer
	{
		CtClass		objectType;
		String[]	stringParams;
		boolean		withConstructorParams;

		NewInitializer()
		{
		}

		/**
		 * Produces codes in which a new object is created and assigned to the
		 * field as the initial value.
		 */
		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			int stacksize;

			code.addAload(0);
			code.addNew(this.objectType);
			code.add(Opcode.DUP);
			code.addAload(0);

			if (this.stringParams == null)
				stacksize = 4;
			else
				stacksize = this.compileStringParameter(code) + 4;

			if (this.withConstructorParams)
				stacksize += CtNewWrappedMethod.compileParameterList(code, parameters, 1);

			code.addInvokespecial(this.objectType, "<init>", this.getDescriptor());
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return stacksize;
		}

		/**
		 * Produces codes for a static field.
		 */
		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			String desc;

			code.addNew(this.objectType);
			code.add(Opcode.DUP);

			int stacksize = 2;
			if (this.stringParams == null)
				desc = "()V";
			else
			{
				desc = "([Ljava/lang/String;)V";
				stacksize += this.compileStringParameter(code);
			}

			code.addInvokespecial(this.objectType, "<init>", desc);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return stacksize;
		}

		protected final int compileStringParameter(Bytecode code) throws CannotCompileException
		{
			int nparam = this.stringParams.length;
			code.addIconst(nparam);
			code.addAnewarray(CtField.javaLangString);
			for (int j = 0; j < nparam; ++j)
			{
				code.add(Opcode.DUP); // dup
				code.addIconst(j); // iconst_<j>
				code.addLdc(this.stringParams[j]); // ldc ...
				code.add(Opcode.AASTORE); // aastore
			}

			return 4;
		}

		private String getDescriptor()
		{
			final String desc3 = "(Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)V";

			if (this.stringParams == null)
				if (this.withConstructorParams)
					return "(Ljava/lang/Object;[Ljava/lang/Object;)V";
				else
					return "(Ljava/lang/Object;)V";
			else if (this.withConstructorParams)
				return desc3;
			else
				return "(Ljava/lang/Object;[Ljava/lang/String;)V";
		}

	}

	/**
	 * A field initialized with a parameter passed to the constructor of the
	 * class containing that field.
	 */
	static class ParamInitializer extends Initializer
	{
		/**
		 * Computes the index of the local variable that the n-th parameter is
		 * assigned to.
		 *
		 * @param nth
		 *            n-th parameter
		 * @param params
		 *            list of parameter types
		 * @param isStatic
		 *            true if the method is static.
		 */
		static int nthParamToLocal(int nth, CtClass[] params, boolean isStatic)
		{
			CtClass longType = CtClass.longType;
			CtClass doubleType = CtClass.doubleType;
			int k;
			if (isStatic)
				k = 0;
			else
				k = 1; // 0 is THIS.

			for (int i = 0; i < nth; ++i)
			{
				CtClass type = params[i];
				if (type == longType || type == doubleType)
					k += 2;
				else
					++k;
			}

			return k;
		}

		int	nthParam;

		ParamInitializer()
		{
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			if (parameters != null && this.nthParam < parameters.length)
			{
				code.addAload(0);
				int nth = ParamInitializer.nthParamToLocal(this.nthParam, parameters, false);
				int s = code.addLoad(nth, type) + 1;
				code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
				return s; // stack size
			} else
				return 0; // do not initialize
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			return 0;
		}
	}

	static class PtreeInitializer extends CodeInitializer0
	{
		private ASTree	expression;

		PtreeInitializer(ASTree expr)
		{
			this.expression = expr;
		}

		@Override
		void compileExpr(Javac drv) throws CompileError
		{
			drv.compileExpr(this.expression);
		}

		@Override
		int getConstantValue(ConstPool cp, CtClass type)
		{
			return this.getConstantValue2(cp, type, this.expression);
		}
	}

	static class StringInitializer extends Initializer
	{
		String	value;

		StringInitializer(String v)
		{
			this.value = v;
		}

		@Override
		int compile(CtClass type, String name, Bytecode code, CtClass[] parameters, Javac drv) throws CannotCompileException
		{
			code.addAload(0);
			code.addLdc(this.value);
			code.addPutfield(Bytecode.THIS, name, Descriptor.of(type));
			return 2; // stack size
		}

		@Override
		int compileIfStatic(CtClass type, String name, Bytecode code, Javac drv) throws CannotCompileException
		{
			code.addLdc(this.value);
			code.addPutstatic(Bytecode.THIS, name, Descriptor.of(type));
			return 1; // stack size
		}

		@Override
		int getConstantValue(ConstPool cp, CtClass type)
		{
			if (type.getName().equals(CtField.javaLangString))
				return cp.addStringInfo(this.value);
			else
				return 0;
		}
	}

	static final String	javaLangString	= "java.lang.String";

	/**
	 * Compiles the given source code and creates a field. Examples of the
	 * source code are:
	 *
	 * <pre>
	 * "public String name;"
	 * "public int k = 3;"
	 * </pre>
	 * <p>
	 * Note that the source code ends with <code>';'</code> (semicolon).
	 *
	 * @param src
	 *            the source text.
	 * @param declaring
	 *            the class to which the created field is added.
	 */
	public static CtField make(String src, CtClass declaring) throws CannotCompileException
	{
		Javac compiler = new Javac(declaring);
		try
		{
			CtMember obj = compiler.compile(src);
			if (obj instanceof CtField)
				return (CtField) obj; // an instance of Javac.CtFieldWithInit
		} catch (CompileError e)
		{
			throw new CannotCompileException(e);
		}

		throw new CannotCompileException("not a field");
	}

	protected FieldInfo	fieldInfo;

	/**
	 * Creates a <code>CtField</code> object. The created field must be added to
	 * a class with <code>CtClass.addField()</code>. An initial value of the
	 * field is specified by a <code>CtField.Initializer</code> object.
	 * <p>
	 * If getter and setter methods are needed, call
	 * <code>CtNewMethod.getter()</code> and <code>CtNewMethod.setter()</code>.
	 *
	 * @param type
	 *            field type
	 * @param name
	 *            field name
	 * @param declaring
	 *            the class to which the field will be added.
	 * @see CtClass#addField(CtField)
	 * @see CtNewMethod#getter(String,CtField)
	 * @see CtNewMethod#setter(String,CtField)
	 * @see CtField.Initializer
	 */
	public CtField(CtClass type, String name, CtClass declaring) throws CannotCompileException
	{
		this(Descriptor.of(type), name, declaring);
	}

	/**
	 * Creates a copy of the given field. The created field must be added to a
	 * class with <code>CtClass.addField()</code>. An initial value of the field
	 * is specified by a <code>CtField.Initializer</code> object.
	 * <p>
	 * If getter and setter methods are needed, call
	 * <code>CtNewMethod.getter()</code> and <code>CtNewMethod.setter()</code>.
	 *
	 * @param src
	 *            the original field
	 * @param declaring
	 *            the class to which the field will be added.
	 * @see CtNewMethod#getter(String,CtField)
	 * @see CtNewMethod#setter(String,CtField)
	 * @see CtField.Initializer
	 */
	public CtField(CtField src, CtClass declaring) throws CannotCompileException
	{
		this(src.fieldInfo.getDescriptor(), src.fieldInfo.getName(), declaring);
		java.util.ListIterator iterator = src.fieldInfo.getAttributes().listIterator();
		FieldInfo fi = this.fieldInfo;
		fi.setAccessFlags(src.fieldInfo.getAccessFlags());
		ConstPool cp = fi.getConstPool();
		while (iterator.hasNext())
		{
			AttributeInfo ainfo = (AttributeInfo) iterator.next();
			fi.addAttribute(ainfo.copy(cp, null));
		}
	}

	CtField(FieldInfo fi, CtClass clazz)
	{
		super(clazz);
		this.fieldInfo = fi;
	}

	private CtField(String typeDesc, String name, CtClass clazz) throws CannotCompileException
	{
		super(clazz);
		ClassFile cf = clazz.getClassFile2();
		if (cf == null)
			throw new CannotCompileException("bad declaring class: " + clazz.getName());

		this.fieldInfo = new FieldInfo(cf.getConstPool(), name, typeDesc);
	}

	@Override
	protected void extendToString(StringBuffer buffer)
	{
		buffer.append(' ');
		buffer.append(this.getName());
		buffer.append(' ');
		buffer.append(this.fieldInfo.getDescriptor());
	}

	/**
	 * Returns the annotation if the class has the specified annotation class.
	 * For example, if an annotation <code>@Author</code> is associated with
	 * this field, an <code>Author</code> object is returned. The member values
	 * can be obtained by calling methods on the <code>Author</code> object.
	 *
	 * @param clz
	 *            the annotation class.
	 * @return the annotation if found, otherwise <code>null</code>.
	 * @since 3.11
	 */
	@Override
	public Object getAnnotation(Class clz) throws ClassNotFoundException
	{
		FieldInfo fi = this.getFieldInfo2();
		AnnotationsAttribute ainfo = (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.invisibleTag);
		AnnotationsAttribute ainfo2 = (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.visibleTag);
		return CtClassType.getAnnotationType(clz, this.getDeclaringClass().getClassPool(), ainfo, ainfo2);
	}

	/**
	 * Returns the annotations associated with this field.
	 *
	 * @return an array of annotation-type objects.
	 * @see #getAvailableAnnotations()
	 * @since 3.1
	 */
	@Override
	public Object[] getAnnotations() throws ClassNotFoundException
	{
		return this.getAnnotations(false);
	}

	private Object[] getAnnotations(boolean ignoreNotFound) throws ClassNotFoundException
	{
		FieldInfo fi = this.getFieldInfo2();
		AnnotationsAttribute ainfo = (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.invisibleTag);
		AnnotationsAttribute ainfo2 = (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.visibleTag);
		return CtClassType.toAnnotationType(ignoreNotFound, this.getDeclaringClass().getClassPool(), ainfo, ainfo2);
	}

	/**
	 * Obtains an attribute with the given name. If that attribute is not found
	 * in the class file, this method returns null.
	 * <p>
	 * Note that an attribute is a data block specified by the class file
	 * format. See {@link javassist.bytecode.AttributeInfo}.
	 *
	 * @param name
	 *            attribute name
	 */
	@Override
	public byte[] getAttribute(String name)
	{
		AttributeInfo ai = this.fieldInfo.getAttribute(name);
		if (ai == null)
			return null;
		else
			return ai.get();
	}

	/**
	 * Returns the annotations associated with this field. If any annotations
	 * are not on the classpath, they are not included in the returned array.
	 *
	 * @return an array of annotation-type objects.
	 * @see #getAnnotations()
	 * @since 3.3
	 */
	@Override
	public Object[] getAvailableAnnotations()
	{
		try
		{
			return this.getAnnotations(true);
		} catch (ClassNotFoundException e)
		{
			throw new RuntimeException("Unexpected exception", e);
		}
	}

	/**
	 * Returns the value of this field if it is a constant field. This method
	 * works only if the field type is a primitive type or <code>String</code>
	 * type. Otherwise, it returns <code>null</code>. A constant field is
	 * <code>static</code> and <code>final</code>.
	 *
	 * @return a <code>Integer</code>, <code>Long</code>, <code>Float</code>,
	 *         <code>Double</code>, <code>Boolean</code>, or <code>String</code>
	 *         object representing the constant value. <code>null</code> if it
	 *         is not a constant field or if the field type is not a primitive
	 *         type or <code>String</code>.
	 */
	public Object getConstantValue()
	{
		// When this method is modified,
		// see also getConstantFieldValue() in TypeChecker.

		int index = this.fieldInfo.getConstantValue();
		if (index == 0)
			return null;

		ConstPool cp = this.fieldInfo.getConstPool();
		switch (cp.getTag(index))
		{
			case ConstPool.CONST_Long:
				return new Long(cp.getLongInfo(index));
			case ConstPool.CONST_Float:
				return new Float(cp.getFloatInfo(index));
			case ConstPool.CONST_Double:
				return new Double(cp.getDoubleInfo(index));
			case ConstPool.CONST_Integer:
				int value = cp.getIntegerInfo(index);
				// "Z" means boolean type.
				if ("Z".equals(this.fieldInfo.getDescriptor()))
					return new Boolean(value != 0);
				else
					return new Integer(value);
			case ConstPool.CONST_String:
				return cp.getStringInfo(index);
			default:
				throw new RuntimeException("bad tag: " + cp.getTag(index) + " at " + index);
		}
	}

	/**
	 * Returns the class declaring the field.
	 */
	@Override
	public CtClass getDeclaringClass()
	{
		// this is redundant but for javadoc.
		return super.getDeclaringClass();
	}

	/**
	 * Returns the FieldInfo representing the field in the class file.
	 */
	public FieldInfo getFieldInfo()
	{
		this.declaringClass.checkModify();
		return this.fieldInfo;
	}

	/**
	 * Returns the FieldInfo representing the field in the class file (read
	 * only). Normal applications do not need calling this method. Use
	 * <code>getFieldInfo()</code>.
	 * <p>
	 * The <code>FieldInfo</code> object obtained by this method is read only.
	 * Changes to this object might not be reflected on a class file generated
	 * by <code>toBytecode()</code>, <code>toClass()</code>, etc in
	 * <code>CtClass</code>.
	 * <p>
	 * This method is available even if the <code>CtClass</code> containing this
	 * field is frozen. However, if the class is frozen, the
	 * <code>FieldInfo</code> might be also pruned.
	 *
	 * @see #getFieldInfo()
	 * @see CtClass#isFrozen()
	 * @see CtClass#prune()
	 */
	public FieldInfo getFieldInfo2()
	{
		return this.fieldInfo;
	}

	// inner classes

	/**
	 * Returns the generic signature of the field. It represents a type
	 * including type variables.
	 *
	 * @see SignatureAttribute#toFieldSignature(String)
	 * @since 3.17
	 */
	@Override
	public String getGenericSignature()
	{
		SignatureAttribute sa = (SignatureAttribute) this.fieldInfo.getAttribute(SignatureAttribute.tag);
		return sa == null ? null : sa.getSignature();
	}

	/*
	 * Called by CtClassType.addField().
	 */
	Initializer getInit()
	{
		ASTree tree = this.getInitAST();
		if (tree == null)
			return null;
		else
			return Initializer.byExpr(tree);
	}

	/*
	 * Javac.CtFieldWithInit overrides.
	 */
	protected ASTree getInitAST()
	{
		return null;
	}

	/**
	 * Returns the encoded modifiers of the field.
	 *
	 * @see Modifier
	 */
	@Override
	public int getModifiers()
	{
		return AccessFlag.toModifier(this.fieldInfo.getAccessFlags());
	}

	/**
	 * Returns the name of the field.
	 */
	@Override
	public String getName()
	{
		return this.fieldInfo.getName();
	}

	/**
	 * Returns the character string representing the type of the field. The
	 * field signature is represented by a character string called a field
	 * descriptor, which is defined in the JVM specification. If two fields have
	 * the same type, <code>getSignature()</code> returns the same string.
	 * <p>
	 * Note that the returned string is not the type signature contained in the
	 * <code>SignatureAttirbute</code>. It is a descriptor.
	 *
	 * @see javassist.bytecode.Descriptor
	 * @see #getGenericSignature()
	 */
	@Override
	public String getSignature()
	{
		return this.fieldInfo.getDescriptor();
	}

	/**
	 * Returns the type of the field.
	 */
	public CtClass getType() throws NotFoundException
	{
		return Descriptor.toCtClass(this.fieldInfo.getDescriptor(), this.declaringClass.getClassPool());
	}

	/**
	 * Returns true if the class has the specified annotation class.
	 *
	 * @param clz
	 *            the annotation class.
	 * @return <code>true</code> if the annotation is found, otherwise
	 *         <code>false</code>.
	 * @since 3.11
	 */
	@Override
	public boolean hasAnnotation(Class clz)
	{
		FieldInfo fi = this.getFieldInfo2();
		AnnotationsAttribute ainfo = (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.invisibleTag);
		AnnotationsAttribute ainfo2 = (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.visibleTag);
		return CtClassType.hasAnnotationType(clz, this.getDeclaringClass().getClassPool(), ainfo, ainfo2);
	}

	/**
	 * Adds an attribute. The attribute is saved in the class file.
	 * <p>
	 * Note that an attribute is a data block specified by the class file
	 * format. See {@link javassist.bytecode.AttributeInfo}.
	 *
	 * @param name
	 *            attribute name
	 * @param data
	 *            attribute value
	 */
	@Override
	public void setAttribute(String name, byte[] data)
	{
		this.declaringClass.checkModify();
		this.fieldInfo.addAttribute(new AttributeInfo(this.fieldInfo.getConstPool(), name, data));
	}

	/**
	 * Set the generic signature of the field. It represents a type including
	 * type variables. See {@link javassist.CtClass#setGenericSignature(String)}
	 * for a code sample.
	 *
	 * @param sig
	 *            a new generic signature.
	 * @see javassist.bytecode.SignatureAttribute.ObjectType#encode()
	 * @since 3.17
	 */
	@Override
	public void setGenericSignature(String sig)
	{
		this.declaringClass.checkModify();
		this.fieldInfo.addAttribute(new SignatureAttribute(this.fieldInfo.getConstPool(), sig));
	}

	/**
	 * Sets the encoded modifiers of the field.
	 *
	 * @see Modifier
	 */
	@Override
	public void setModifiers(int mod)
	{
		this.declaringClass.checkModify();
		this.fieldInfo.setAccessFlags(AccessFlag.of(mod));
	}

	/**
	 * Changes the name of the field.
	 */
	public void setName(String newName)
	{
		this.declaringClass.checkModify();
		this.fieldInfo.setName(newName);
	}

	/**
	 * Sets the type of the field.
	 */
	public void setType(CtClass clazz)
	{
		this.declaringClass.checkModify();
		this.fieldInfo.setDescriptor(Descriptor.of(clazz));
	}

	/**
	 * Returns a String representation of the object.
	 */
	@Override
	public String toString()
	{
		return this.getDeclaringClass().getName() + "." + this.getName() + ":" + this.fieldInfo.getDescriptor();
	}
}
