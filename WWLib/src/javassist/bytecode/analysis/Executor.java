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
package javassist.bytecode.analysis;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * Executor is responsible for modeling the effects of a JVM instruction on a
 * frame.
 *
 * @author Jason T. Greene
 */
public class Executor implements Opcode
{
	private final ConstPool	constPool;
	private final ClassPool	classPool;
	private final Type		STRING_TYPE;
	private final Type		CLASS_TYPE;
	private final Type		THROWABLE_TYPE;
	private int				lastPos;

	public Executor(ClassPool classPool, ConstPool constPool)
	{
		this.constPool = constPool;
		this.classPool = classPool;

		try
		{
			this.STRING_TYPE = this.getType("java.lang.String");
			this.CLASS_TYPE = this.getType("java.lang.Class");
			this.THROWABLE_TYPE = this.getType("java.lang.Throwable");
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private void access(int index, Type type, Subroutine subroutine)
	{
		if (subroutine == null)
			return;
		subroutine.access(index);
		if (type.getSize() == 2)
			subroutine.access(index + 1);
	}

	private void evalArrayLoad(Type expectedComponent, Frame frame) throws BadBytecode
	{
		Type index = frame.pop();
		Type array = frame.pop();

		// Special case, an array defined by aconst_null
		// TODO - we might need to be more inteligent about this
		if (array == Type.UNINIT)
		{
			this.verifyAssignable(Type.INTEGER, index);
			if (expectedComponent == Type.OBJECT)
				this.simplePush(Type.UNINIT, frame);
			else
				this.simplePush(expectedComponent, frame);
			return;
		}

		Type component = array.getComponent();

		if (component == null)
			throw new BadBytecode("Not an array! [pos = " + this.lastPos + "]: " + component);

		component = this.zeroExtend(component);

		this.verifyAssignable(expectedComponent, component);
		this.verifyAssignable(Type.INTEGER, index);
		this.simplePush(component, frame);
	}

	private void evalArrayStore(Type expectedComponent, Frame frame) throws BadBytecode
	{
		Type value = this.simplePop(frame);
		Type index = frame.pop();
		Type array = frame.pop();

		if (array == Type.UNINIT)
		{
			this.verifyAssignable(Type.INTEGER, index);
			return;
		}

		Type component = array.getComponent();

		if (component == null)
			throw new BadBytecode("Not an array! [pos = " + this.lastPos + "]: " + component);

		component = this.zeroExtend(component);

		this.verifyAssignable(expectedComponent, component);
		this.verifyAssignable(Type.INTEGER, index);

		// This intentionally only checks for Object on aastore
		// downconverting of an array (no casts)
		// e.g. Object[] blah = new String[];
		// blah[2] = (Object) "test";
		// blah[3] = new Integer(); // compiler doesnt catch it (has legal
		// bytecode),
		// // but will throw arraystoreexception
		if (expectedComponent == Type.OBJECT)
			this.verifyAssignable(expectedComponent, value);
		else
			this.verifyAssignable(component, value);
	}

	private void evalBinaryMath(Type expected, Frame frame) throws BadBytecode
	{
		Type value2 = this.simplePop(frame);
		Type value1 = this.simplePop(frame);

		this.verifyAssignable(expected, value2);
		this.verifyAssignable(expected, value1);
		this.simplePush(value1, frame);
	}

	private void evalGetField(int opcode, int index, Frame frame) throws BadBytecode
	{
		String desc = this.constPool.getFieldrefType(index);
		Type type = this.zeroExtend(this.typeFromDesc(desc));

		if (opcode == Opcode.GETFIELD)
		{
			Type objectType = this.resolveClassInfo(this.constPool.getFieldrefClassName(index));
			this.verifyAssignable(objectType, this.simplePop(frame));
		}

		this.simplePush(type, frame);
	}

	private void evalInvokeDynamic(int opcode, int index, Frame frame) throws BadBytecode
	{
		String desc = this.constPool.getInvokeDynamicType(index);
		Type[] types = this.paramTypesFromDesc(desc);
		int i = types.length;

		while (i > 0)
			this.verifyAssignable(this.zeroExtend(types[--i]), this.simplePop(frame));

		// simplePop(frame); // assume CosntPool#REF_invokeStatic

		Type returnType = this.returnTypeFromDesc(desc);
		if (returnType != Type.VOID)
			this.simplePush(this.zeroExtend(returnType), frame);
	}

	private void evalInvokeIntfMethod(int opcode, int index, Frame frame) throws BadBytecode
	{
		String desc = this.constPool.getInterfaceMethodrefType(index);
		Type[] types = this.paramTypesFromDesc(desc);
		int i = types.length;

		while (i > 0)
			this.verifyAssignable(this.zeroExtend(types[--i]), this.simplePop(frame));

		String classInfo = this.constPool.getInterfaceMethodrefClassName(index);
		Type objectType = this.resolveClassInfo(classInfo);
		this.verifyAssignable(objectType, this.simplePop(frame));

		Type returnType = this.returnTypeFromDesc(desc);
		if (returnType != Type.VOID)
			this.simplePush(this.zeroExtend(returnType), frame);
	}

	private void evalInvokeMethod(int opcode, int index, Frame frame) throws BadBytecode
	{
		String desc = this.constPool.getMethodrefType(index);
		Type[] types = this.paramTypesFromDesc(desc);
		int i = types.length;

		while (i > 0)
			this.verifyAssignable(this.zeroExtend(types[--i]), this.simplePop(frame));

		if (opcode != Opcode.INVOKESTATIC)
		{
			Type objectType = this.resolveClassInfo(this.constPool.getMethodrefClassName(index));
			this.verifyAssignable(objectType, this.simplePop(frame));
		}

		Type returnType = this.returnTypeFromDesc(desc);
		if (returnType != Type.VOID)
			this.simplePush(this.zeroExtend(returnType), frame);
	}

	private void evalLDC(int index, Frame frame) throws BadBytecode
	{
		int tag = this.constPool.getTag(index);
		Type type;
		switch (tag)
		{
			case ConstPool.CONST_String:
				type = this.STRING_TYPE;
				break;
			case ConstPool.CONST_Integer:
				type = Type.INTEGER;
				break;
			case ConstPool.CONST_Float:
				type = Type.FLOAT;
				break;
			case ConstPool.CONST_Long:
				type = Type.LONG;
				break;
			case ConstPool.CONST_Double:
				type = Type.DOUBLE;
				break;
			case ConstPool.CONST_Class:
				type = this.CLASS_TYPE;
				break;
			default:
				throw new BadBytecode("bad LDC [pos = " + this.lastPos + "]: " + tag);
		}

		this.simplePush(type, frame);
	}

	private void evalLoad(Type expected, int index, Frame frame, Subroutine subroutine) throws BadBytecode
	{
		Type type = frame.getLocal(index);

		this.verifyAssignable(expected, type);

		this.simplePush(type, frame);
		this.access(index, type, subroutine);
	}

	private void evalNewArray(int pos, CodeIterator iter, Frame frame) throws BadBytecode
	{
		this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
		Type type = null;
		int typeInfo = iter.byteAt(pos + 1);
		switch (typeInfo)
		{
			case T_BOOLEAN:
				type = this.getType("boolean[]");
				break;
			case T_CHAR:
				type = this.getType("char[]");
				break;
			case T_BYTE:
				type = this.getType("byte[]");
				break;
			case T_SHORT:
				type = this.getType("short[]");
				break;
			case T_INT:
				type = this.getType("int[]");
				break;
			case T_LONG:
				type = this.getType("long[]");
				break;
			case T_FLOAT:
				type = this.getType("float[]");
				break;
			case T_DOUBLE:
				type = this.getType("double[]");
				break;
			default:
				throw new BadBytecode("Invalid array type [pos = " + pos + "]: " + typeInfo);

		}

		frame.push(type);
	}

	private void evalNewObjectArray(int pos, CodeIterator iter, Frame frame) throws BadBytecode
	{
		// Convert to x[] format
		Type type = this.resolveClassInfo(this.constPool.getClassInfo(iter.u16bitAt(pos + 1)));
		String name = type.getCtClass().getName();
		int opcode = iter.byteAt(pos);
		int dimensions;

		if (opcode == Opcode.MULTIANEWARRAY)
			dimensions = iter.byteAt(pos + 3);
		else
		{
			name = name + "[]";
			dimensions = 1;
		}

		while (dimensions-- > 0)
			this.verifyAssignable(Type.INTEGER, this.simplePop(frame));

		this.simplePush(this.getType(name), frame);
	}

	private void evalPutField(int opcode, int index, Frame frame) throws BadBytecode
	{
		String desc = this.constPool.getFieldrefType(index);
		Type type = this.zeroExtend(this.typeFromDesc(desc));

		this.verifyAssignable(type, this.simplePop(frame));

		if (opcode == Opcode.PUTFIELD)
		{
			Type objectType = this.resolveClassInfo(this.constPool.getFieldrefClassName(index));
			this.verifyAssignable(objectType, this.simplePop(frame));
		}
	}

	private void evalShift(Type expected, Frame frame) throws BadBytecode
	{
		Type value2 = this.simplePop(frame);
		Type value1 = this.simplePop(frame);

		this.verifyAssignable(Type.INTEGER, value2);
		this.verifyAssignable(expected, value1);
		this.simplePush(value1, frame);
	}

	private void evalStore(Type expected, int index, Frame frame, Subroutine subroutine) throws BadBytecode
	{
		Type type = this.simplePop(frame);

		// RETURN_ADDRESS is allowed by ASTORE
		if (!(expected == Type.OBJECT && type == Type.RETURN_ADDRESS))
			this.verifyAssignable(expected, type);
		this.simpleSetLocal(index, type, frame);
		this.access(index, type, subroutine);
	}

	private void evalWide(int pos, CodeIterator iter, Frame frame, Subroutine subroutine) throws BadBytecode
	{
		int opcode = iter.byteAt(pos + 1);
		int index = iter.u16bitAt(pos + 2);
		switch (opcode)
		{
			case ILOAD:
				this.evalLoad(Type.INTEGER, index, frame, subroutine);
				break;
			case LLOAD:
				this.evalLoad(Type.LONG, index, frame, subroutine);
				break;
			case FLOAD:
				this.evalLoad(Type.FLOAT, index, frame, subroutine);
				break;
			case DLOAD:
				this.evalLoad(Type.DOUBLE, index, frame, subroutine);
				break;
			case ALOAD:
				this.evalLoad(Type.OBJECT, index, frame, subroutine);
				break;
			case ISTORE:
				this.evalStore(Type.INTEGER, index, frame, subroutine);
				break;
			case LSTORE:
				this.evalStore(Type.LONG, index, frame, subroutine);
				break;
			case FSTORE:
				this.evalStore(Type.FLOAT, index, frame, subroutine);
				break;
			case DSTORE:
				this.evalStore(Type.DOUBLE, index, frame, subroutine);
				break;
			case ASTORE:
				this.evalStore(Type.OBJECT, index, frame, subroutine);
				break;
			case IINC:
				this.verifyAssignable(Type.INTEGER, frame.getLocal(index));
				break;
			case RET:
				this.verifyAssignable(Type.RETURN_ADDRESS, frame.getLocal(index));
				break;
			default:
				throw new BadBytecode("Invalid WIDE operand [pos = " + pos + "]: " + opcode);
		}

	}

	/**
	 * Execute the instruction, modeling the effects on the specified frame and
	 * subroutine. If a subroutine is passed, the access flags will be modified
	 * if this instruction accesses the local variable table.
	 *
	 * @param method
	 *            the method containing the instruction
	 * @param pos
	 *            the position of the instruction in the method
	 * @param iter
	 *            the code iterator used to find the instruction
	 * @param frame
	 *            the frame to modify to represent the result of the instruction
	 * @param subroutine
	 *            the optional subroutine this instruction belongs to.
	 * @throws BadBytecode
	 *             if the bytecode violates the jvm spec
	 */
	public void execute(MethodInfo method, int pos, CodeIterator iter, Frame frame, Subroutine subroutine) throws BadBytecode
	{
		this.lastPos = pos;
		int opcode = iter.byteAt(pos);

		// Declared opcode in order
		switch (opcode)
		{
			case NOP:
				break;
			case ACONST_NULL:
				frame.push(Type.UNINIT);
				break;
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
				frame.push(Type.INTEGER);
				break;
			case LCONST_0:
			case LCONST_1:
				frame.push(Type.LONG);
				frame.push(Type.TOP);
				break;
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
				frame.push(Type.FLOAT);
				break;
			case DCONST_0:
			case DCONST_1:
				frame.push(Type.DOUBLE);
				frame.push(Type.TOP);
				break;
			case BIPUSH:
			case SIPUSH:
				frame.push(Type.INTEGER);
				break;
			case LDC:
				this.evalLDC(iter.byteAt(pos + 1), frame);
				break;
			case LDC_W:
			case LDC2_W:
				this.evalLDC(iter.u16bitAt(pos + 1), frame);
				break;
			case ILOAD:
				this.evalLoad(Type.INTEGER, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case LLOAD:
				this.evalLoad(Type.LONG, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case FLOAD:
				this.evalLoad(Type.FLOAT, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case DLOAD:
				this.evalLoad(Type.DOUBLE, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case ALOAD:
				this.evalLoad(Type.OBJECT, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
				this.evalLoad(Type.INTEGER, opcode - Opcode.ILOAD_0, frame, subroutine);
				break;
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
				this.evalLoad(Type.LONG, opcode - Opcode.LLOAD_0, frame, subroutine);
				break;
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
				this.evalLoad(Type.FLOAT, opcode - Opcode.FLOAD_0, frame, subroutine);
				break;
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
				this.evalLoad(Type.DOUBLE, opcode - Opcode.DLOAD_0, frame, subroutine);
				break;
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
				this.evalLoad(Type.OBJECT, opcode - Opcode.ALOAD_0, frame, subroutine);
				break;
			case IALOAD:
				this.evalArrayLoad(Type.INTEGER, frame);
				break;
			case LALOAD:
				this.evalArrayLoad(Type.LONG, frame);
				break;
			case FALOAD:
				this.evalArrayLoad(Type.FLOAT, frame);
				break;
			case DALOAD:
				this.evalArrayLoad(Type.DOUBLE, frame);
				break;
			case AALOAD:
				this.evalArrayLoad(Type.OBJECT, frame);
				break;
			case BALOAD:
			case CALOAD:
			case SALOAD:
				this.evalArrayLoad(Type.INTEGER, frame);
				break;
			case ISTORE:
				this.evalStore(Type.INTEGER, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case LSTORE:
				this.evalStore(Type.LONG, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case FSTORE:
				this.evalStore(Type.FLOAT, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case DSTORE:
				this.evalStore(Type.DOUBLE, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case ASTORE:
				this.evalStore(Type.OBJECT, iter.byteAt(pos + 1), frame, subroutine);
				break;
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
				this.evalStore(Type.INTEGER, opcode - Opcode.ISTORE_0, frame, subroutine);
				break;
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
				this.evalStore(Type.LONG, opcode - Opcode.LSTORE_0, frame, subroutine);
				break;
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
				this.evalStore(Type.FLOAT, opcode - Opcode.FSTORE_0, frame, subroutine);
				break;
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
				this.evalStore(Type.DOUBLE, opcode - Opcode.DSTORE_0, frame, subroutine);
				break;
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
				this.evalStore(Type.OBJECT, opcode - Opcode.ASTORE_0, frame, subroutine);
				break;
			case IASTORE:
				this.evalArrayStore(Type.INTEGER, frame);
				break;
			case LASTORE:
				this.evalArrayStore(Type.LONG, frame);
				break;
			case FASTORE:
				this.evalArrayStore(Type.FLOAT, frame);
				break;
			case DASTORE:
				this.evalArrayStore(Type.DOUBLE, frame);
				break;
			case AASTORE:
				this.evalArrayStore(Type.OBJECT, frame);
				break;
			case BASTORE:
			case CASTORE:
			case SASTORE:
				this.evalArrayStore(Type.INTEGER, frame);
				break;
			case POP:
				if (frame.pop() == Type.TOP)
					throw new BadBytecode("POP can not be used with a category 2 value, pos = " + pos);
				break;
			case POP2:
				frame.pop();
				frame.pop();
				break;
			case DUP:
			{
				Type type = frame.peek();
				if (type == Type.TOP)
					throw new BadBytecode("DUP can not be used with a category 2 value, pos = " + pos);

				frame.push(frame.peek());
				break;
			}
			case DUP_X1:
			case DUP_X2:
			{
				Type type = frame.peek();
				if (type == Type.TOP)
					throw new BadBytecode("DUP can not be used with a category 2 value, pos = " + pos);
				int end = frame.getTopIndex();
				int insert = end - (opcode - Opcode.DUP_X1) - 1;
				frame.push(type);

				while (end > insert)
				{
					frame.setStack(end, frame.getStack(end - 1));
					end--;
				}
				frame.setStack(insert, type);
				break;
			}
			case DUP2:
				frame.push(frame.getStack(frame.getTopIndex() - 1));
				frame.push(frame.getStack(frame.getTopIndex() - 1));
				break;
			case DUP2_X1:
			case DUP2_X2:
			{
				int end = frame.getTopIndex();
				int insert = end - (opcode - Opcode.DUP2_X1) - 1;
				Type type1 = frame.getStack(frame.getTopIndex() - 1);
				Type type2 = frame.peek();
				frame.push(type1);
				frame.push(type2);
				while (end > insert)
				{
					frame.setStack(end, frame.getStack(end - 2));
					end--;
				}
				frame.setStack(insert, type2);
				frame.setStack(insert - 1, type1);
				break;
			}
			case SWAP:
			{
				Type type1 = frame.pop();
				Type type2 = frame.pop();
				if (type1.getSize() == 2 || type2.getSize() == 2)
					throw new BadBytecode("Swap can not be used with category 2 values, pos = " + pos);
				frame.push(type1);
				frame.push(type2);
				break;
			}

			// Math
			case IADD:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LADD:
				this.evalBinaryMath(Type.LONG, frame);
				break;
			case FADD:
				this.evalBinaryMath(Type.FLOAT, frame);
				break;
			case DADD:
				this.evalBinaryMath(Type.DOUBLE, frame);
				break;
			case ISUB:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LSUB:
				this.evalBinaryMath(Type.LONG, frame);
				break;
			case FSUB:
				this.evalBinaryMath(Type.FLOAT, frame);
				break;
			case DSUB:
				this.evalBinaryMath(Type.DOUBLE, frame);
				break;
			case IMUL:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LMUL:
				this.evalBinaryMath(Type.LONG, frame);
				break;
			case FMUL:
				this.evalBinaryMath(Type.FLOAT, frame);
				break;
			case DMUL:
				this.evalBinaryMath(Type.DOUBLE, frame);
				break;
			case IDIV:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LDIV:
				this.evalBinaryMath(Type.LONG, frame);
				break;
			case FDIV:
				this.evalBinaryMath(Type.FLOAT, frame);
				break;
			case DDIV:
				this.evalBinaryMath(Type.DOUBLE, frame);
				break;
			case IREM:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LREM:
				this.evalBinaryMath(Type.LONG, frame);
				break;
			case FREM:
				this.evalBinaryMath(Type.FLOAT, frame);
				break;
			case DREM:
				this.evalBinaryMath(Type.DOUBLE, frame);
				break;

			// Unary
			case INEG:
				this.verifyAssignable(Type.INTEGER, this.simplePeek(frame));
				break;
			case LNEG:
				this.verifyAssignable(Type.LONG, this.simplePeek(frame));
				break;
			case FNEG:
				this.verifyAssignable(Type.FLOAT, this.simplePeek(frame));
				break;
			case DNEG:
				this.verifyAssignable(Type.DOUBLE, this.simplePeek(frame));
				break;

			// Shifts
			case ISHL:
				this.evalShift(Type.INTEGER, frame);
				break;
			case LSHL:
				this.evalShift(Type.LONG, frame);
				break;
			case ISHR:
				this.evalShift(Type.INTEGER, frame);
				break;
			case LSHR:
				this.evalShift(Type.LONG, frame);
				break;
			case IUSHR:
				this.evalShift(Type.INTEGER, frame);
				break;
			case LUSHR:
				this.evalShift(Type.LONG, frame);
				break;

			// Bitwise Math
			case IAND:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LAND:
				this.evalBinaryMath(Type.LONG, frame);
				break;
			case IOR:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LOR:
				this.evalBinaryMath(Type.LONG, frame);
				break;
			case IXOR:
				this.evalBinaryMath(Type.INTEGER, frame);
				break;
			case LXOR:
				this.evalBinaryMath(Type.LONG, frame);
				break;

			case IINC:
			{
				int index = iter.byteAt(pos + 1);
				this.verifyAssignable(Type.INTEGER, frame.getLocal(index));
				this.access(index, Type.INTEGER, subroutine);
				break;
			}

			// Conversion
			case I2L:
				this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
				this.simplePush(Type.LONG, frame);
				break;
			case I2F:
				this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
				this.simplePush(Type.FLOAT, frame);
				break;
			case I2D:
				this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
				this.simplePush(Type.DOUBLE, frame);
				break;
			case L2I:
				this.verifyAssignable(Type.LONG, this.simplePop(frame));
				this.simplePush(Type.INTEGER, frame);
				break;
			case L2F:
				this.verifyAssignable(Type.LONG, this.simplePop(frame));
				this.simplePush(Type.FLOAT, frame);
				break;
			case L2D:
				this.verifyAssignable(Type.LONG, this.simplePop(frame));
				this.simplePush(Type.DOUBLE, frame);
				break;
			case F2I:
				this.verifyAssignable(Type.FLOAT, this.simplePop(frame));
				this.simplePush(Type.INTEGER, frame);
				break;
			case F2L:
				this.verifyAssignable(Type.FLOAT, this.simplePop(frame));
				this.simplePush(Type.LONG, frame);
				break;
			case F2D:
				this.verifyAssignable(Type.FLOAT, this.simplePop(frame));
				this.simplePush(Type.DOUBLE, frame);
				break;
			case D2I:
				this.verifyAssignable(Type.DOUBLE, this.simplePop(frame));
				this.simplePush(Type.INTEGER, frame);
				break;
			case D2L:
				this.verifyAssignable(Type.DOUBLE, this.simplePop(frame));
				this.simplePush(Type.LONG, frame);
				break;
			case D2F:
				this.verifyAssignable(Type.DOUBLE, this.simplePop(frame));
				this.simplePush(Type.FLOAT, frame);
				break;
			case I2B:
			case I2C:
			case I2S:
				this.verifyAssignable(Type.INTEGER, frame.peek());
				break;
			case LCMP:
				this.verifyAssignable(Type.LONG, this.simplePop(frame));
				this.verifyAssignable(Type.LONG, this.simplePop(frame));
				frame.push(Type.INTEGER);
				break;
			case FCMPL:
			case FCMPG:
				this.verifyAssignable(Type.FLOAT, this.simplePop(frame));
				this.verifyAssignable(Type.FLOAT, this.simplePop(frame));
				frame.push(Type.INTEGER);
				break;
			case DCMPL:
			case DCMPG:
				this.verifyAssignable(Type.DOUBLE, this.simplePop(frame));
				this.verifyAssignable(Type.DOUBLE, this.simplePop(frame));
				frame.push(Type.INTEGER);
				break;

			// Control flow
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
				this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
				break;
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
				this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
				this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
				break;
			case IF_ACMPEQ:
			case IF_ACMPNE:
				this.verifyAssignable(Type.OBJECT, this.simplePop(frame));
				this.verifyAssignable(Type.OBJECT, this.simplePop(frame));
				break;
			case GOTO:
				break;
			case JSR:
				frame.push(Type.RETURN_ADDRESS);
				break;
			case RET:
				this.verifyAssignable(Type.RETURN_ADDRESS, frame.getLocal(iter.byteAt(pos + 1)));
				break;
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case IRETURN:
				this.verifyAssignable(Type.INTEGER, this.simplePop(frame));
				break;
			case LRETURN:
				this.verifyAssignable(Type.LONG, this.simplePop(frame));
				break;
			case FRETURN:
				this.verifyAssignable(Type.FLOAT, this.simplePop(frame));
				break;
			case DRETURN:
				this.verifyAssignable(Type.DOUBLE, this.simplePop(frame));
				break;
			case ARETURN:
				try
				{
					CtClass returnType = Descriptor.getReturnType(method.getDescriptor(), this.classPool);
					this.verifyAssignable(Type.get(returnType), this.simplePop(frame));
				} catch (NotFoundException e)
				{
					throw new RuntimeException(e);
				}
				break;
			case RETURN:
				break;
			case GETSTATIC:
				this.evalGetField(opcode, iter.u16bitAt(pos + 1), frame);
				break;
			case PUTSTATIC:
				this.evalPutField(opcode, iter.u16bitAt(pos + 1), frame);
				break;
			case GETFIELD:
				this.evalGetField(opcode, iter.u16bitAt(pos + 1), frame);
				break;
			case PUTFIELD:
				this.evalPutField(opcode, iter.u16bitAt(pos + 1), frame);
				break;
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
				this.evalInvokeMethod(opcode, iter.u16bitAt(pos + 1), frame);
				break;
			case INVOKEINTERFACE:
				this.evalInvokeIntfMethod(opcode, iter.u16bitAt(pos + 1), frame);
				break;
			case INVOKEDYNAMIC:
				this.evalInvokeDynamic(opcode, iter.u16bitAt(pos + 1), frame);
				break;
			case NEW:
				frame.push(this.resolveClassInfo(this.constPool.getClassInfo(iter.u16bitAt(pos + 1))));
				break;
			case NEWARRAY:
				this.evalNewArray(pos, iter, frame);
				break;
			case ANEWARRAY:
				this.evalNewObjectArray(pos, iter, frame);
				break;
			case ARRAYLENGTH:
			{
				Type array = this.simplePop(frame);
				if (!array.isArray() && array != Type.UNINIT)
					throw new BadBytecode("Array length passed a non-array [pos = " + pos + "]: " + array);
				frame.push(Type.INTEGER);
				break;
			}
			case ATHROW:
				this.verifyAssignable(this.THROWABLE_TYPE, this.simplePop(frame));
				break;
			case CHECKCAST:
				this.verifyAssignable(Type.OBJECT, this.simplePop(frame));
				frame.push(this.typeFromDesc(this.constPool.getClassInfoByDescriptor(iter.u16bitAt(pos + 1))));
				break;
			case INSTANCEOF:
				this.verifyAssignable(Type.OBJECT, this.simplePop(frame));
				frame.push(Type.INTEGER);
				break;
			case MONITORENTER:
			case MONITOREXIT:
				this.verifyAssignable(Type.OBJECT, this.simplePop(frame));
				break;
			case WIDE:
				this.evalWide(pos, iter, frame, subroutine);
				break;
			case MULTIANEWARRAY:
				this.evalNewObjectArray(pos, iter, frame);
				break;
			case IFNULL:
			case IFNONNULL:
				this.verifyAssignable(Type.OBJECT, this.simplePop(frame));
				break;
			case GOTO_W:
				break;
			case JSR_W:
				frame.push(Type.RETURN_ADDRESS);
				break;
		}
	}

	private Type getType(String name) throws BadBytecode
	{
		try
		{
			return Type.get(this.classPool.get(name));
		} catch (NotFoundException e)
		{
			throw new BadBytecode("Could not find class [pos = " + this.lastPos + "]: " + name);
		}
	}

	private Type[] paramTypesFromDesc(String desc) throws BadBytecode
	{
		CtClass classes[] = null;
		try
		{
			classes = Descriptor.getParameterTypes(desc, this.classPool);
		} catch (NotFoundException e)
		{
			throw new BadBytecode("Could not find class in descriptor [pos = " + this.lastPos + "]: " + e.getMessage());
		}

		if (classes == null)
			throw new BadBytecode("Could not obtain parameters for descriptor [pos = " + this.lastPos + "]: " + desc);

		Type[] types = new Type[classes.length];
		for (int i = 0; i < types.length; i++)
			types[i] = Type.get(classes[i]);

		return types;
	}

	private Type resolveClassInfo(String info) throws BadBytecode
	{
		CtClass clazz = null;
		try
		{
			if (info.charAt(0) == '[')
				clazz = Descriptor.toCtClass(info, this.classPool);
			else
				clazz = this.classPool.get(info);

		} catch (NotFoundException e)
		{
			throw new BadBytecode("Could not find class in descriptor [pos = " + this.lastPos + "]: " + e.getMessage());
		}

		if (clazz == null)
			throw new BadBytecode("Could not obtain type for descriptor [pos = " + this.lastPos + "]: " + info);

		return Type.get(clazz);
	}

	private Type returnTypeFromDesc(String desc) throws BadBytecode
	{
		CtClass clazz = null;
		try
		{
			clazz = Descriptor.getReturnType(desc, this.classPool);
		} catch (NotFoundException e)
		{
			throw new BadBytecode("Could not find class in descriptor [pos = " + this.lastPos + "]: " + e.getMessage());
		}

		if (clazz == null)
			throw new BadBytecode("Could not obtain return type for descriptor [pos = " + this.lastPos + "]: " + desc);

		return Type.get(clazz);
	}

	private Type simplePeek(Frame frame)
	{
		Type type = frame.peek();
		return type == Type.TOP ? frame.getStack(frame.getTopIndex() - 1) : type;
	}

	private Type simplePop(Frame frame)
	{
		Type type = frame.pop();
		return type == Type.TOP ? frame.pop() : type;
	}

	private void simplePush(Type type, Frame frame)
	{
		frame.push(type);
		if (type.getSize() == 2)
			frame.push(Type.TOP);
	}

	private void simpleSetLocal(int index, Type type, Frame frame)
	{
		frame.setLocal(index, type);
		if (type.getSize() == 2)
			frame.setLocal(index + 1, Type.TOP);
	}

	private Type typeFromDesc(String desc) throws BadBytecode
	{
		CtClass clazz = null;
		try
		{
			clazz = Descriptor.toCtClass(desc, this.classPool);
		} catch (NotFoundException e)
		{
			throw new BadBytecode("Could not find class in descriptor [pos = " + this.lastPos + "]: " + e.getMessage());
		}

		if (clazz == null)
			throw new BadBytecode("Could not obtain type for descriptor [pos = " + this.lastPos + "]: " + desc);

		return Type.get(clazz);
	}

	private void verifyAssignable(Type expected, Type type) throws BadBytecode
	{
		if (!expected.isAssignableFrom(type))
			throw new BadBytecode("Expected type: " + expected + " Got: " + type + " [pos = " + this.lastPos + "]");
	}

	private Type zeroExtend(Type type)
	{
		if (type == Type.SHORT || type == Type.BYTE || type == Type.CHAR || type == Type.BOOLEAN)
			return Type.INTEGER;

		return type;
	}
}
