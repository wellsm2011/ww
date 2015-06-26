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

package javassist.bytecode;

import javassist.CtClass;
import javassist.CtPrimitiveType;

/**
 * A utility class for producing a bytecode sequence.
 * <p>
 * A <code>Bytecode</code> object is an unbounded array containing bytecode. For
 * example,
 *
 * <pre>
 * ConstPool cp = ...;    // constant pool table
 * Bytecode b = new Bytecode(cp, 1, 0);
 * b.addIconst(3);
 * b.addReturn(CtClass.intType);
 * CodeAttribute ca = b.toCodeAttribute();
 * </pre>
 * <p>
 * This program produces a Code attribute including a bytecode sequence:
 *
 * <pre>
 * iconst_3
 * ireturn
 * </pre>
 *
 * @see ConstPool
 * @see CodeAttribute
 */
public class Bytecode extends ByteVector implements Cloneable, Opcode
{
	/**
	 * Represents the <code>CtClass</code> file using the constant pool table
	 * given to this <code>Bytecode</code> object.
	 */
	public static final CtClass	THIS	= ConstPool.THIS;

	ConstPool					constPool;
	int							maxStack, maxLocals;
	ExceptionTable				tryblocks;
	private int					stackDepth;

	/**
	 * Constructs a <code>Bytecode</code> object with an empty bytecode
	 * sequence. The initial values of <code>max_stack</code> and
	 * <code>max_locals</code> are zero.
	 *
	 * @param cp
	 *            constant pool table.
	 * @see Bytecode#setMaxStack(int)
	 * @see Bytecode#setMaxLocals(int)
	 */
	public Bytecode(ConstPool cp)
	{
		this(cp, 0, 0);
	}

	/**
	 * Constructs a <code>Bytecode</code> object with an empty bytecode
	 * sequence.
	 * <p>
	 * The parameters <code>stacksize</code> and <code>localvars</code> specify
	 * initial values of <code>max_stack</code> and <code>max_locals</code>.
	 * They can be changed later.
	 *
	 * @param cp
	 *            constant pool table.
	 * @param stacksize
	 *            <code>max_stack</code>.
	 * @param localvars
	 *            <code>max_locals</code>.
	 */
	public Bytecode(ConstPool cp, int stacksize, int localvars)
	{
		this.constPool = cp;
		this.maxStack = stacksize;
		this.maxLocals = localvars;
		this.tryblocks = new ExceptionTable(cp);
		this.stackDepth = 0;
	}

	/**
	 * Appends an 8bit value to the end of the bytecode sequence.
	 */
	@Override
	public void add(int code)
	{
		super.add(code);
	}

	/**
	 * Appends a 32bit value to the end of the bytecode sequence.
	 */
	public void add32bit(int value)
	{
		this.add(value >> 24, value >> 16, value >> 8, value);
	}

	/**
	 * Appends ALOAD or (WIDE) ALOAD_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addAload(int n)
	{
		if (n < 4)
			this.addOpcode(42 + n); // aload_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.ALOAD); // aload
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.ALOAD);
			this.addIndex(n);
		}
	}

	/**
	 * Appends ICONST and ANEWARRAY.
	 *
	 * @param clazz
	 *            the elememnt type.
	 * @param length
	 *            the array length.
	 */
	public void addAnewarray(CtClass clazz, int length)
	{
		this.addIconst(length);
		this.addOpcode(Opcode.ANEWARRAY);
		this.addIndex(this.constPool.addClassInfo(clazz));
	}

	/**
	 * Appends ANEWARRAY.
	 *
	 * @param classname
	 *            the qualified class name of the element type.
	 */
	public void addAnewarray(String classname)
	{
		this.addOpcode(Opcode.ANEWARRAY);
		this.addIndex(this.constPool.addClassInfo(classname));
	}

	/**
	 * Appends ASTORE or (WIDE) ASTORE_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addAstore(int n)
	{
		if (n < 4)
			this.addOpcode(75 + n); // astore_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.ASTORE); // astore
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.ASTORE);
			this.addIndex(n);
		}
	}

	/**
	 * Appends CHECKCAST.
	 *
	 * @param c
	 *            the type.
	 */
	public void addCheckcast(CtClass c)
	{
		this.addOpcode(Opcode.CHECKCAST);
		this.addIndex(this.constPool.addClassInfo(c));
	}

	/**
	 * Appends CHECKCAST.
	 *
	 * @param classname
	 *            a fully-qualified class name.
	 */
	public void addCheckcast(String classname)
	{
		this.addOpcode(Opcode.CHECKCAST);
		this.addIndex(this.constPool.addClassInfo(classname));
	}

	/**
	 * Appends an instruction for pushing zero or null on the stack. If the type
	 * is void, this method does not append any instruction.
	 *
	 * @param type
	 *            the type of the zero value (or null).
	 */
	public void addConstZero(CtClass type)
	{
		if (type.isPrimitive())
		{
			if (type == CtClass.longType)
				this.addOpcode(Opcode.LCONST_0);
			else if (type == CtClass.floatType)
				this.addOpcode(Opcode.FCONST_0);
			else if (type == CtClass.doubleType)
				this.addOpcode(Opcode.DCONST_0);
			else if (type == CtClass.voidType)
				throw new RuntimeException("void type?");
			else
				this.addOpcode(Opcode.ICONST_0);
		} else
			this.addOpcode(Opcode.ACONST_NULL);
	}

	/**
	 * Appends DCONST or DCONST_&lt;n&gt;
	 *
	 * @param d
	 *            the pushed double constant.
	 */
	public void addDconst(double d)
	{
		if (d == 0.0 || d == 1.0)
			this.addOpcode(14 + (int) d); // dconst_<n>
		else
			this.addLdc2w(d);
	}

	/**
	 * Appends DLOAD or (WIDE) DLOAD_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addDload(int n)
	{
		if (n < 4)
			this.addOpcode(38 + n); // dload_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.DLOAD); // dload
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.DLOAD);
			this.addIndex(n);
		}
	}

	/**
	 * Appends DSTORE or (WIDE) DSTORE_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addDstore(int n)
	{
		if (n < 4)
			this.addOpcode(71 + n); // dstore_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.DSTORE); // dstore
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.DSTORE);
			this.addIndex(n);
		}
	}

	/**
	 * Adds a new entry of <code>exception_table</code>.
	 */
	public void addExceptionHandler(int start, int end, int handler, CtClass type)
	{
		this.addExceptionHandler(start, end, handler, this.constPool.addClassInfo(type));
	}

	/**
	 * Adds a new entry of <code>exception_table</code>.
	 */
	public void addExceptionHandler(int start, int end, int handler, int type)
	{
		this.tryblocks.add(start, end, handler, type);
	}

	/**
	 * Adds a new entry of <code>exception_table</code>.
	 *
	 * @param type
	 *            the fully-qualified name of a throwable class.
	 */
	public void addExceptionHandler(int start, int end, int handler, String type)
	{
		this.addExceptionHandler(start, end, handler, this.constPool.addClassInfo(type));
	}

	/**
	 * Appends FCONST or FCONST_&lt;n&gt;
	 *
	 * @param f
	 *            the pushed float constant.
	 */
	public void addFconst(float f)
	{
		if (f == 0.0f || f == 1.0f || f == 2.0f)
			this.addOpcode(11 + (int) f); // fconst_<n>
		else
			this.addLdc(this.constPool.addFloatInfo(f));
	}

	/**
	 * Appends FLOAD or (WIDE) FLOAD_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addFload(int n)
	{
		if (n < 4)
			this.addOpcode(34 + n); // fload_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.FLOAD); // fload
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.FLOAD);
			this.addIndex(n);
		}
	}

	/**
	 * Appends FSTORE or FSTORE_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addFstore(int n)
	{
		if (n < 4)
			this.addOpcode(67 + n); // fstore_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.FSTORE); // fstore
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.FSTORE);
			this.addIndex(n);
		}
	}

	/**
	 * Appends the length-byte gap to the end of the bytecode sequence.
	 *
	 * @param length
	 *            the gap length in byte.
	 */
	@Override
	public void addGap(int length)
	{
		super.addGap(length);
	}

	/**
	 * Appends GETFIELD.
	 *
	 * @param c
	 *            the class.
	 * @param name
	 *            the field name.
	 * @param type
	 *            the descriptor of the field type.
	 * @see Descriptor#of(CtClass)
	 */
	public void addGetfield(CtClass c, String name, String type)
	{
		this.add(Opcode.GETFIELD);
		int ci = this.constPool.addClassInfo(c);
		this.addIndex(this.constPool.addFieldrefInfo(ci, name, type));
		this.growStack(Descriptor.dataSize(type) - 1);
	}

	/**
	 * Appends GETFIELD.
	 *
	 * @param c
	 *            the fully-qualified class name.
	 * @param name
	 *            the field name.
	 * @param type
	 *            the descriptor of the field type.
	 * @see Descriptor#of(CtClass)
	 */
	public void addGetfield(String c, String name, String type)
	{
		this.add(Opcode.GETFIELD);
		int ci = this.constPool.addClassInfo(c);
		this.addIndex(this.constPool.addFieldrefInfo(ci, name, type));
		this.growStack(Descriptor.dataSize(type) - 1);
	}

	/**
	 * Appends GETSTATIC.
	 *
	 * @param c
	 *            the class
	 * @param name
	 *            the field name
	 * @param type
	 *            the descriptor of the field type.
	 * @see Descriptor#of(CtClass)
	 */
	public void addGetstatic(CtClass c, String name, String type)
	{
		this.add(Opcode.GETSTATIC);
		int ci = this.constPool.addClassInfo(c);
		this.addIndex(this.constPool.addFieldrefInfo(ci, name, type));
		this.growStack(Descriptor.dataSize(type));
	}

	/**
	 * Appends GETSTATIC.
	 *
	 * @param c
	 *            the fully-qualified class name
	 * @param name
	 *            the field name
	 * @param type
	 *            the descriptor of the field type.
	 * @see Descriptor#of(CtClass)
	 */
	public void addGetstatic(String c, String name, String type)
	{
		this.add(Opcode.GETSTATIC);
		int ci = this.constPool.addClassInfo(c);
		this.addIndex(this.constPool.addFieldrefInfo(ci, name, type));
		this.growStack(Descriptor.dataSize(type));
	}

	/**
	 * Appends ICONST or ICONST_&lt;n&gt;
	 *
	 * @param n
	 *            the pushed integer constant.
	 */
	public void addIconst(int n)
	{
		if (n < 6 && -2 < n)
			this.addOpcode(3 + n); // iconst_<i> -1..5
		else if (n <= 127 && -128 <= n)
		{
			this.addOpcode(16); // bipush
			this.add(n);
		} else if (n <= 32767 && -32768 <= n)
		{
			this.addOpcode(17); // sipush
			this.add(n >> 8);
			this.add(n);
		} else
			this.addLdc(this.constPool.addIntegerInfo(n));
	}

	/**
	 * Appends ILOAD or (WIDE) ILOAD_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addIload(int n)
	{
		if (n < 4)
			this.addOpcode(26 + n); // iload_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.ILOAD); // iload
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.ILOAD);
			this.addIndex(n);
		}
	}

	/**
	 * Appends a 16bit value to the end of the bytecode sequence. It never
	 * changes the current stack depth.
	 */
	public void addIndex(int index)
	{
		this.add(index >> 8, index);
	}

	/**
	 * Appends INSTANCEOF.
	 *
	 * @param classname
	 *            the class name.
	 */
	public void addInstanceof(String classname)
	{
		this.addOpcode(Opcode.INSTANCEOF);
		this.addIndex(this.constPool.addClassInfo(classname));
	}

	/**
	 * Appends INVOKEDYNAMIC.
	 *
	 * @param bootstrap
	 *            an index into the <code>bootstrap_methods</code> array of the
	 *            bootstrap method table.
	 * @param name
	 *            the method name.
	 * @param desc
	 *            the method descriptor.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 * @since 3.17
	 */
	public void addInvokedynamic(int bootstrap, String name, String desc)
	{
		int nt = this.constPool.addNameAndTypeInfo(name, desc);
		int dyn = this.constPool.addInvokeDynamicInfo(bootstrap, nt);
		this.add(Opcode.INVOKEDYNAMIC);
		this.addIndex(dyn);
		this.add(0, 0);
		this.growStack(Descriptor.dataSize(desc)); // assume
													// ConstPool#REF_invokeStatic
	}

	/**
	 * Appends INVOKEINTERFACE.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name
	 * @param returnType
	 *            the return type.
	 * @param paramTypes
	 *            the parameter types.
	 * @param count
	 *            the count operand of the instruction.
	 */
	public void addInvokeinterface(CtClass clazz, String name, CtClass returnType, CtClass[] paramTypes, int count)
	{
		String desc = Descriptor.ofMethod(returnType, paramTypes);
		this.addInvokeinterface(clazz, name, desc, count);
	}

	/**
	 * Appends INVOKEINTERFACE.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @param count
	 *            the count operand of the instruction.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokeinterface(CtClass clazz, String name, String desc, int count)
	{
		this.addInvokeinterface(this.constPool.addClassInfo(clazz), name, desc, count);
	}

	/**
	 * Appends INVOKEINTERFACE.
	 *
	 * @param clazz
	 *            the index of <code>CONSTANT_Class_info</code> structure.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @param count
	 *            the count operand of the instruction.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokeinterface(int clazz, String name, String desc, int count)
	{
		this.add(Opcode.INVOKEINTERFACE);
		this.addIndex(this.constPool.addInterfaceMethodrefInfo(clazz, name, desc));
		this.add(count);
		this.add(0);
		this.growStack(Descriptor.dataSize(desc) - 1);
	}

	/**
	 * Appends INVOKEINTERFACE.
	 *
	 * @param classname
	 *            the fully-qualified class name.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @param count
	 *            the count operand of the instruction.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokeinterface(String classname, String name, String desc, int count)
	{
		this.addInvokeinterface(this.constPool.addClassInfo(classname), name, desc, count);
	}

	/**
	 * Appends INVOKESPECIAL.
	 *
	 * @param isInterface
	 *            true if the invoked method is a default method declared in an
	 *            interface.
	 * @param clazz
	 *            the index of <code>CONSTANT_Class_info</code> structure.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 * @see Descriptor#ofConstructor(CtClass[])
	 */
	public void addInvokespecial(boolean isInterface, int clazz, String name, String desc)
	{
		this.add(Opcode.INVOKESPECIAL);
		int index;
		if (isInterface)
			index = this.constPool.addInterfaceMethodrefInfo(clazz, name, desc);
		else
			index = this.constPool.addMethodrefInfo(clazz, name, desc);

		this.addIndex(index);
		this.growStack(Descriptor.dataSize(desc) - 1);
	}

	/**
	 * Appends INVOKESPECIAL.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name.
	 * @param returnType
	 *            the return type.
	 * @param paramTypes
	 *            the parameter types.
	 */
	public void addInvokespecial(CtClass clazz, String name, CtClass returnType, CtClass[] paramTypes)
	{
		String desc = Descriptor.ofMethod(returnType, paramTypes);
		this.addInvokespecial(clazz, name, desc);
	}

	/**
	 * Appends INVOKESPECIAL.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 * @see Descriptor#ofConstructor(CtClass[])
	 */
	public void addInvokespecial(CtClass clazz, String name, String desc)
	{
		boolean isInterface = clazz == null ? false : clazz.isInterface();
		this.addInvokespecial(isInterface, this.constPool.addClassInfo(clazz), name, desc);
	}

	/**
	 * Appends INVOKESPECIAL. The invoked method must not be a default method
	 * declared in an interface.
	 *
	 * @param clazz
	 *            the index of <code>CONSTANT_Class_info</code> structure.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 * @see Descriptor#ofConstructor(CtClass[])
	 */
	public void addInvokespecial(int clazz, String name, String desc)
	{
		this.addInvokespecial(false, clazz, name, desc);
	}

	/**
	 * Appends INVOKESPECIAL. The invoked method must not be a default method
	 * declared in an interface.
	 *
	 * @param clazz
	 *            the fully-qualified class name.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 * @see Descriptor#ofConstructor(CtClass[])
	 */
	public void addInvokespecial(String clazz, String name, String desc)
	{
		this.addInvokespecial(false, this.constPool.addClassInfo(clazz), name, desc);
	}

	/**
	 * Appends INVOKESTATIC.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name
	 * @param returnType
	 *            the return type.
	 * @param paramTypes
	 *            the parameter types.
	 */
	public void addInvokestatic(CtClass clazz, String name, CtClass returnType, CtClass[] paramTypes)
	{
		String desc = Descriptor.ofMethod(returnType, paramTypes);
		this.addInvokestatic(clazz, name, desc);
	}

	/**
	 * Appends INVOKESTATIC.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokestatic(CtClass clazz, String name, String desc)
	{
		this.addInvokestatic(this.constPool.addClassInfo(clazz), name, desc);
	}

	/**
	 * Appends INVOKESTATIC.
	 *
	 * @param clazz
	 *            the index of <code>CONSTANT_Class_info</code> structure.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokestatic(int clazz, String name, String desc)
	{
		this.add(Opcode.INVOKESTATIC);
		this.addIndex(this.constPool.addMethodrefInfo(clazz, name, desc));
		this.growStack(Descriptor.dataSize(desc));
	}

	/**
	 * Appends INVOKESTATIC.
	 *
	 * @param classname
	 *            the fully-qualified class name.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokestatic(String classname, String name, String desc)
	{
		this.addInvokestatic(this.constPool.addClassInfo(classname), name, desc);
	}

	/**
	 * Appends INVOKEVIRTUAL.
	 * <p>
	 * The specified method must not be an inherited method. It must be directly
	 * declared in the class specified in <code>clazz</code>.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name
	 * @param returnType
	 *            the return type.
	 * @param paramTypes
	 *            the parameter types.
	 */
	public void addInvokevirtual(CtClass clazz, String name, CtClass returnType, CtClass[] paramTypes)
	{
		String desc = Descriptor.ofMethod(returnType, paramTypes);
		this.addInvokevirtual(clazz, name, desc);
	}

	/**
	 * Appends INVOKEVIRTUAL.
	 * <p>
	 * The specified method must not be an inherited method. It must be directly
	 * declared in the class specified in <code>clazz</code>.
	 *
	 * @param clazz
	 *            the target class.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokevirtual(CtClass clazz, String name, String desc)
	{
		this.addInvokevirtual(this.constPool.addClassInfo(clazz), name, desc);
	}

	/**
	 * Appends INVOKEVIRTUAL.
	 * <p>
	 * The specified method must not be an inherited method. It must be directly
	 * declared in the class specified by <code>clazz</code>.
	 *
	 * @param clazz
	 *            the index of <code>CONSTANT_Class_info</code> structure.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokevirtual(int clazz, String name, String desc)
	{
		this.add(Opcode.INVOKEVIRTUAL);
		this.addIndex(this.constPool.addMethodrefInfo(clazz, name, desc));
		this.growStack(Descriptor.dataSize(desc) - 1);
	}

	/**
	 * Appends INVOKEVIRTUAL.
	 * <p>
	 * The specified method must not be an inherited method. It must be directly
	 * declared in the class specified in <code>classname</code>.
	 *
	 * @param classname
	 *            the fully-qualified class name.
	 * @param name
	 *            the method name
	 * @param desc
	 *            the descriptor of the method signature.
	 * @see Descriptor#ofMethod(CtClass,CtClass[])
	 */
	public void addInvokevirtual(String classname, String name, String desc)
	{
		this.addInvokevirtual(this.constPool.addClassInfo(classname), name, desc);
	}

	/**
	 * Appends ISTORE or (WIDE) ISTORE_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addIstore(int n)
	{
		if (n < 4)
			this.addOpcode(59 + n); // istore_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.ISTORE); // istore
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.ISTORE);
			this.addIndex(n);
		}
	}

	/**
	 * Appends LCONST or LCONST_&lt;n&gt;
	 *
	 * @param n
	 *            the pushed long integer constant.
	 */
	public void addLconst(long n)
	{
		if (n == 0 || n == 1)
			this.addOpcode(9 + (int) n); // lconst_<n>
		else
			this.addLdc2w(n);
	}

	/**
	 * Appends LDC or LDC_W.
	 *
	 * @param i
	 *            index into the constant pool.
	 */
	public void addLdc(int i)
	{
		if (i > 0xFF)
		{
			this.addOpcode(Opcode.LDC_W);
			this.addIndex(i);
		} else
		{
			this.addOpcode(Opcode.LDC);
			this.add(i);
		}
	}

	/**
	 * Appends LDC or LDC_W. The pushed item is a <code>String</code> object.
	 *
	 * @param s
	 *            the character string pushed by LDC or LDC_W.
	 */
	public void addLdc(String s)
	{
		this.addLdc(this.constPool.addStringInfo(s));
	}

	/**
	 * Appends LDC2_W. The pushed item is a double value.
	 */
	public void addLdc2w(double d)
	{
		this.addOpcode(Opcode.LDC2_W);
		this.addIndex(this.constPool.addDoubleInfo(d));
	}

	/**
	 * Appends LDC2_W. The pushed item is a long value.
	 */
	public void addLdc2w(long l)
	{
		this.addOpcode(Opcode.LDC2_W);
		this.addIndex(this.constPool.addLongInfo(l));
	}

	/**
	 * Appends LLOAD or (WIDE) LLOAD_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addLload(int n)
	{
		if (n < 4)
			this.addOpcode(30 + n); // lload_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.LLOAD); // lload
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.LLOAD);
			this.addIndex(n);
		}
	}

	/**
	 * Appends an instruction for loading a value from the local variable at the
	 * index <code>n</code>.
	 *
	 * @param n
	 *            the index.
	 * @param type
	 *            the type of the loaded value.
	 * @return the size of the value (1 or 2 word).
	 */
	public int addLoad(int n, CtClass type)
	{
		if (type.isPrimitive())
		{
			if (type == CtClass.booleanType || type == CtClass.charType || type == CtClass.byteType || type == CtClass.shortType || type == CtClass.intType)
				this.addIload(n);
			else if (type == CtClass.longType)
			{
				this.addLload(n);
				return 2;
			} else if (type == CtClass.floatType)
				this.addFload(n);
			else if (type == CtClass.doubleType)
			{
				this.addDload(n);
				return 2;
			} else
				throw new RuntimeException("void type?");
		} else
			this.addAload(n);

		return 1;
	}

	/**
	 * Appends instructions for loading all the parameters onto the operand
	 * stack.
	 *
	 * @param offset
	 *            the index of the first parameter. It is 0 if the method is
	 *            static. Otherwise, it is 1.
	 */
	public int addLoadParameters(CtClass[] params, int offset)
	{
		int stacksize = 0;
		if (params != null)
		{
			int n = params.length;
			for (int i = 0; i < n; ++i)
				stacksize += this.addLoad(stacksize + offset, params[i]);
		}

		return stacksize;
	}

	/**
	 * Appends LSTORE or LSTORE_&lt;n&gt;
	 *
	 * @param n
	 *            an index into the local variable array.
	 */
	public void addLstore(int n)
	{
		if (n < 4)
			this.addOpcode(63 + n); // lstore_<n>
		else if (n < 0x100)
		{
			this.addOpcode(Opcode.LSTORE); // lstore
			this.add(n);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.LSTORE);
			this.addIndex(n);
		}
	}

	/**
	 * Appends MULTINEWARRAY. The size of every dimension must have been already
	 * pushed on the stack.
	 *
	 * @param clazz
	 *            the array type.
	 * @param dim
	 *            the number of the dimensions.
	 * @return the value of <code>dim</code>.
	 */
	public int addMultiNewarray(CtClass clazz, int dim)
	{
		this.add(Opcode.MULTIANEWARRAY);
		this.addIndex(this.constPool.addClassInfo(clazz));
		this.add(dim);
		this.growStack(1 - dim);
		return dim;
	}

	/**
	 * Appends MULTINEWARRAY.
	 *
	 * @param clazz
	 *            the array type.
	 * @param dimensions
	 *            the sizes of all dimensions.
	 * @return the length of <code>dimensions</code>.
	 */
	public int addMultiNewarray(CtClass clazz, int[] dimensions)
	{
		int len = dimensions.length;
		for (int i = 0; i < len; ++i)
			this.addIconst(dimensions[i]);

		this.growStack(len);
		return this.addMultiNewarray(clazz, len);
	}

	/**
	 * Appends MULTINEWARRAY.
	 *
	 * @param desc
	 *            the type descriptor of the created array.
	 * @param dim
	 *            dimensions.
	 * @return the value of <code>dim</code>.
	 */
	public int addMultiNewarray(String desc, int dim)
	{
		this.add(Opcode.MULTIANEWARRAY);
		this.addIndex(this.constPool.addClassInfo(desc));
		this.add(dim);
		this.growStack(1 - dim);
		return dim;
	}

	/**
	 * Appends NEW.
	 *
	 * @param clazz
	 *            the class of the created instance.
	 */
	public void addNew(CtClass clazz)
	{
		this.addOpcode(Opcode.NEW);
		this.addIndex(this.constPool.addClassInfo(clazz));
	}

	/**
	 * Appends NEW.
	 *
	 * @param classname
	 *            the fully-qualified class name.
	 */
	public void addNew(String classname)
	{
		this.addOpcode(Opcode.NEW);
		this.addIndex(this.constPool.addClassInfo(classname));
	}

	/**
	 * Appends NEWARRAY for primitive types.
	 *
	 * @param atype
	 *            <code>T_BOOLEAN</code>, <code>T_CHAR</code>, ...
	 * @see Opcode
	 */
	public void addNewarray(int atype, int length)
	{
		this.addIconst(length);
		this.addOpcode(Opcode.NEWARRAY);
		this.add(atype);
	}

	/**
	 * Appends an 8bit opcode to the end of the bytecode sequence. The current
	 * stack depth is updated. <code>max_stack</code> is updated if the current
	 * stack depth is the deepest so far.
	 * <p>
	 * Note: some instructions such as INVOKEVIRTUAL does not update the current
	 * stack depth since the increment depends on the method signature.
	 * <code>growStack()</code> must be explicitly called.
	 */
	public void addOpcode(int code)
	{
		this.add(code);
		this.growStack(Opcode.STACK_GROW[code]);
	}

	/**
	 * Appends instructions for executing
	 * <code>java.lang.System.println(<i>message</i>)</code>.
	 *
	 * @param message
	 *            printed message.
	 */
	public void addPrintln(String message)
	{
		this.addGetstatic("java.lang.System", "err", "Ljava/io/PrintStream;");
		this.addLdc(message);
		this.addInvokevirtual("java.io.PrintStream", "println", "(Ljava/lang/String;)V");
	}

	/**
	 * Appends PUTFIELD.
	 *
	 * @param c
	 *            the target class.
	 * @param name
	 *            the field name.
	 * @param desc
	 *            the descriptor of the field type.
	 */
	public void addPutfield(CtClass c, String name, String desc)
	{
		this.addPutfield0(c, null, name, desc);
	}

	/**
	 * Appends PUTFIELD.
	 *
	 * @param classname
	 *            the fully-qualified name of the target class.
	 * @param name
	 *            the field name.
	 * @param desc
	 *            the descriptor of the field type.
	 */
	public void addPutfield(String classname, String name, String desc)
	{
		// if classnaem is null, the target class is THIS.
		this.addPutfield0(null, classname, name, desc);
	}

	private void addPutfield0(CtClass target, String classname, String name, String desc)
	{
		this.add(Opcode.PUTFIELD);
		// target is null if it represents THIS.
		int ci = classname == null ? this.constPool.addClassInfo(target) : this.constPool.addClassInfo(classname);
		this.addIndex(this.constPool.addFieldrefInfo(ci, name, desc));
		this.growStack(-1 - Descriptor.dataSize(desc));
	}

	/**
	 * Appends PUTSTATIC.
	 *
	 * @param c
	 *            the target class.
	 * @param name
	 *            the field name.
	 * @param desc
	 *            the descriptor of the field type.
	 */
	public void addPutstatic(CtClass c, String name, String desc)
	{
		this.addPutstatic0(c, null, name, desc);
	}

	/**
	 * Appends PUTSTATIC.
	 *
	 * @param classname
	 *            the fully-qualified name of the target class.
	 * @param fieldName
	 *            the field name.
	 * @param desc
	 *            the descriptor of the field type.
	 */
	public void addPutstatic(String classname, String fieldName, String desc)
	{
		// if classname is null, the target class is THIS.
		this.addPutstatic0(null, classname, fieldName, desc);
	}

	private void addPutstatic0(CtClass target, String classname, String fieldName, String desc)
	{
		this.add(Opcode.PUTSTATIC);
		// target is null if it represents THIS.
		int ci = classname == null ? this.constPool.addClassInfo(target) : this.constPool.addClassInfo(classname);
		this.addIndex(this.constPool.addFieldrefInfo(ci, fieldName, desc));
		this.growStack(-Descriptor.dataSize(desc));
	}

	/**
	 * Appends RET.
	 *
	 * @param var
	 *            local variable
	 */
	public void addRet(int var)
	{
		if (var < 0x100)
		{
			this.addOpcode(Opcode.RET);
			this.add(var);
		} else
		{
			this.addOpcode(Opcode.WIDE);
			this.addOpcode(Opcode.RET);
			this.addIndex(var);
		}
	}

	/**
	 * Appends ARETURN, IRETURN, .., or RETURN.
	 *
	 * @param type
	 *            the return type.
	 */
	public void addReturn(CtClass type)
	{
		if (type == null)
			this.addOpcode(Opcode.RETURN);
		else if (type.isPrimitive())
		{
			CtPrimitiveType ptype = (CtPrimitiveType) type;
			this.addOpcode(ptype.getReturnOp());
		} else
			this.addOpcode(Opcode.ARETURN);
	}

	/**
	 * Appends an instruction for storing a value into the local variable at the
	 * index <code>n</code>.
	 *
	 * @param n
	 *            the index.
	 * @param type
	 *            the type of the stored value.
	 * @return 2 if the type is long or double. Otherwise 1.
	 */
	public int addStore(int n, CtClass type)
	{
		if (type.isPrimitive())
		{
			if (type == CtClass.booleanType || type == CtClass.charType || type == CtClass.byteType || type == CtClass.shortType || type == CtClass.intType)
				this.addIstore(n);
			else if (type == CtClass.longType)
			{
				this.addLstore(n);
				return 2;
			} else if (type == CtClass.floatType)
				this.addFstore(n);
			else if (type == CtClass.doubleType)
			{
				this.addDstore(n);
				return 2;
			} else
				throw new RuntimeException("void type?");
		} else
			this.addAstore(n);

		return 1;
	}

	/**
	 * Creates and returns a copy of this object. The constant pool object is
	 * shared between this object and the cloned object.
	 */
	@Override
	public Object clone()
	{
		try
		{
			Bytecode bc = (Bytecode) super.clone();
			bc.tryblocks = (ExceptionTable) this.tryblocks.clone();
			return bc;
		} catch (CloneNotSupportedException cnse)
		{
			throw new RuntimeException(cnse);
		}
	}

	/**
	 * Returns the length of bytecode sequence that have been added so far.
	 */
	public int currentPc()
	{
		return this.getSize();
	}

	/**
	 * Returns the produced bytecode sequence.
	 */
	public byte[] get()
	{
		return this.copy();
	}

	/**
	 * Gets a constant pool table.
	 */
	public ConstPool getConstPool()
	{
		return this.constPool;
	}

	/**
	 * Returns <code>exception_table</code>.
	 */
	public ExceptionTable getExceptionTable()
	{
		return this.tryblocks;
	}

	/**
	 * Gets <code>max_locals</code>.
	 */
	public int getMaxLocals()
	{
		return this.maxLocals;
	}

	/**
	 * Gets <code>max_stack</code>.
	 */
	public int getMaxStack()
	{
		return this.maxStack;
	}

	/**
	 * Returns the current stack depth.
	 */
	public int getStackDepth()
	{
		return this.stackDepth;
	}

	/**
	 * Increases the current stack depth. It also updates <code>max_stack</code>
	 * if the current stack depth is the deepest so far.
	 *
	 * @param diff
	 *            the number added to the current stack depth.
	 */
	public void growStack(int diff)
	{
		this.setStackDepth(this.stackDepth + diff);
	}

	/**
	 * Increments <code>max_locals</code>.
	 */
	public void incMaxLocals(int diff)
	{
		this.maxLocals += diff;
	}

	/**
	 * Returns the length of the bytecode sequence.
	 */
	public int length()
	{
		return this.getSize();
	}

	/**
	 * Reads a signed 8bit value at the offset from the beginning of the
	 * bytecode sequence.
	 *
	 * @throws ArrayIndexOutOfBoundsException
	 *             if offset is invalid.
	 */
	@Override
	public int read(int offset)
	{
		return super.read(offset);
	}

	/**
	 * Reads a signed 16bit value at the offset from the beginning of the
	 * bytecode sequence.
	 */
	public int read16bit(int offset)
	{
		int v1 = this.read(offset);
		int v2 = this.read(offset + 1);
		return (v1 << 8) + (v2 & 0xff);
	}

	/**
	 * Reads a signed 32bit value at the offset from the beginning of the
	 * bytecode sequence.
	 */
	public int read32bit(int offset)
	{
		int v1 = this.read16bit(offset);
		int v2 = this.read16bit(offset + 2);
		return (v1 << 16) + (v2 & 0xffff);
	}

	/**
	 * Sets <code>max_locals</code>.
	 * <p>
	 * This computes the number of local variables used to pass method
	 * parameters and sets <code>max_locals</code> to that number plus
	 * <code>locals</code>.
	 *
	 * @param isStatic
	 *            true if <code>params</code> must be interpreted as parameters
	 *            to a static method.
	 * @param params
	 *            parameter types.
	 * @param locals
	 *            the number of local variables excluding ones used to pass
	 *            parameters.
	 */
	public void setMaxLocals(boolean isStatic, CtClass[] params, int locals)
	{
		if (!isStatic)
			++locals;

		if (params != null)
		{
			CtClass doubleType = CtClass.doubleType;
			CtClass longType = CtClass.longType;
			int n = params.length;
			for (int i = 0; i < n; ++i)
			{
				CtClass type = params[i];
				if (type == doubleType || type == longType)
					locals += 2;
				else
					++locals;
			}
		}

		this.maxLocals = locals;
	}

	/**
	 * Sets <code>max_locals</code>.
	 */
	public void setMaxLocals(int size)
	{
		this.maxLocals = size;
	}

	/**
	 * Sets <code>max_stack</code>.
	 * <p>
	 * This value may be automatically updated when an instruction is appended.
	 * A <code>Bytecode</code> object maintains the current stack depth whenever
	 * an instruction is added by <code>addOpcode()</code>. For example, if DUP
	 * is appended, the current stack depth is increased by one. If the new
	 * stack depth is more than <code>max_stack</code>, then it is assigned to
	 * <code>max_stack</code>. However, if branch instructions are appended, the
	 * current stack depth may not be correctly maintained.
	 *
	 * @see #addOpcode(int)
	 */
	public void setMaxStack(int size)
	{
		this.maxStack = size;
	}

	/**
	 * Sets the current stack depth. It also updates <code>max_stack</code> if
	 * the current stack depth is the deepest so far.
	 *
	 * @param depth
	 *            new value.
	 */
	public void setStackDepth(int depth)
	{
		this.stackDepth = depth;
		if (this.stackDepth > this.maxStack)
			this.maxStack = this.stackDepth;
	}

	/**
	 * Converts to a <code>CodeAttribute</code>.
	 */
	public CodeAttribute toCodeAttribute()
	{
		return new CodeAttribute(this.constPool, this.maxStack, this.maxLocals, this.get(), this.tryblocks);
	}

	/**
	 * Writes an 8bit value at the offset from the beginning of the bytecode
	 * sequence.
	 *
	 * @throws ArrayIndexOutOfBoundsException
	 *             if offset is invalid.
	 */
	@Override
	public void write(int offset, int value)
	{
		super.write(offset, value);
	}

	/**
	 * Writes an 16bit value at the offset from the beginning of the bytecode
	 * sequence.
	 */
	public void write16bit(int offset, int value)
	{
		this.write(offset, value >> 8);
		this.write(offset + 1, value);
	}

	/**
	 * Writes an 32bit value at the offset from the beginning of the bytecode
	 * sequence.
	 */
	public void write32bit(int offset, int value)
	{
		this.write16bit(offset, value >> 16);
		this.write16bit(offset + 2, value);
	}
}

class ByteVector implements Cloneable
{
	private byte[]	buffer;
	private int		size;

	public ByteVector()
	{
		this.buffer = new byte[64];
		this.size = 0;
	}

	public void add(int code)
	{
		this.addGap(1);
		this.buffer[this.size - 1] = (byte) code;
	}

	public void add(int b1, int b2)
	{
		this.addGap(2);
		this.buffer[this.size - 2] = (byte) b1;
		this.buffer[this.size - 1] = (byte) b2;
	}

	public void add(int b1, int b2, int b3, int b4)
	{
		this.addGap(4);
		this.buffer[this.size - 4] = (byte) b1;
		this.buffer[this.size - 3] = (byte) b2;
		this.buffer[this.size - 2] = (byte) b3;
		this.buffer[this.size - 1] = (byte) b4;
	}

	public void addGap(int length)
	{
		if (this.size + length > this.buffer.length)
		{
			int newSize = this.size << 1;
			if (newSize < this.size + length)
				newSize = this.size + length;

			byte[] newBuf = new byte[newSize];
			System.arraycopy(this.buffer, 0, newBuf, 0, this.size);
			this.buffer = newBuf;
		}

		this.size += length;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		ByteVector bv = (ByteVector) super.clone();
		bv.buffer = this.buffer.clone();
		return bv;
	}

	public final byte[] copy()
	{
		byte[] b = new byte[this.size];
		System.arraycopy(this.buffer, 0, b, 0, this.size);
		return b;
	}

	public final int getSize()
	{
		return this.size;
	}

	public int read(int offset)
	{
		if (offset < 0 || this.size <= offset)
			throw new ArrayIndexOutOfBoundsException(offset);

		return this.buffer[offset];
	}

	public void write(int offset, int value)
	{
		if (offset < 0 || this.size <= offset)
			throw new ArrayIndexOutOfBoundsException(offset);

		this.buffer[offset] = (byte) value;
	}
}
