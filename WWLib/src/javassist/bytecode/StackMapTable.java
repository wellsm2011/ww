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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javassist.CannotCompileException;

/**
 * <code>stack_map</code> attribute.
 * <p>
 * This is an entry in the attributes table of a Code attribute. It was
 * introduced by J2SE 6 for the verification by typechecking.
 *
 * @see StackMap
 * @since 3.4
 */
public class StackMapTable extends AttributeInfo
{
	static class Copier extends SimpleCopy
	{
		private ConstPool	srcPool, destPool;
		private Map			classnames;

		public Copier(ConstPool src, byte[] data, ConstPool dest, Map names)
		{
			super(data);
			this.srcPool = src;
			this.destPool = dest;
			this.classnames = names;
		}

		@Override
		protected int copyData(int tag, int data)
		{
			if (tag == StackMapTable.OBJECT)
				return this.srcPool.copy(data, this.destPool, this.classnames);
			else
				return data;
		}

		@Override
		protected int[] copyData(int[] tags, int[] data)
		{
			int[] newData = new int[data.length];
			for (int i = 0; i < data.length; i++)
				if (tags[i] == StackMapTable.OBJECT)
					newData[i] = this.srcPool.copy(data[i], this.destPool, this.classnames);
				else
					newData[i] = data[i];

			return newData;
		}
	}

	/*
	 * This implementation assumes that a local variable initially holding a
	 * parameter value is never changed to be a different type.
	 */
	static class InsertLocal extends SimpleCopy
	{
		private int	varIndex;
		private int	varTag, varData;

		public InsertLocal(byte[] data, int varIndex, int varTag, int varData)
		{
			super(data);
			this.varIndex = varIndex;
			this.varTag = varTag;
			this.varData = varData;
		}

		@Override
		public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData)
		{
			int len = localTags.length;
			if (len < this.varIndex)
			{
				super.fullFrame(pos, offsetDelta, localTags, localData, stackTags, stackData);
				return;
			}

			int typeSize = this.varTag == StackMapTable.LONG || this.varTag == StackMapTable.DOUBLE ? 2 : 1;
			int[] localTags2 = new int[len + typeSize];
			int[] localData2 = new int[len + typeSize];
			int index = this.varIndex;
			int j = 0;
			for (int i = 0; i < len; i++)
			{
				if (j == index)
					j += typeSize;

				localTags2[j] = localTags[i];
				localData2[j++] = localData[i];
			}

			localTags2[index] = this.varTag;
			localData2[index] = this.varData;
			if (typeSize > 1)
			{
				localTags2[index + 1] = StackMapTable.TOP;
				localData2[index + 1] = 0;
			}

			super.fullFrame(pos, offsetDelta, localTags2, localData2, stackTags, stackData);
		}
	}

	static class NewRemover extends SimpleCopy
	{
		int	posOfNew;

		public NewRemover(byte[] data, int pos)
		{
			super(data);
			this.posOfNew = pos;
		}

		@Override
		public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData)
		{
			int n = stackTags.length - 1;
			for (int i = 0; i < n; i++)
				if (stackTags[i] == StackMapTable.UNINIT && stackData[i] == this.posOfNew && stackTags[i + 1] == StackMapTable.UNINIT && stackData[i + 1] == this.posOfNew)
				{
					n++;
					int[] stackTags2 = new int[n - 2];
					int[] stackData2 = new int[n - 2];
					int k = 0;
					for (int j = 0; j < n; j++)
						if (j == i)
							j++;
						else
						{
							stackTags2[k] = stackTags[j];
							stackData2[k++] = stackData[j];
						}

					stackTags = stackTags2;
					stackData = stackData2;
					break;
				}

			super.fullFrame(pos, offsetDelta, localTags, localData, stackTags, stackData);
		}

		@Override
		public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData)
		{
			if (stackTag == StackMapTable.UNINIT && stackData == this.posOfNew)
				super.sameFrame(pos, offsetDelta);
			else
				super.sameLocals(pos, offsetDelta, stackTag, stackData);
		}
	}

	static class OffsetShifter extends Walker
	{
		int	where, gap;

		public OffsetShifter(StackMapTable smt, int where, int gap)
		{
			super(smt);
			this.where = where;
			this.gap = gap;
		}

		@Override
		public void objectOrUninitialized(int tag, int data, int pos)
		{
			if (tag == StackMapTable.UNINIT)
				if (this.where <= data)
					ByteArray.write16bit(data + this.gap, this.info, pos);
		}
	}

	static class Printer extends Walker
	{
		/**
		 * Prints the stack table map.
		 */
		public static void print(StackMapTable smt, PrintWriter writer)
		{
			try
			{
				new Printer(smt.get(), writer).parse();
			} catch (BadBytecode e)
			{
				writer.println(e.getMessage());
			}
		}

		private PrintWriter	writer;

		private int			offset;

		Printer(byte[] data, PrintWriter pw)
		{
			super(data);
			this.writer = pw;
			this.offset = -1;
		}

		@Override
		public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data)
		{
			this.offset += offsetDelta + 1;
			this.writer.println(this.offset + " append frame: " + offsetDelta);
			for (int i = 0; i < tags.length; i++)
				this.printTypeInfo(tags[i], data[i]);
		}

		@Override
		public void chopFrame(int pos, int offsetDelta, int k)
		{
			this.offset += offsetDelta + 1;
			this.writer.println(this.offset + " chop frame: " + offsetDelta + ",    " + k + " last locals");
		}

		@Override
		public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData)
		{
			this.offset += offsetDelta + 1;
			this.writer.println(this.offset + " full frame: " + offsetDelta);
			this.writer.println("[locals]");
			for (int i = 0; i < localTags.length; i++)
				this.printTypeInfo(localTags[i], localData[i]);

			this.writer.println("[stack]");
			for (int i = 0; i < stackTags.length; i++)
				this.printTypeInfo(stackTags[i], stackData[i]);
		}

		private void printTypeInfo(int tag, int data)
		{
			String msg = null;
			switch (tag)
			{
				case TOP:
					msg = "top";
					break;
				case INTEGER:
					msg = "integer";
					break;
				case FLOAT:
					msg = "float";
					break;
				case DOUBLE:
					msg = "double";
					break;
				case LONG:
					msg = "long";
					break;
				case NULL:
					msg = "null";
					break;
				case THIS:
					msg = "this";
					break;
				case OBJECT:
					msg = "object (cpool_index " + data + ")";
					break;
				case UNINIT:
					msg = "uninitialized (offset " + data + ")";
					break;
			}

			this.writer.print("    ");
			this.writer.println(msg);
		}

		@Override
		public void sameFrame(int pos, int offsetDelta)
		{
			this.offset += offsetDelta + 1;
			this.writer.println(this.offset + " same frame: " + offsetDelta);
		}

		@Override
		public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData)
		{
			this.offset += offsetDelta + 1;
			this.writer.println(this.offset + " same locals: " + offsetDelta);
			this.printTypeInfo(stackTag, stackData);
		}
	}

	/**
	 * An exception that may be thrown by <code>copy()</code> in
	 * <code>StackMapTable</code>.
	 */
	public static class RuntimeCopyException extends RuntimeException
	{
		/**
		 * 
		 */
		private static final long	serialVersionUID	= 1L;

		/**
		 * Constructs an exception.
		 */
		public RuntimeCopyException(String s)
		{
			super(s);
		}
	}

	static class Shifter extends Walker
	{
		static byte[] insertGap(byte[] info, int where, int gap)
		{
			int len = info.length;
			byte[] newinfo = new byte[len + gap];
			for (int i = 0; i < len; i++)
				newinfo[i + (i < where ? 0 : gap)] = info[i];

			return newinfo;
		}

		private StackMapTable	stackMap;
		int						where, gap;
		int						position;
		byte[]					updatedInfo;

		boolean					exclusive;

		public Shifter(StackMapTable smt, int where, int gap, boolean exclusive)
		{
			super(smt);
			this.stackMap = smt;
			this.where = where;
			this.gap = gap;
			this.position = 0;
			this.updatedInfo = null;
			this.exclusive = exclusive;
		}

		@Override
		public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data)
		{
			this.update(pos, offsetDelta);
		}

		@Override
		public void chopFrame(int pos, int offsetDelta, int k)
		{
			this.update(pos, offsetDelta);
		}

		public void doit() throws BadBytecode
		{
			this.parse();
			if (this.updatedInfo != null)
				this.stackMap.set(this.updatedInfo);
		}

		@Override
		public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData)
		{
			this.update(pos, offsetDelta);
		}

		@Override
		public void sameFrame(int pos, int offsetDelta)
		{
			this.update(pos, offsetDelta, 0, 251);
		}

		@Override
		public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData)
		{
			this.update(pos, offsetDelta, 64, 247);
		}

		void update(int pos, int offsetDelta)
		{
			int oldPos = this.position;
			this.position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
			boolean match;
			if (this.exclusive)
				match = oldPos < this.where && this.where <= this.position;
			else
				match = oldPos <= this.where && this.where < this.position;

			if (match)
			{
				int newDelta = offsetDelta + this.gap;
				ByteArray.write16bit(newDelta, this.info, pos + 1);
				this.position += this.gap;
			}
		}

		void update(int pos, int offsetDelta, int base, int entry)
		{
			int oldPos = this.position;
			this.position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
			boolean match;
			if (this.exclusive)
				match = oldPos < this.where && this.where <= this.position;
			else
				match = oldPos <= this.where && this.where < this.position;

			if (match)
			{
				int newDelta = offsetDelta + this.gap;
				this.position += this.gap;
				if (newDelta < 64)
					this.info[pos] = (byte) (newDelta + base);
				else if (offsetDelta < 64)
				{
					byte[] newinfo = Shifter.insertGap(this.info, pos, 2);
					newinfo[pos] = (byte) entry;
					ByteArray.write16bit(newDelta, newinfo, pos + 1);
					this.updatedInfo = newinfo;
				} else
					ByteArray.write16bit(newDelta, this.info, pos + 1);
			}
		}
	}

	static class SimpleCopy extends Walker
	{
		private Writer	writer;

		public SimpleCopy(byte[] data)
		{
			super(data);
			this.writer = new Writer(data.length);
		}

		@Override
		public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data)
		{
			this.writer.appendFrame(offsetDelta, tags, this.copyData(tags, data));
		}

		@Override
		public void chopFrame(int pos, int offsetDelta, int k)
		{
			this.writer.chopFrame(offsetDelta, k);
		}

		protected int copyData(int tag, int data)
		{
			return data;
		}

		protected int[] copyData(int[] tags, int[] data)
		{
			return data;
		}

		public byte[] doit() throws BadBytecode
		{
			this.parse();
			return this.writer.toByteArray();
		}

		@Override
		public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData)
		{
			this.writer.fullFrame(offsetDelta, localTags, this.copyData(localTags, localData), stackTags, this.copyData(stackTags, stackData));
		}

		@Override
		public void sameFrame(int pos, int offsetDelta)
		{
			this.writer.sameFrame(offsetDelta);
		}

		@Override
		public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData)
		{
			this.writer.sameLocals(offsetDelta, stackTag, this.copyData(stackTag, stackData));
		}
	}

	static class SwitchShifter extends Shifter
	{
		static byte[] deleteGap(byte[] info, int where, int gap)
		{
			where += gap;
			int len = info.length;
			byte[] newinfo = new byte[len - gap];
			for (int i = 0; i < len; i++)
				newinfo[i - (i < where ? 0 : gap)] = info[i];

			return newinfo;
		}

		SwitchShifter(StackMapTable smt, int where, int gap)
		{
			super(smt, where, gap, false);
		}

		@Override
		void update(int pos, int offsetDelta)
		{
			int oldPos = this.position;
			this.position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
			int newDelta = offsetDelta;
			if (this.where == this.position)
				newDelta = offsetDelta - this.gap;
			else if (this.where == oldPos)
				newDelta = offsetDelta + this.gap;
			else
				return;

			ByteArray.write16bit(newDelta, this.info, pos + 1);
		}

		@Override
		void update(int pos, int offsetDelta, int base, int entry)
		{
			int oldPos = this.position;
			this.position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
			int newDelta = offsetDelta;
			if (this.where == this.position)
				newDelta = offsetDelta - this.gap;
			else if (this.where == oldPos)
				newDelta = offsetDelta + this.gap;
			else
				return;

			if (offsetDelta < 64)
				if (newDelta < 64)
					this.info[pos] = (byte) (newDelta + base);
				else
				{
					byte[] newinfo = Shifter.insertGap(this.info, pos, 2);
					newinfo[pos] = (byte) entry;
					ByteArray.write16bit(newDelta, newinfo, pos + 1);
					this.updatedInfo = newinfo;
				}
			else if (newDelta < 64)
			{
				byte[] newinfo = SwitchShifter.deleteGap(this.info, pos, 2);
				newinfo[pos] = (byte) (newDelta + base);
				this.updatedInfo = newinfo;
			} else
				ByteArray.write16bit(newDelta, this.info, pos + 1);
		}
	}

	/**
	 * A code walker for a StackMapTable attribute.
	 */
	public static class Walker
	{
		byte[]	info;
		int		numOfEntries;

		/**
		 * Constructs a walker.
		 *
		 * @param data
		 *            the <code>info</code> field of the
		 *            <code>attribute_info</code> structure. It can be obtained
		 *            by <code>get()</code> in the <code>AttributeInfo</code>
		 *            class.
		 */
		public Walker(byte[] data)
		{
			this.info = data;
			this.numOfEntries = ByteArray.readU16bit(data, 0);
		}

		/**
		 * Constructs a walker.
		 *
		 * @param smt
		 *            the StackMapTable that this walker walks around.
		 */
		public Walker(StackMapTable smt)
		{
			this(smt.get());
		}

		private int appendFrame(int pos, int type) throws BadBytecode
		{
			int k = type - 251;
			int offset = ByteArray.readU16bit(this.info, pos + 1);
			int[] tags = new int[k];
			int[] data = new int[k];
			int p = pos + 3;
			for (int i = 0; i < k; i++)
			{
				int tag = this.info[p] & 0xff;
				tags[i] = tag;
				if (tag == StackMapTable.OBJECT || tag == StackMapTable.UNINIT)
				{
					data[i] = ByteArray.readU16bit(this.info, p + 1);
					this.objectOrUninitialized(tag, data[i], p + 1);
					p += 3;
				} else
				{
					data[i] = 0;
					p++;
				}
			}

			this.appendFrame(pos, offset, tags, data);
			return p;
		}

		/**
		 * Invoked if the visited frame is a <code>append_frame</code>.
		 *
		 * @param pos
		 *            the position.
		 * @param offsetDelta
		 * @param tags
		 *            <code>locals[i].tag</code>.
		 * @param data
		 *            <code>locals[i].cpool_index</code> or
		 *            <code>locals[i].offset</code>.
		 */
		public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) throws BadBytecode
		{
		}

		/**
		 * Invoked if the visited frame is a <code>chop_frame</code>.
		 *
		 * @param pos
		 *            the position.
		 * @param offsetDelta
		 * @param k
		 *            the <code>k</code> last locals are absent.
		 */
		public void chopFrame(int pos, int offsetDelta, int k) throws BadBytecode
		{
		}

		private int fullFrame(int pos) throws BadBytecode
		{
			int offset = ByteArray.readU16bit(this.info, pos + 1);
			int numOfLocals = ByteArray.readU16bit(this.info, pos + 3);
			int[] localsTags = new int[numOfLocals];
			int[] localsData = new int[numOfLocals];
			int p = this.verifyTypeInfo(pos + 5, numOfLocals, localsTags, localsData);
			int numOfItems = ByteArray.readU16bit(this.info, p);
			int[] itemsTags = new int[numOfItems];
			int[] itemsData = new int[numOfItems];
			p = this.verifyTypeInfo(p + 2, numOfItems, itemsTags, itemsData);
			this.fullFrame(pos, offset, localsTags, localsData, itemsTags, itemsData);
			return p;
		}

		/**
		 * Invoked if the visited frame is <code>full_frame</code>.
		 *
		 * @param pos
		 *            the position.
		 * @param offsetDelta
		 * @param localTags
		 *            <code>locals[i].tag</code>
		 * @param localData
		 *            <code>locals[i].cpool_index</code> or
		 *            <code>locals[i].offset</code>
		 * @param stackTags
		 *            <code>stack[i].tag</code>
		 * @param stackData
		 *            <code>stack[i].cpool_index</code> or
		 *            <code>stack[i].offset</code>
		 */
		public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData) throws BadBytecode
		{
		}

		/**
		 * Invoked if <code>Object_variable_info</code> or
		 * <code>Uninitialized_variable_info</code> is visited.
		 *
		 * @param tag
		 *            <code>OBJECT</code> or <code>UNINIT</code>.
		 * @param data
		 *            the value of <code>cpool_index</code> or
		 *            <code>offset</code>.
		 * @param pos
		 *            the position of <code>cpool_index</code> or
		 *            <code>offset</code>.
		 */
		public void objectOrUninitialized(int tag, int data, int pos)
		{
		}

		/**
		 * Visits each entry of the stack map frames.
		 */
		public void parse() throws BadBytecode
		{
			int n = this.numOfEntries;
			int pos = 2;
			for (int i = 0; i < n; i++)
				pos = this.stackMapFrames(pos, i);
		}

		/**
		 * Invoked if the visited frame is a <code>same_frame</code> or a
		 * <code>same_frame_extended</code>.
		 *
		 * @param pos
		 *            the position of this frame in the <code>info</code> field
		 *            of <code>attribute_info</code> structure.
		 * @param offsetDelta
		 */
		public void sameFrame(int pos, int offsetDelta) throws BadBytecode
		{
		}

		private int sameLocals(int pos, int type) throws BadBytecode
		{
			int top = pos;
			int offset;
			if (type < 128)
				offset = type - 64;
			else
			{ // type == 247
				offset = ByteArray.readU16bit(this.info, pos + 1);
				pos += 2;
			}

			int tag = this.info[pos + 1] & 0xff;
			int data = 0;
			if (tag == StackMapTable.OBJECT || tag == StackMapTable.UNINIT)
			{
				data = ByteArray.readU16bit(this.info, pos + 2);
				this.objectOrUninitialized(tag, data, pos + 2);
				pos += 2;
			}

			this.sameLocals(top, offset, tag, data);
			return pos + 2;
		}

		/**
		 * Invoked if the visited frame is a
		 * <code>same_locals_1_stack_item_frame</code> or a
		 * <code>same_locals_1_stack_item_frame_extended</code>.
		 *
		 * @param pos
		 *            the position.
		 * @param offsetDelta
		 * @param stackTag
		 *            <code>stack[0].tag</code>.
		 * @param stackData
		 *            <code>stack[0].cpool_index</code> if the tag is
		 *            <code>OBJECT</code>, or <code>stack[0].offset</code> if
		 *            the tag is <code>UNINIT</code>.
		 */
		public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) throws BadBytecode
		{
		}

		/**
		 * Returns the number of the entries.
		 */
		public final int size()
		{
			return this.numOfEntries;
		}

		/**
		 * Invoked when the next entry of the stack map frames is visited.
		 *
		 * @param pos
		 *            the position of the frame in the <code>info</code> field
		 *            of <code>attribute_info</code> structure.
		 * @param nth
		 *            the frame is the N-th (0, 1st, 2nd, 3rd, 4th, ...) entry.
		 * @return the position of the next frame.
		 */
		int stackMapFrames(int pos, int nth) throws BadBytecode
		{
			int type = this.info[pos] & 0xff;
			if (type < 64)
			{
				this.sameFrame(pos, type);
				pos++;
			} else if (type < 128)
				pos = this.sameLocals(pos, type);
			else if (type < 247)
				throw new BadBytecode("bad frame_type in StackMapTable");
			else if (type == 247) // SAME_LOCALS_1_STACK_ITEM_EXTENDED
				pos = this.sameLocals(pos, type);
			else if (type < 251)
			{
				int offset = ByteArray.readU16bit(this.info, pos + 1);
				this.chopFrame(pos, offset, 251 - type);
				pos += 3;
			} else if (type == 251)
			{ // SAME_FRAME_EXTENDED
				int offset = ByteArray.readU16bit(this.info, pos + 1);
				this.sameFrame(pos, offset);
				pos += 3;
			} else if (type < 255)
				pos = this.appendFrame(pos, type);
			else
				// FULL_FRAME
				pos = this.fullFrame(pos);

			return pos;
		}

		private int verifyTypeInfo(int pos, int n, int[] tags, int[] data)
		{
			for (int i = 0; i < n; i++)
			{
				int tag = this.info[pos++] & 0xff;
				tags[i] = tag;
				if (tag == StackMapTable.OBJECT || tag == StackMapTable.UNINIT)
				{
					data[i] = ByteArray.readU16bit(this.info, pos);
					this.objectOrUninitialized(tag, data[i], pos);
					pos += 2;
				}
			}

			return pos;
		}
	}

	/**
	 * A writer of stack map tables.
	 */
	public static class Writer
	{
		ByteArrayOutputStream	output;
		int						numOfEntries;

		/**
		 * Constructs a writer.
		 *
		 * @param size
		 *            the initial buffer size.
		 */
		public Writer(int size)
		{
			this.output = new ByteArrayOutputStream(size);
			this.numOfEntries = 0;
			this.output.write(0); // u2 number_of_entries
			this.output.write(0);
		}

		/**
		 * Writes a <code>append_frame</code>. The number of the appended locals
		 * is specified by the length of <code>tags</code>.
		 *
		 * @param tags
		 *            <code>locals[].tag</code>. The length of this array must
		 *            be either 1, 2, or 3.
		 * @param data
		 *            <code>locals[].cpool_index</code> if the tag is
		 *            <code>OBJECT</code>, or <code>locals[].offset</code> if
		 *            the tag is <code>UNINIT</code>. Otherwise, this parameter
		 *            is not used.
		 */
		public void appendFrame(int offsetDelta, int[] tags, int[] data)
		{
			this.numOfEntries++;
			int k = tags.length; // k is 1, 2, or 3
			this.output.write(k + 251);
			this.write16(offsetDelta);
			for (int i = 0; i < k; i++)
				this.writeTypeInfo(tags[i], data[i]);
		}

		/**
		 * Writes a <code>chop_frame</code>.
		 *
		 * @param k
		 *            the number of absent locals. 1, 2, or 3.
		 */
		public void chopFrame(int offsetDelta, int k)
		{
			this.numOfEntries++;
			this.output.write(251 - k);
			this.write16(offsetDelta);
		}

		/**
		 * Writes a <code>full_frame</code>. <code>number_of_locals</code> and
		 * <code>number_of_stack_items</code> are specified by the the length of
		 * <code>localTags</code> and <code>stackTags</code>.
		 *
		 * @param localTags
		 *            <code>locals[].tag</code>.
		 * @param localData
		 *            <code>locals[].cpool_index</code> if the tag is
		 *            <code>OBJECT</code>, or <code>locals[].offset</code> if
		 *            the tag is <code>UNINIT</code>. Otherwise, this parameter
		 *            is not used.
		 * @param stackTags
		 *            <code>stack[].tag</code>.
		 * @param stackData
		 *            <code>stack[].cpool_index</code> if the tag is
		 *            <code>OBJECT</code>, or <code>stack[].offset</code> if the
		 *            tag is <code>UNINIT</code>. Otherwise, this parameter is
		 *            not used.
		 */
		public void fullFrame(int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData)
		{
			this.numOfEntries++;
			this.output.write(255); // FULL_FRAME
			this.write16(offsetDelta);
			int n = localTags.length;
			this.write16(n);
			for (int i = 0; i < n; i++)
				this.writeTypeInfo(localTags[i], localData[i]);

			n = stackTags.length;
			this.write16(n);
			for (int i = 0; i < n; i++)
				this.writeTypeInfo(stackTags[i], stackData[i]);
		}

		/**
		 * Writes a <code>same_frame</code> or a
		 * <code>same_frame_extended</code>.
		 */
		public void sameFrame(int offsetDelta)
		{
			this.numOfEntries++;
			if (offsetDelta < 64)
				this.output.write(offsetDelta);
			else
			{
				this.output.write(251); // SAME_FRAME_EXTENDED
				this.write16(offsetDelta);
			}
		}

		/**
		 * Writes a <code>same_locals_1_stack_item</code> or a
		 * <code>same_locals_1_stack_item_extended</code>.
		 *
		 * @param tag
		 *            <code>stack[0].tag</code>.
		 * @param data
		 *            <code>stack[0].cpool_index</code> if the tag is
		 *            <code>OBJECT</code>, or <code>stack[0].offset</code> if
		 *            the tag is <code>UNINIT</code>. Otherwise, this parameter
		 *            is not used.
		 */
		public void sameLocals(int offsetDelta, int tag, int data)
		{
			this.numOfEntries++;
			if (offsetDelta < 64)
				this.output.write(offsetDelta + 64);
			else
			{
				this.output.write(247); // SAME_LOCALS_1_STACK_ITEM_EXTENDED
				this.write16(offsetDelta);
			}

			this.writeTypeInfo(tag, data);
		}

		/**
		 * Returns the stack map table written out.
		 */
		public byte[] toByteArray()
		{
			byte[] b = this.output.toByteArray();
			ByteArray.write16bit(this.numOfEntries, b, 0);
			return b;
		}

		/**
		 * Constructs and a return a stack map table containing the written
		 * stack map entries.
		 *
		 * @param cp
		 *            the constant pool used to write the stack map entries.
		 */
		public StackMapTable toStackMapTable(ConstPool cp)
		{
			return new StackMapTable(cp, this.toByteArray());
		}

		private void write16(int value)
		{
			this.output.write(value >>> 8 & 0xff);
			this.output.write(value & 0xff);
		}

		private void writeTypeInfo(int tag, int data)
		{
			this.output.write(tag);
			if (tag == StackMapTable.OBJECT || tag == StackMapTable.UNINIT)
				this.write16(data);
		}
	}

	/**
	 * The name of this attribute <code>"StackMapTable"</code>.
	 */
	public static final String	tag		= "StackMapTable";

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
	 * Returns the tag of the type specified by the descriptor. This method
	 * returns <code>INTEGER</code> unless the descriptor is either D (double),
	 * F (float), J (long), L (class type), or [ (array).
	 *
	 * @param descriptor
	 *            the type descriptor.
	 * @see Descriptor
	 */
	public static int typeTagOf(char descriptor)
	{
		switch (descriptor)
		{
			case 'D':
				return StackMapTable.DOUBLE;
			case 'F':
				return StackMapTable.FLOAT;
			case 'J':
				return StackMapTable.LONG;
			case 'L':
			case '[':
				return StackMapTable.OBJECT;
				// case 'V' :
			default:
				return StackMapTable.INTEGER;
		}
	}

	/**
	 * Constructs a <code>stack_map</code> attribute.
	 */
	StackMapTable(ConstPool cp, byte[] newInfo)
	{
		super(cp, StackMapTable.tag, newInfo);
	}

	StackMapTable(ConstPool cp, int name_id, DataInputStream in) throws IOException
	{
		super(cp, name_id, in);
	}

	/**
	 * Makes a copy.
	 *
	 * @exception RuntimeCopyException
	 *                if a <code>BadBytecode</code> exception is thrown while
	 *                copying, it is converted into
	 *                <code>RuntimeCopyException</code>.
	 */
	@Override
	public AttributeInfo copy(ConstPool newCp, Map classnames) throws RuntimeCopyException
	{
		try
		{
			return new StackMapTable(newCp, new Copier(this.constPool, this.info, newCp, classnames).doit());
		} catch (BadBytecode e)
		{
			throw new RuntimeCopyException("bad bytecode. fatal?");
		}
	}

	/**
	 * Updates this stack map table when a new local variable is inserted for a
	 * new parameter.
	 *
	 * @param index
	 *            the index of the added local variable.
	 * @param tag
	 *            the type tag of that local variable.
	 * @param classInfo
	 *            the index of the <code>CONSTANT_Class_info</code> structure in
	 *            a constant pool table. This should be zero unless the tag is
	 *            <code>ITEM_Object</code>.
	 * @see javassist.CtBehavior#addParameter(javassist.CtClass)
	 * @see #typeTagOf(char)
	 * @see ConstPool
	 */
	public void insertLocal(int index, int tag, int classInfo) throws BadBytecode
	{
		byte[] data = new InsertLocal(this.get(), index, tag, classInfo).doit();
		this.set(data);
	}

	/**
	 * Prints the stack table map.
	 *
	 * @param ps
	 *            a print stream such as <code>System.out</code>.
	 */
	public void println(java.io.PrintStream ps)
	{
		Printer.print(this, new java.io.PrintWriter(ps, true));
	}

	/**
	 * Prints the stack table map.
	 */
	public void println(PrintWriter w)
	{
		Printer.print(this, w);
	}

	/**
	 * Undocumented method. Do not use; internal-use only.
	 * <p>
	 * This method is for javassist.convert.TransformNew. It is called to update
	 * the stack map table when the NEW opcode (and the following DUP) is
	 * removed.
	 *
	 * @param where
	 *            the position of the removed NEW opcode.
	 */
	public void removeNew(int where) throws CannotCompileException
	{
		try
		{
			byte[] data = new NewRemover(this.get(), where).doit();
			this.set(data);
		} catch (BadBytecode e)
		{
			throw new CannotCompileException("bad stack map table", e);
		}
	}

	/**
	 * @see CodeIterator.Switcher#adjustOffsets(int, int)
	 */
	void shiftForSwitch(int where, int gapSize) throws BadBytecode
	{
		new SwitchShifter(this, where, gapSize).doit();
	}

	void shiftPc(int where, int gapSize, boolean exclusive) throws BadBytecode
	{
		new OffsetShifter(this, where, gapSize).parse();
		new Shifter(this, where, gapSize, exclusive).doit();
	}

	@Override
	void write(DataOutputStream out) throws IOException
	{
		super.write(out);
	}
}
