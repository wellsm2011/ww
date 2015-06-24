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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import javassist.CannotCompileException;

/**
 * Another <code>stack_map</code> attribute defined in CLDC 1.1 for J2ME.
 *
 * <p>
 * This is an entry in the attributes table of a Code attribute. It was
 * introduced by J2ME CLDC 1.1 (JSR 139) for pre-verification.
 *
 * <p>
 * According to the CLDC specification, the sizes of some fields are not 16bit
 * but 32bit if the code size is more than 64K or the number of the local
 * variables is more than 64K. However, for the J2ME CLDC technology, they are
 * always 16bit. The implementation of the StackMap class assumes they are
 * 16bit.
 *
 * @see MethodInfo#doPreverify
 * @see StackMapTable
 * @since 3.12
 */
public class StackMap extends AttributeInfo
{
	static class Copier extends Walker
	{
		byte[]	dest;
		ConstPool	srcCp, destCp;
		Map			classnames;

		Copier(StackMap map, ConstPool newCp, Map classnames)
		{
			super(map);
			this.srcCp = map.getConstPool();
			this.dest = new byte[this.info.length];
			this.destCp = newCp;
			this.classnames = classnames;
		}

		public StackMap getStackMap()
		{
			return new StackMap(this.destCp, this.dest);
		}

		@Override
		public int locals(int pos, int offset, int num)
		{
			ByteArray.write16bit(offset, this.dest, pos - 4);
			return super.locals(pos, offset, num);
		}

		@Override
		public void objectVariable(int pos, int clazz)
		{
			this.dest[pos] = StackMap.OBJECT;
			int newClazz = this.srcCp.copy(clazz, this.destCp, this.classnames);
			ByteArray.write16bit(newClazz, this.dest, pos + 1);
		}

		@Override
		public void typeInfo(int pos, byte tag)
		{
			this.dest[pos] = tag;
		}

		@Override
		public int typeInfoArray(int pos, int offset, int num, boolean isLocals)
		{
			ByteArray.write16bit(num, this.dest, pos - 2);
			return super.typeInfoArray(pos, offset, num, isLocals);
		}

		@Override
		public void uninitialized(int pos, int offset)
		{
			this.dest[pos] = StackMap.UNINIT;
			ByteArray.write16bit(offset, this.dest, pos + 1);
		}

		@Override
		public void visit()
		{
			int num = ByteArray.readU16bit(this.info, 0);
			ByteArray.write16bit(num, this.dest, 0);
			super.visit();
		}
	}

	static class InsertLocal extends SimpleCopy
	{
		private int	varIndex;
		private int	varTag, varData;

		InsertLocal(StackMap map, int varIndex, int varTag, int varData)
		{
			super(map);
			this.varIndex = varIndex;
			this.varTag = varTag;
			this.varData = varData;
		}

		@Override
		public int typeInfoArray(int pos, int offset, int num, boolean isLocals)
		{
			if (!isLocals || num < this.varIndex)
				return super.typeInfoArray(pos, offset, num, isLocals);

			this.writer.write16bit(num + 1);
			for (int k = 0; k < num; k++)
			{
				if (k == this.varIndex)
					this.writeVarTypeInfo();

				pos = this.typeInfoArray2(k, pos);
			}

			if (num == this.varIndex)
				this.writeVarTypeInfo();

			return pos;
		}

		private void writeVarTypeInfo()
		{
			if (this.varTag == StackMap.OBJECT)
				this.writer.writeVerifyTypeInfo(StackMap.OBJECT, this.varData);
			else if (this.varTag == StackMap.UNINIT)
				this.writer.writeVerifyTypeInfo(StackMap.UNINIT, this.varData);
			else
				this.writer.writeVerifyTypeInfo(this.varTag, 0);
		}
	}

	static class NewRemover extends SimpleCopy
	{
		int	posOfNew;

		NewRemover(StackMap map, int where)
		{
			super(map);
			this.posOfNew = where;
		}

		@Override
		public int stack(int pos, int offset, int num)
		{
			return this.stackTypeInfoArray(pos, offset, num);
		}

		private int stackTypeInfoArray(int pos, int offset, int num)
		{
			int p = pos;
			int count = 0;
			for (int k = 0; k < num; k++)
			{
				byte tag = this.info[p];
				if (tag == StackMap.OBJECT)
					p += 3;
				else if (tag == StackMap.UNINIT)
				{
					int offsetOfNew = ByteArray.readU16bit(this.info, p + 1);
					if (offsetOfNew == this.posOfNew)
						count++;

					p += 3;
				} else
					p++;
			}

			this.writer.write16bit(num - count);
			for (int k = 0; k < num; k++)
			{
				byte tag = this.info[pos];
				if (tag == StackMap.OBJECT)
				{
					int clazz = ByteArray.readU16bit(this.info, pos + 1);
					this.objectVariable(pos, clazz);
					pos += 3;
				} else if (tag == StackMap.UNINIT)
				{
					int offsetOfNew = ByteArray.readU16bit(this.info, pos + 1);
					if (offsetOfNew != this.posOfNew)
						this.uninitialized(pos, offsetOfNew);

					pos += 3;
				} else
				{
					this.typeInfo(pos, tag);
					pos++;
				}
			}

			return pos;
		}
	}

	static class Printer extends Walker
	{
		private java.io.PrintWriter	writer;

		public Printer(StackMap map, java.io.PrintWriter out)
		{
			super(map);
			this.writer = out;
		}

		@Override
		public int locals(int pos, int offset, int num)
		{
			this.writer.println("  * offset " + offset);
			return super.locals(pos, offset, num);
		}

		public void print()
		{
			int num = ByteArray.readU16bit(this.info, 0);
			this.writer.println(num + " entries");
			this.visit();
		}
	}

	static class Shifter extends Walker
	{
		private int		where, gap;
		private boolean	exclusive;

		public Shifter(StackMap smt, int where, int gap, boolean exclusive)
		{
			super(smt);
			this.where = where;
			this.gap = gap;
			this.exclusive = exclusive;
		}

		@Override
		public int locals(int pos, int offset, int num)
		{
			if (this.exclusive ? this.where <= offset : this.where < offset)
				ByteArray.write16bit(offset + this.gap, this.info, pos - 4);

			return super.locals(pos, offset, num);
		}

		@Override
		public void uninitialized(int pos, int offset)
		{
			if (this.where <= offset)
				ByteArray.write16bit(offset + this.gap, this.info, pos + 1);
		}
	}

	static class SimpleCopy extends Walker
	{
		Writer	writer;

		SimpleCopy(StackMap map)
		{
			super(map);
			this.writer = new Writer();
		}

		byte[] doit()
		{
			this.visit();
			return this.writer.toByteArray();
		}

		@Override
		public int locals(int pos, int offset, int num)
		{
			this.writer.write16bit(offset);
			return super.locals(pos, offset, num);
		}

		@Override
		public void objectVariable(int pos, int clazz)
		{
			this.writer.writeVerifyTypeInfo(StackMap.OBJECT, clazz);
		}

		@Override
		public void typeInfo(int pos, byte tag)
		{
			this.writer.writeVerifyTypeInfo(tag, 0);
		}

		@Override
		public int typeInfoArray(int pos, int offset, int num, boolean isLocals)
		{
			this.writer.write16bit(num);
			return super.typeInfoArray(pos, offset, num, isLocals);
		}

		@Override
		public void uninitialized(int pos, int offset)
		{
			this.writer.writeVerifyTypeInfo(StackMap.UNINIT, offset);
		}

		@Override
		public void visit()
		{
			int num = ByteArray.readU16bit(this.info, 0);
			this.writer.write16bit(num);
			super.visit();
		}
	}

	static class SwitchShifter extends Walker
	{
		private int	where, gap;

		public SwitchShifter(StackMap smt, int where, int gap)
		{
			super(smt);
			this.where = where;
			this.gap = gap;
		}

		@Override
		public int locals(int pos, int offset, int num)
		{
			if (this.where == pos + offset)
				ByteArray.write16bit(offset - this.gap, this.info, pos - 4);
			else if (this.where == pos)
				ByteArray.write16bit(offset + this.gap, this.info, pos - 4);

			return super.locals(pos, offset, num);
		}
	}

	/**
	 * A code walker for a StackMap attribute.
	 */
	public static class Walker
	{
		byte[]	info;

		/**
		 * Constructs a walker.
		 */
		public Walker(StackMap sm)
		{
			this.info = sm.get();
		}

		/**
		 * Invoked when <code>locals</code> of <code>stack_map_frame</code> is
		 * visited.
		 */
		public int locals(int pos, int offset, int num)
		{
			return this.typeInfoArray(pos, offset, num, true);
		}

		/**
		 * Invoked when an element of type <code>Object_variable_info</code> is
		 * visited.
		 */
		public void objectVariable(int pos, int clazz)
		{
		}

		/**
		 * Invoked when <code>stack</code> of <code>stack_map_frame</code> is
		 * visited.
		 */
		public int stack(int pos, int offset, int num)
		{
			return this.typeInfoArray(pos, offset, num, false);
		}

		/**
		 * Invoked when an element of <code>verification_type_info</code>
		 * (except <code>Object_variable_info</code> and
		 * <code>Uninitialized_variable_info</code>) is visited.
		 */
		public void typeInfo(int pos, byte tag)
		{
		}

		/**
		 * Invoked when an array of <code>verification_type_info</code> is
		 * visited.
		 *
		 * @param num
		 *            the number of elements.
		 * @param isLocals
		 *            true if this array is for <code>locals</code>. false if it
		 *            is for <code>stack</code>.
		 */
		public int typeInfoArray(int pos, int offset, int num, boolean isLocals)
		{
			for (int k = 0; k < num; k++)
				pos = this.typeInfoArray2(k, pos);

			return pos;
		}

		int typeInfoArray2(int k, int pos)
		{
			byte tag = this.info[pos];
			if (tag == StackMap.OBJECT)
			{
				int clazz = ByteArray.readU16bit(this.info, pos + 1);
				this.objectVariable(pos, clazz);
				pos += 3;
			} else if (tag == StackMap.UNINIT)
			{
				int offsetOfNew = ByteArray.readU16bit(this.info, pos + 1);
				this.uninitialized(pos, offsetOfNew);
				pos += 3;
			} else
			{
				this.typeInfo(pos, tag);
				pos++;
			}

			return pos;
		}

		/**
		 * Invoked when an element of type
		 * <code>Uninitialized_variable_info</code> is visited.
		 */
		public void uninitialized(int pos, int offset)
		{
		}

		/**
		 * Visits each entry of the stack map frames.
		 */
		public void visit()
		{
			int num = ByteArray.readU16bit(this.info, 0);
			int pos = 2;
			for (int i = 0; i < num; i++)
			{
				int offset = ByteArray.readU16bit(this.info, pos);
				int numLoc = ByteArray.readU16bit(this.info, pos + 2);
				pos = this.locals(pos + 4, offset, numLoc);
				int numStack = ByteArray.readU16bit(this.info, pos);
				pos = this.stack(pos + 2, offset, numStack);
			}
		}
	}

	/**
	 * Internal use only.
	 */
	public static class Writer
	{
		// see javassist.bytecode.stackmap.MapMaker

		private ByteArrayOutputStream	output;

		/**
		 * Constructs a writer.
		 */
		public Writer()
		{
			this.output = new ByteArrayOutputStream();
		}

		/**
		 * Converts the written data into a byte array.
		 */
		public byte[] toByteArray()
		{
			return this.output.toByteArray();
		}

		/**
		 * Converts to a <code>StackMap</code> attribute.
		 */
		public StackMap toStackMap(ConstPool cp)
		{
			return new StackMap(cp, this.output.toByteArray());
		}

		/**
		 * Writes a 16bit value.
		 */
		public void write16bit(int value)
		{
			this.output.write(value >>> 8 & 0xff);
			this.output.write(value & 0xff);
		}

		/**
		 * Writes a <code>union verification_type_info</code> value.
		 *
		 * @param data
		 *            <code>cpool_index</code> or <code>offset</code>.
		 */
		public void writeVerifyTypeInfo(int tag, int data)
		{
			this.output.write(tag);
			if (tag == StackMap.OBJECT || tag == StackMap.UNINIT)
				this.write16bit(data);
		}
	}

	/**
	 * The name of this attribute <code>"StackMap"</code>.
	 */
	public static final String	tag		= "StackMap";

	/**
	 * <code>Top_variable_info.tag</code>.
	 */
	public static final int		TOP		= 0;

	/**
	 * <code>Integer_variable_info.tag</code>.
	 */
	public static final int		INTEGER	= 1;

	/**
	 * <code>Float_variable_info.tag</code>.
	 */
	public static final int		FLOAT	= 2;

	/**
	 * <code>Double_variable_info.tag</code>.
	 */
	public static final int		DOUBLE	= 3;

	/**
	 * <code>Long_variable_info.tag</code>.
	 */
	public static final int		LONG	= 4;

	/**
	 * <code>Null_variable_info.tag</code>.
	 */
	public static final int		NULL	= 5;

	/**
	 * <code>UninitializedThis_variable_info.tag</code>.
	 */
	public static final int		THIS	= 6;

	/**
	 * <code>Object_variable_info.tag</code>.
	 */
	public static final int		OBJECT	= 7;

	/**
	 * <code>Uninitialized_variable_info.tag</code>.
	 */
	public static final int		UNINIT	= 8;

	/**
	 * Constructs a <code>stack_map</code> attribute.
	 */
	StackMap(ConstPool cp, byte[] newInfo)
	{
		super(cp, StackMap.tag, newInfo);
	}

	StackMap(ConstPool cp, int name_id, DataInputStream in) throws IOException
	{
		super(cp, name_id, in);
	}

	/**
	 * Makes a copy.
	 */
	@Override
	public AttributeInfo copy(ConstPool newCp, Map classnames)
	{
		Copier copier = new Copier(this, newCp, classnames);
		copier.visit();
		return copier.getStackMap();
	}

	/**
	 * Updates this stack map table when a new local variable is inserted for a
	 * new parameter.
	 *
	 * @param index
	 *            the index of the added local variable.
	 * @param tag
	 *            the type tag of that local variable. It is available by
	 *            <code>StackMapTable.typeTagOf(char)</code>.
	 * @param classInfo
	 *            the index of the <code>CONSTANT_Class_info</code> structure in
	 *            a constant pool table. This should be zero unless the tag is
	 *            <code>ITEM_Object</code>.
	 *
	 * @see javassist.CtBehavior#addParameter(javassist.CtClass)
	 * @see StackMapTable#typeTagOf(char)
	 * @see ConstPool
	 */
	public void insertLocal(int index, int tag, int classInfo) throws BadBytecode
	{
		byte[] data = new InsertLocal(this, index, tag, classInfo).doit();
		this.set(data);
	}

	/**
	 * Returns <code>number_of_entries</code>.
	 */
	public int numOfEntries()
	{
		return ByteArray.readU16bit(this.info, 0);
	}

	/**
	 * Prints this stack map.
	 */
	public void print(java.io.PrintWriter out)
	{
		new Printer(this, out).print();
	}

	/**
	 * Undocumented method. Do not use; internal-use only.
	 *
	 * <p>
	 * This method is for javassist.convert.TransformNew. It is called to update
	 * the stack map when the NEW opcode (and the following DUP) is removed.
	 *
	 * @param where
	 *            the position of the removed NEW opcode.
	 */
	public void removeNew(int where) throws CannotCompileException
	{
		byte[] data = new NewRemover(this, where).doit();
		this.set(data);
	}

	/**
	 * @see CodeIterator.Switcher#adjustOffsets(int, int)
	 */
	void shiftForSwitch(int where, int gapSize) throws BadBytecode
	{
		new SwitchShifter(this, where, gapSize).visit();
	}

	void shiftPc(int where, int gapSize, boolean exclusive) throws BadBytecode
	{
		new Shifter(this, where, gapSize, exclusive).visit();
	}
}
