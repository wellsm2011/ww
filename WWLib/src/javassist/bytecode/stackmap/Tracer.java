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

package javassist.bytecode.stackmap;

import javassist.ClassPool;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ByteArray;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;

/*
 * A class for performing abstract interpretation.
 * See also MapMaker class.
 */

public abstract class Tracer implements TypeTag
{
	private static String getFieldClassName(String desc, int index)
	{
		return desc.substring(index + 1, desc.length() - 1).replace('/', '.');
	}

	protected ClassPool	classPool;
	protected ConstPool	cpool;

	protected String		returnType;			// used as the type of ARETURN
	protected int			stackTop;
	protected TypeData[]	stackTypes;

	protected TypeData[] localsTypes;

	public Tracer(ClassPool classes, ConstPool cp, int maxStack, int maxLocals, String retType)
	{
		this.classPool = classes;
		this.cpool = cp;
		this.returnType = retType;
		this.stackTop = 0;
		this.stackTypes = TypeData.make(maxStack);
		this.localsTypes = TypeData.make(maxLocals);
	}

	public Tracer(Tracer t)
	{
		this.classPool = t.classPool;
		this.cpool = t.cpool;
		this.returnType = t.returnType;
		this.stackTop = t.stackTop;
		this.stackTypes = TypeData.make(t.stackTypes.length);
		this.localsTypes = TypeData.make(t.localsTypes.length);
	}

	private void checkParamTypes(String desc, int i) throws BadBytecode
	{
		char c = desc.charAt(i);
		if (c == ')')
			return;

		int k = i;
		boolean array = false;
		while (c == '[')
		{
			array = true;
			c = desc.charAt(++k);
		}

		if (c == 'L')
		{
			k = desc.indexOf(';', k) + 1;
			if (k <= 0)
				throw new IndexOutOfBoundsException("bad descriptor");
		} else
			k++;

		this.checkParamTypes(desc, k);
		if (!array && (c == 'J' || c == 'D'))
			this.stackTop -= 2;
		else
			this.stackTop--;

		if (array)
			this.stackTypes[this.stackTop].setType(desc.substring(i, k), this.classPool);
		else if (c == 'L')
			this.stackTypes[this.stackTop].setType(desc.substring(i + 1, k - 1).replace('/', '.'), this.classPool);
	}

	/*
	 * This is a constructor call on an uninitialized object. Sets flags of
	 * other references to that object.
	 * @param offset the offset where the object has been created.
	 */
	private void constructorCalled(TypeData target, int offset)
	{
		target.constructorCalled(offset);
		for (int i = 0; i < this.stackTop; i++)
			this.stackTypes[i].constructorCalled(offset);

		for (int i = 0; i < this.localsTypes.length; i++)
			this.localsTypes[i].constructorCalled(offset);
	}

	private int doALOAD(int localVar)
	{
		this.stackTypes[this.stackTop++] = this.localsTypes[localVar];
		return 2;
	}

	private int doASTORE(int index)
	{
		this.stackTop--;
		// implicit upcast might be done.
		this.localsTypes[index] = this.stackTypes[this.stackTop];
		return 2;
	}

	private void doDUP_XX(int delta, int len)
	{
		TypeData types[] = this.stackTypes;
		int sp = this.stackTop - 1;
		int end = sp - len;
		while (sp > end)
		{
			types[sp + delta] = types[sp];
			sp--;
		}
	}

	private int doGetField(int pos, byte[] code, boolean notStatic) throws BadBytecode
	{
		int index = ByteArray.readU16bit(code, pos + 1);
		this.setFieldTarget(notStatic, index);
		String desc = this.cpool.getFieldrefType(index);
		this.pushMemberType(desc);
		return 3;
	}

	private int doInvokeDynamic(int pos, byte[] code) throws BadBytecode
	{
		int i = ByteArray.readU16bit(code, pos + 1);
		String desc = this.cpool.getInvokeDynamicType(i);
		this.checkParamTypes(desc, 1);

		// assume CosntPool#REF_invokeStatic
		/*
		 * TypeData target = stackTypes[--stackTop]; if (target instanceof
		 * TypeData.UninitTypeVar && target.isUninit())
		 * constructorCalled((TypeData.UninitTypeVar)target);
		 */

		this.pushMemberType(desc);
		return 5;
	}

	private int doInvokeIntfMethod(int pos, byte[] code) throws BadBytecode
	{
		int i = ByteArray.readU16bit(code, pos + 1);
		String desc = this.cpool.getInterfaceMethodrefType(i);
		this.checkParamTypes(desc, 1);
		String className = this.cpool.getInterfaceMethodrefClassName(i);
		this.stackTypes[--this.stackTop].setType(className, this.classPool);
		this.pushMemberType(desc);
		return 5;
	}

	private int doInvokeMethod(int pos, byte[] code, boolean notStatic) throws BadBytecode
	{
		int i = ByteArray.readU16bit(code, pos + 1);
		String desc = this.cpool.getMethodrefType(i);
		this.checkParamTypes(desc, 1);
		if (notStatic)
		{
			String className = this.cpool.getMethodrefClassName(i);
			TypeData target = this.stackTypes[--this.stackTop];
			if (target instanceof TypeData.UninitTypeVar && target.isUninit())
				this.constructorCalled(target, ((TypeData.UninitTypeVar) target).offset());
			else if (target instanceof TypeData.UninitData)
				this.constructorCalled(target, ((TypeData.UninitData) target).offset());

			target.setType(className, this.classPool);
		}

		this.pushMemberType(desc);
		return 3;
	}

	private void doLDC(int index)
	{
		TypeData[] stackTypes = this.stackTypes;
		int tag = this.cpool.getTag(index);
		if (tag == ConstPool.CONST_String)
			stackTypes[this.stackTop++] = new TypeData.ClassName("java.lang.String");
		else if (tag == ConstPool.CONST_Integer)
			stackTypes[this.stackTop++] = TypeTag.INTEGER;
		else if (tag == ConstPool.CONST_Float)
			stackTypes[this.stackTop++] = TypeTag.FLOAT;
		else if (tag == ConstPool.CONST_Long)
		{
			stackTypes[this.stackTop++] = TypeTag.LONG;
			stackTypes[this.stackTop++] = TypeTag.TOP;
		} else if (tag == ConstPool.CONST_Double)
		{
			stackTypes[this.stackTop++] = TypeTag.DOUBLE;
			stackTypes[this.stackTop++] = TypeTag.TOP;
		} else if (tag == ConstPool.CONST_Class)
			stackTypes[this.stackTop++] = new TypeData.ClassName("java.lang.Class");
		else
			throw new RuntimeException("bad LDC: " + tag);
	}

	private int doMultiANewArray(int pos, byte[] code)
	{
		int i = ByteArray.readU16bit(code, pos + 1);
		int dim = code[pos + 3] & 0xff;
		this.stackTop -= dim - 1;

		String type = this.cpool.getClassInfo(i).replace('.', '/');
		this.stackTypes[this.stackTop - 1] = new TypeData.ClassName(type);
		return 4;
	}

	private int doNEWARRAY(int pos, byte[] code)
	{
		int s = this.stackTop - 1;
		String type;
		switch (code[pos + 1] & 0xff)
		{
			case Opcode.T_BOOLEAN:
				type = "[Z";
				break;
			case Opcode.T_CHAR:
				type = "[C";
				break;
			case Opcode.T_FLOAT:
				type = "[F";
				break;
			case Opcode.T_DOUBLE:
				type = "[D";
				break;
			case Opcode.T_BYTE:
				type = "[B";
				break;
			case Opcode.T_SHORT:
				type = "[S";
				break;
			case Opcode.T_INT:
				type = "[I";
				break;
			case Opcode.T_LONG:
				type = "[J";
				break;
			default:
				throw new RuntimeException("bad newarray");
		}

		this.stackTypes[s] = new TypeData.ClassName(type);
		return 2;
	}

	/**
	 * Does abstract interpretation on the given bytecode instruction. It
	 * records whether or not a local variable (i.e. register) is accessed. If
	 * the instruction requires that a local variable or a stack element has a
	 * more specific type, this method updates the type of it.
	 *
	 * @param pos
	 *            the position of the instruction.
	 * @return the size of the instruction at POS.
	 */
	protected int doOpcode(int pos, byte[] code) throws BadBytecode
	{
		try
		{
			int op = code[pos] & 0xff;
			if (op < 96)
				if (op < 54)
					return this.doOpcode0_53(pos, code, op);
				else
					return this.doOpcode54_95(pos, code, op);
			else if (op < 148)
				return this.doOpcode96_147(pos, code, op);
			else
				return this.doOpcode148_201(pos, code, op);
		} catch (ArrayIndexOutOfBoundsException e)
		{
			throw new BadBytecode("inconsistent stack height " + e.getMessage(), e);
		}
	}

	private int doOpcode0_53(int pos, byte[] code, int op) throws BadBytecode
	{
		int reg;
		TypeData[] stackTypes = this.stackTypes;
		switch (op)
		{
			case Opcode.NOP:
				break;
			case Opcode.ACONST_NULL:
				stackTypes[this.stackTop++] = new TypeData.NullType();
				break;
			case Opcode.ICONST_M1:
			case Opcode.ICONST_0:
			case Opcode.ICONST_1:
			case Opcode.ICONST_2:
			case Opcode.ICONST_3:
			case Opcode.ICONST_4:
			case Opcode.ICONST_5:
				stackTypes[this.stackTop++] = TypeTag.INTEGER;
				break;
			case Opcode.LCONST_0:
			case Opcode.LCONST_1:
				stackTypes[this.stackTop++] = TypeTag.LONG;
				stackTypes[this.stackTop++] = TypeTag.TOP;
				break;
			case Opcode.FCONST_0:
			case Opcode.FCONST_1:
			case Opcode.FCONST_2:
				stackTypes[this.stackTop++] = TypeTag.FLOAT;
				break;
			case Opcode.DCONST_0:
			case Opcode.DCONST_1:
				stackTypes[this.stackTop++] = TypeTag.DOUBLE;
				stackTypes[this.stackTop++] = TypeTag.TOP;
				break;
			case Opcode.BIPUSH:
			case Opcode.SIPUSH:
				stackTypes[this.stackTop++] = TypeTag.INTEGER;
				return op == Opcode.SIPUSH ? 3 : 2;
			case Opcode.LDC:
				this.doLDC(code[pos + 1] & 0xff);
				return 2;
			case Opcode.LDC_W:
			case Opcode.LDC2_W:
				this.doLDC(ByteArray.readU16bit(code, pos + 1));
				return 3;
			case Opcode.ILOAD:
				return this.doXLOAD(TypeTag.INTEGER, code, pos);
			case Opcode.LLOAD:
				return this.doXLOAD(TypeTag.LONG, code, pos);
			case Opcode.FLOAD:
				return this.doXLOAD(TypeTag.FLOAT, code, pos);
			case Opcode.DLOAD:
				return this.doXLOAD(TypeTag.DOUBLE, code, pos);
			case Opcode.ALOAD:
				return this.doALOAD(code[pos + 1] & 0xff);
			case Opcode.ILOAD_0:
			case Opcode.ILOAD_1:
			case Opcode.ILOAD_2:
			case Opcode.ILOAD_3:
				stackTypes[this.stackTop++] = TypeTag.INTEGER;
				break;
			case Opcode.LLOAD_0:
			case Opcode.LLOAD_1:
			case Opcode.LLOAD_2:
			case Opcode.LLOAD_3:
				stackTypes[this.stackTop++] = TypeTag.LONG;
				stackTypes[this.stackTop++] = TypeTag.TOP;
				break;
			case Opcode.FLOAD_0:
			case Opcode.FLOAD_1:
			case Opcode.FLOAD_2:
			case Opcode.FLOAD_3:
				stackTypes[this.stackTop++] = TypeTag.FLOAT;
				break;
			case Opcode.DLOAD_0:
			case Opcode.DLOAD_1:
			case Opcode.DLOAD_2:
			case Opcode.DLOAD_3:
				stackTypes[this.stackTop++] = TypeTag.DOUBLE;
				stackTypes[this.stackTop++] = TypeTag.TOP;
				break;
			case Opcode.ALOAD_0:
			case Opcode.ALOAD_1:
			case Opcode.ALOAD_2:
			case Opcode.ALOAD_3:
				reg = op - Opcode.ALOAD_0;
				stackTypes[this.stackTop++] = this.localsTypes[reg];
				break;
			case Opcode.IALOAD:
				stackTypes[--this.stackTop - 1] = TypeTag.INTEGER;
				break;
			case Opcode.LALOAD:
				stackTypes[this.stackTop - 2] = TypeTag.LONG;
				stackTypes[this.stackTop - 1] = TypeTag.TOP;
				break;
			case Opcode.FALOAD:
				stackTypes[--this.stackTop - 1] = TypeTag.FLOAT;
				break;
			case Opcode.DALOAD:
				stackTypes[this.stackTop - 2] = TypeTag.DOUBLE;
				stackTypes[this.stackTop - 1] = TypeTag.TOP;
				break;
			case Opcode.AALOAD:
			{
				int s = --this.stackTop - 1;
				TypeData data = stackTypes[s];
				stackTypes[s] = TypeData.ArrayElement.make(data);
				break;
			}
			case Opcode.BALOAD:
			case Opcode.CALOAD:
			case Opcode.SALOAD:
				stackTypes[--this.stackTop - 1] = TypeTag.INTEGER;
				break;
			default:
				throw new RuntimeException("fatal");
		}

		return 1;
	}

	private int doOpcode148_201(int pos, byte[] code, int op) throws BadBytecode
	{
		switch (op)
		{
			case Opcode.LCMP:
				this.stackTypes[this.stackTop - 4] = TypeTag.INTEGER;
				this.stackTop -= 3;
				break;
			case Opcode.FCMPL:
			case Opcode.FCMPG:
				this.stackTypes[--this.stackTop - 1] = TypeTag.INTEGER;
				break;
			case Opcode.DCMPL:
			case Opcode.DCMPG:
				this.stackTypes[this.stackTop - 4] = TypeTag.INTEGER;
				this.stackTop -= 3;
				break;
			case Opcode.IFEQ:
			case Opcode.IFNE:
			case Opcode.IFLT:
			case Opcode.IFGE:
			case Opcode.IFGT:
			case Opcode.IFLE:
				this.stackTop--; // branch
				this.visitBranch(pos, code, ByteArray.readS16bit(code, pos + 1));
				return 3;
			case Opcode.IF_ICMPEQ:
			case Opcode.IF_ICMPNE:
			case Opcode.IF_ICMPLT:
			case Opcode.IF_ICMPGE:
			case Opcode.IF_ICMPGT:
			case Opcode.IF_ICMPLE:
			case Opcode.IF_ACMPEQ:
			case Opcode.IF_ACMPNE:
				this.stackTop -= 2; // branch
				this.visitBranch(pos, code, ByteArray.readS16bit(code, pos + 1));
				return 3;
			case Opcode.GOTO:
				this.visitGoto(pos, code, ByteArray.readS16bit(code, pos + 1));
				return 3; // branch
			case Opcode.JSR:
				this.visitJSR(pos, code);
				return 3; // branch
			case Opcode.RET:
				this.visitRET(pos, code);
				return 2;
			case Opcode.TABLESWITCH:
			{
				this.stackTop--; // branch
				int pos2 = (pos & ~3) + 8;
				int low = ByteArray.read32bit(code, pos2);
				int high = ByteArray.read32bit(code, pos2 + 4);
				int n = high - low + 1;
				this.visitTableSwitch(pos, code, n, pos2 + 8, ByteArray.read32bit(code, pos2 - 4));
				return n * 4 + 16 - (pos & 3);
			}
			case Opcode.LOOKUPSWITCH:
			{
				this.stackTop--; // branch
				int pos2 = (pos & ~3) + 8;
				int n = ByteArray.read32bit(code, pos2);
				this.visitLookupSwitch(pos, code, n, pos2 + 4, ByteArray.read32bit(code, pos2 - 4));
				return n * 8 + 12 - (pos & 3);
			}
			case Opcode.IRETURN:
				this.stackTop--;
				this.visitReturn(pos, code);
				break;
			case Opcode.LRETURN:
				this.stackTop -= 2;
				this.visitReturn(pos, code);
				break;
			case Opcode.FRETURN:
				this.stackTop--;
				this.visitReturn(pos, code);
				break;
			case Opcode.DRETURN:
				this.stackTop -= 2;
				this.visitReturn(pos, code);
				break;
			case Opcode.ARETURN:
				this.stackTypes[--this.stackTop].setType(this.returnType, this.classPool);
				this.visitReturn(pos, code);
				break;
			case Opcode.RETURN:
				this.visitReturn(pos, code);
				break;
			case Opcode.GETSTATIC:
				return this.doGetField(pos, code, false);
			case Opcode.PUTSTATIC:
				return this.doPutField(pos, code, false);
			case Opcode.GETFIELD:
				return this.doGetField(pos, code, true);
			case Opcode.PUTFIELD:
				return this.doPutField(pos, code, true);
			case Opcode.INVOKEVIRTUAL:
			case Opcode.INVOKESPECIAL:
				return this.doInvokeMethod(pos, code, true);
			case Opcode.INVOKESTATIC:
				return this.doInvokeMethod(pos, code, false);
			case Opcode.INVOKEINTERFACE:
				return this.doInvokeIntfMethod(pos, code);
			case Opcode.INVOKEDYNAMIC:
				return this.doInvokeDynamic(pos, code);
			case Opcode.NEW:
			{
				int i = ByteArray.readU16bit(code, pos + 1);
				this.stackTypes[this.stackTop++] = new TypeData.UninitData(pos, this.cpool.getClassInfo(i));
				return 3;
			}
			case Opcode.NEWARRAY:
				return this.doNEWARRAY(pos, code);
			case Opcode.ANEWARRAY:
			{
				int i = ByteArray.readU16bit(code, pos + 1);
				String type = this.cpool.getClassInfo(i).replace('.', '/');
				if (type.charAt(0) == '[')
					type = "[" + type;
				else
					type = "[L" + type + ";";

				this.stackTypes[this.stackTop - 1] = new TypeData.ClassName(type);
				return 3;
			}
			case Opcode.ARRAYLENGTH:
				this.stackTypes[this.stackTop - 1].setType("[Ljava.lang.Object;", this.classPool);
				this.stackTypes[this.stackTop - 1] = TypeTag.INTEGER;
				break;
			case Opcode.ATHROW:
				this.stackTypes[--this.stackTop].setType("java.lang.Throwable", this.classPool);
				this.visitThrow(pos, code);
				break;
			case Opcode.CHECKCAST:
			{
				// TypeData.setType(stackTypes[stackTop - 1],
				// "java.lang.Object", classPool);
				int i = ByteArray.readU16bit(code, pos + 1);
				String type = this.cpool.getClassInfo(i);
				if (type.charAt(0) == '[')
					type = type.replace('.', '/'); // getClassInfo() may return
				// "[java.lang.Object;".

				this.stackTypes[this.stackTop - 1] = new TypeData.ClassName(type);
				return 3;
			}
			case Opcode.INSTANCEOF:
				// TypeData.setType(stackTypes[stackTop - 1],
				// "java.lang.Object", classPool);
				this.stackTypes[this.stackTop - 1] = TypeTag.INTEGER;
				return 3;
			case Opcode.MONITORENTER:
			case Opcode.MONITOREXIT:
				this.stackTop--;
				// TypeData.setType(stackTypes[stackTop], "java.lang.Object",
				// classPool);
				break;
			case Opcode.WIDE:
				return this.doWIDE(pos, code);
			case Opcode.MULTIANEWARRAY:
				return this.doMultiANewArray(pos, code);
			case Opcode.IFNULL:
			case Opcode.IFNONNULL:
				this.stackTop--; // branch
				this.visitBranch(pos, code, ByteArray.readS16bit(code, pos + 1));
				return 3;
			case Opcode.GOTO_W:
				this.visitGoto(pos, code, ByteArray.read32bit(code, pos + 1));
				return 5; // branch
			case Opcode.JSR_W:
				this.visitJSR(pos, code);
				return 5;
		}
		return 1;
	}

	private int doOpcode54_95(int pos, byte[] code, int op) throws BadBytecode
	{
		switch (op)
		{
			case Opcode.ISTORE:
				return this.doXSTORE(pos, code, TypeTag.INTEGER);
			case Opcode.LSTORE:
				return this.doXSTORE(pos, code, TypeTag.LONG);
			case Opcode.FSTORE:
				return this.doXSTORE(pos, code, TypeTag.FLOAT);
			case Opcode.DSTORE:
				return this.doXSTORE(pos, code, TypeTag.DOUBLE);
			case Opcode.ASTORE:
				return this.doASTORE(code[pos + 1] & 0xff);
			case Opcode.ISTORE_0:
			case Opcode.ISTORE_1:
			case Opcode.ISTORE_2:
			case Opcode.ISTORE_3:
			{
				int var = op - Opcode.ISTORE_0;
				this.localsTypes[var] = TypeTag.INTEGER;
				this.stackTop--;
			}
				break;
			case Opcode.LSTORE_0:
			case Opcode.LSTORE_1:
			case Opcode.LSTORE_2:
			case Opcode.LSTORE_3:
			{
				int var = op - Opcode.LSTORE_0;
				this.localsTypes[var] = TypeTag.LONG;
				this.localsTypes[var + 1] = TypeTag.TOP;
				this.stackTop -= 2;
			}
				break;
			case Opcode.FSTORE_0:
			case Opcode.FSTORE_1:
			case Opcode.FSTORE_2:
			case Opcode.FSTORE_3:
			{
				int var = op - Opcode.FSTORE_0;
				this.localsTypes[var] = TypeTag.FLOAT;
				this.stackTop--;
			}
				break;
			case Opcode.DSTORE_0:
			case Opcode.DSTORE_1:
			case Opcode.DSTORE_2:
			case Opcode.DSTORE_3:
			{
				int var = op - Opcode.DSTORE_0;
				this.localsTypes[var] = TypeTag.DOUBLE;
				this.localsTypes[var + 1] = TypeTag.TOP;
				this.stackTop -= 2;
			}
				break;
			case Opcode.ASTORE_0:
			case Opcode.ASTORE_1:
			case Opcode.ASTORE_2:
			case Opcode.ASTORE_3:
			{
				int var = op - Opcode.ASTORE_0;
				this.doASTORE(var);
				break;
			}
			case Opcode.IASTORE:
			case Opcode.LASTORE:
			case Opcode.FASTORE:
			case Opcode.DASTORE:
				this.stackTop -= op == Opcode.LASTORE || op == Opcode.DASTORE ? 4 : 3;
				break;
			case Opcode.AASTORE:
				TypeData.aastore(this.stackTypes[this.stackTop - 3], this.stackTypes[this.stackTop - 1], this.classPool);
				this.stackTop -= 3;
				break;
			case Opcode.BASTORE:
			case Opcode.CASTORE:
			case Opcode.SASTORE:
				this.stackTop -= 3;
				break;
			case Opcode.POP:
				this.stackTop--;
				break;
			case Opcode.POP2:
				this.stackTop -= 2;
				break;
			case Opcode.DUP:
			{
				int sp = this.stackTop;
				this.stackTypes[sp] = this.stackTypes[sp - 1];
				this.stackTop = sp + 1;
				break;
			}
			case Opcode.DUP_X1:
			case Opcode.DUP_X2:
			{
				int len = op - Opcode.DUP_X1 + 2;
				this.doDUP_XX(1, len);
				int sp = this.stackTop;
				this.stackTypes[sp - len] = this.stackTypes[sp];
				this.stackTop = sp + 1;
				break;
			}
			case Opcode.DUP2:
				this.doDUP_XX(2, 2);
				this.stackTop += 2;
				break;
			case Opcode.DUP2_X1:
			case Opcode.DUP2_X2:
			{
				int len = op - Opcode.DUP2_X1 + 3;
				this.doDUP_XX(2, len);
				int sp = this.stackTop;
				this.stackTypes[sp - len] = this.stackTypes[sp];
				this.stackTypes[sp - len + 1] = this.stackTypes[sp + 1];
				this.stackTop = sp + 2;
				break;
			}
			case Opcode.SWAP:
			{
				int sp = this.stackTop - 1;
				TypeData t = this.stackTypes[sp];
				this.stackTypes[sp] = this.stackTypes[sp - 1];
				this.stackTypes[sp - 1] = t;
				break;
			}
			default:
				throw new RuntimeException("fatal");
		}

		return 1;
	}

	private int doOpcode96_147(int pos, byte[] code, int op)
	{
		if (op <= Opcode.LXOR)
		{ // IADD...LXOR
			this.stackTop += Opcode.STACK_GROW[op];
			return 1;
		}

		switch (op)
		{
			case Opcode.IINC:
				// this does not call writeLocal().
				return 3;
			case Opcode.I2L:
				this.stackTypes[this.stackTop - 1] = TypeTag.LONG;
				this.stackTypes[this.stackTop] = TypeTag.TOP;
				this.stackTop++;
				break;
			case Opcode.I2F:
				this.stackTypes[this.stackTop - 1] = TypeTag.FLOAT;
				break;
			case Opcode.I2D:
				this.stackTypes[this.stackTop - 1] = TypeTag.DOUBLE;
				this.stackTypes[this.stackTop] = TypeTag.TOP;
				this.stackTop++;
				break;
			case Opcode.L2I:
				this.stackTypes[--this.stackTop - 1] = TypeTag.INTEGER;
				break;
			case Opcode.L2F:
				this.stackTypes[--this.stackTop - 1] = TypeTag.FLOAT;
				break;
			case Opcode.L2D:
				this.stackTypes[this.stackTop - 2] = TypeTag.DOUBLE;
				break;
			case Opcode.F2I:
				this.stackTypes[this.stackTop - 1] = TypeTag.INTEGER;
				break;
			case Opcode.F2L:
				this.stackTypes[this.stackTop - 1] = TypeTag.LONG;
				this.stackTypes[this.stackTop] = TypeTag.TOP;
				this.stackTop++;
				break;
			case Opcode.F2D:
				this.stackTypes[this.stackTop - 1] = TypeTag.DOUBLE;
				this.stackTypes[this.stackTop] = TypeTag.TOP;
				this.stackTop++;
				break;
			case Opcode.D2I:
				this.stackTypes[--this.stackTop - 1] = TypeTag.INTEGER;
				break;
			case Opcode.D2L:
				this.stackTypes[this.stackTop - 2] = TypeTag.LONG;
				break;
			case Opcode.D2F:
				this.stackTypes[--this.stackTop - 1] = TypeTag.FLOAT;
				break;
			case Opcode.I2B:
			case Opcode.I2C:
			case Opcode.I2S:
				break;
			default:
				throw new RuntimeException("fatal");
		}

		return 1;
	}

	private int doPutField(int pos, byte[] code, boolean notStatic) throws BadBytecode
	{
		int index = ByteArray.readU16bit(code, pos + 1);
		String desc = this.cpool.getFieldrefType(index);
		this.stackTop -= Descriptor.dataSize(desc);
		char c = desc.charAt(0);
		if (c == 'L')
			this.stackTypes[this.stackTop].setType(Tracer.getFieldClassName(desc, 0), this.classPool);
		else if (c == '[')
			this.stackTypes[this.stackTop].setType(desc, this.classPool);

		this.setFieldTarget(notStatic, index);
		return 3;
	}

	private int doWIDE(int pos, byte[] code) throws BadBytecode
	{
		int op = code[pos + 1] & 0xff;
		switch (op)
		{
			case Opcode.ILOAD:
				this.doWIDE_XLOAD(pos, code, TypeTag.INTEGER);
				break;
			case Opcode.LLOAD:
				this.doWIDE_XLOAD(pos, code, TypeTag.LONG);
				break;
			case Opcode.FLOAD:
				this.doWIDE_XLOAD(pos, code, TypeTag.FLOAT);
				break;
			case Opcode.DLOAD:
				this.doWIDE_XLOAD(pos, code, TypeTag.DOUBLE);
				break;
			case Opcode.ALOAD:
			{
				int index = ByteArray.readU16bit(code, pos + 2);
				this.doALOAD(index);
				break;
			}
			case Opcode.ISTORE:
				this.doWIDE_STORE(pos, code, TypeTag.INTEGER);
				break;
			case Opcode.LSTORE:
				this.doWIDE_STORE(pos, code, TypeTag.LONG);
				break;
			case Opcode.FSTORE:
				this.doWIDE_STORE(pos, code, TypeTag.FLOAT);
				break;
			case Opcode.DSTORE:
				this.doWIDE_STORE(pos, code, TypeTag.DOUBLE);
				break;
			case Opcode.ASTORE:
			{
				int index = ByteArray.readU16bit(code, pos + 2);
				this.doASTORE(index);
				break;
			}
			case Opcode.IINC:
				// this does not call writeLocal().
				return 6;
			case Opcode.RET:
				this.visitRET(pos, code);
				break;
			default:
				throw new RuntimeException("bad WIDE instruction: " + op);
		}

		return 4;
	}

	private void doWIDE_STORE(int pos, byte[] code, TypeData type)
	{
		int index = ByteArray.readU16bit(code, pos + 2);
		this.doXSTORE(index, type);
	}

	private void doWIDE_XLOAD(int pos, byte[] code, TypeData type)
	{
		int index = ByteArray.readU16bit(code, pos + 2);
		this.doXLOAD(index, type);
	}

	private int doXLOAD(int localVar, TypeData type)
	{
		this.stackTypes[this.stackTop++] = type;
		if (type.is2WordType())
			this.stackTypes[this.stackTop++] = TypeTag.TOP;

		return 2;
	}

	private int doXLOAD(TypeData type, byte[] code, int pos)
	{
		int localVar = code[pos + 1] & 0xff;
		return this.doXLOAD(localVar, type);
	}

	private int doXSTORE(int pos, byte[] code, TypeData type)
	{
		int index = code[pos + 1] & 0xff;
		return this.doXSTORE(index, type);
	}

	private int doXSTORE(int index, TypeData type)
	{
		this.stackTop--;
		this.localsTypes[index] = type;
		if (type.is2WordType())
		{
			this.stackTop--;
			this.localsTypes[index + 1] = TypeTag.TOP;
		}

		return 2;
	}

	private void pushMemberType(String descriptor)
	{
		int top = 0;
		if (descriptor.charAt(0) == '(')
		{
			top = descriptor.indexOf(')') + 1;
			if (top < 1)
				throw new IndexOutOfBoundsException("bad descriptor: " + descriptor);
		}

		TypeData[] types = this.stackTypes;
		int index = this.stackTop;
		switch (descriptor.charAt(top))
		{
			case '[':
				types[index] = new TypeData.ClassName(descriptor.substring(top));
				break;
			case 'L':
				types[index] = new TypeData.ClassName(Tracer.getFieldClassName(descriptor, top));
				break;
			case 'J':
				types[index] = TypeTag.LONG;
				types[index + 1] = TypeTag.TOP;
				this.stackTop += 2;
				return;
			case 'F':
				types[index] = TypeTag.FLOAT;
				break;
			case 'D':
				types[index] = TypeTag.DOUBLE;
				types[index + 1] = TypeTag.TOP;
				this.stackTop += 2;
				return;
			case 'V':
				return;
			default: // C, B, S, I, Z
				types[index] = TypeTag.INTEGER;
				break;
		}

		this.stackTop++;
	}

	private void setFieldTarget(boolean notStatic, int index) throws BadBytecode
	{
		if (notStatic)
		{
			String className = this.cpool.getFieldrefClassName(index);
			this.stackTypes[--this.stackTop].setType(className, this.classPool);
		}
	}

	protected void visitBranch(int pos, byte[] code, int offset) throws BadBytecode
	{
	}

	protected void visitGoto(int pos, byte[] code, int offset) throws BadBytecode
	{
	}

	/**
	 * Invoked when the visited instruction is jsr. Java6 or later does not
	 * allow using RET.
	 */
	protected void visitJSR(int pos, byte[] code) throws BadBytecode
	{
		/*
		 * Since JSR pushes a return address onto the operand stack, the stack
		 * map at the entry point of a subroutine is stackTypes resulting after
		 * executing the following code: stackTypes[stackTop++] = TOP;
		 */
	}

	/**
	 * @param pos
	 *            the position of LOOKUPSWITCH
	 * @param code
	 *            bytecode
	 * @param n
	 *            the number of case labels
	 * @param pairsPos
	 *            the position of the table of pairs of a value and a branch
	 *            target.
	 * @param defaultOffset
	 *            the offset to the default branch target.
	 */
	protected void visitLookupSwitch(int pos, byte[] code, int n, int pairsPos, int defaultOffset) throws BadBytecode
	{
	}

	/**
	 * Invoked when the visited instruction is ret or wide ret. Java6 or later
	 * does not allow using RET.
	 */
	protected void visitRET(int pos, byte[] code) throws BadBytecode
	{
	}

	protected void visitReturn(int pos, byte[] code) throws BadBytecode
	{
	}

	/**
	 * @param pos
	 *            the position of TABLESWITCH
	 * @param code
	 *            bytecode
	 * @param n
	 *            the number of case labels
	 * @param offsetPos
	 *            the position of the branch-target table.
	 * @param defaultOffset
	 *            the offset to the default branch target.
	 */
	protected void visitTableSwitch(int pos, byte[] code, int n, int offsetPos, int defaultOffset) throws BadBytecode
	{
	}

	protected void visitThrow(int pos, byte[] code) throws BadBytecode
	{
	}
}
