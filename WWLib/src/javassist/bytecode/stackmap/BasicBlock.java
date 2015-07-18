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

import java.util.ArrayList;
import java.util.HashMap;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * A basic block is a sequence of bytecode that does not contain jump/branch
 * instructions except at the last bytecode. Since Java7 or later does not allow
 * JSR, this class throws an exception when it finds JSR.
 */
public class BasicBlock
{
	public static class Catch
	{
		public Catch		next;
		public BasicBlock	body;
		public int			typeIndex;

		Catch(BasicBlock b, int i, Catch c)
		{
			this.body = b;
			this.typeIndex = i;
			this.next = c;
		}
	}

	static class JsrBytecode extends BadBytecode
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		JsrBytecode()
		{
			super("JSR");
		}
	}

	public static class Maker
	{
		private static BasicBlock getBBlock(Mark m)
		{
			BasicBlock b = m.block;
			if (b != null && m.size > 0)
			{
				b.exit = m.jump;
				b.length = m.size;
				b.stop = m.alwaysJmp;
			}

			return b;
		}

		private void addCatchers(BasicBlock[] blocks, ExceptionTable et) throws BadBytecode
		{
			if (et == null)
				return;

			int i = et.size();
			while (--i >= 0)
			{
				BasicBlock handler = BasicBlock.find(blocks, et.handlerPc(i));
				int start = et.startPc(i);
				int end = et.endPc(i);
				int type = et.catchType(i);
				handler.incoming--;
				for (int k = 0; k < blocks.length; k++)
				{
					BasicBlock bb = blocks[k];
					int iPos = bb.position;
					if (start <= iPos && iPos < end)
					{
						bb.toCatch = new Catch(handler, type, bb.toCatch);
						handler.incoming++;
					}
				}
			}
		}

		public BasicBlock[] make(CodeIterator ci, int begin, int end, ExceptionTable et) throws BadBytecode
		{
			HashMap marks = this.makeMarks(ci, begin, end, et);
			BasicBlock[] bb = this.makeBlocks(marks);
			this.addCatchers(bb, et);
			return bb;
		}

		public BasicBlock[] make(MethodInfo minfo) throws BadBytecode
		{
			CodeAttribute ca = minfo.getCodeAttribute();
			if (ca == null)
				return null;

			CodeIterator ci = ca.iterator();
			return this.make(ci, 0, ci.getCodeLength(), ca.getExceptionTable());
		}

		private BasicBlock[] makeArray(BasicBlock b)
		{
			BasicBlock[] array = this.makeArray(1);
			array[0] = b;
			return array;
		}

		private BasicBlock[] makeArray(BasicBlock b1, BasicBlock b2)
		{
			BasicBlock[] array = this.makeArray(2);
			array[0] = b1;
			array[1] = b2;
			return array;
		}

		protected BasicBlock[] makeArray(int size)
		{
			return new BasicBlock[size];
		}

		/*
		 * Override these two methods if a subclass of BasicBlock must be
		 * instantiated.
		 */
		protected BasicBlock makeBlock(int pos)
		{
			return new BasicBlock(pos);
		}

		private BasicBlock[] makeBlocks(HashMap markTable)
		{
			Mark[] marks = (Mark[]) markTable.values().toArray(new Mark[markTable.size()]);
			java.util.Arrays.sort(marks);
			ArrayList blocks = new ArrayList();
			int i = 0;
			BasicBlock prev;
			if (marks.length > 0 && marks[0].position == 0 && marks[0].block != null)
				prev = Maker.getBBlock(marks[i++]);
			else
				prev = this.makeBlock(0);

			blocks.add(prev);
			while (i < marks.length)
			{
				Mark m = marks[i++];
				BasicBlock bb = Maker.getBBlock(m);
				if (bb == null)
				{
					// the mark indicates a branch instruction
					if (prev.length > 0)
					{
						// the previous mark already has exits.
						prev = this.makeBlock(prev.position + prev.length);
						blocks.add(prev);
					}

					prev.length = m.position + m.size - prev.position;
					prev.exit = m.jump;
					prev.stop = m.alwaysJmp;
				} else
				{
					// the mark indicates a branch target
					if (prev.length == 0)
					{
						prev.length = m.position - prev.position;
						bb.incoming++;
						prev.exit = this.makeArray(bb);
					} else // the previous mark already has exits.
						if (prev.position + prev.length < m.position)
					{
						// dead code is found.
						prev = this.makeBlock(prev.position + prev.length);
						blocks.add(prev);
						prev.length = m.position - prev.position;
						// the incoming flow from dead code is not counted
						// bb.incoming++;
						prev.stop = true; // because the incoming flow is not
						// counted.
						prev.exit = this.makeArray(bb);
					}

					blocks.add(bb);
					prev = bb;
				}
			}

			return (BasicBlock[]) blocks.toArray(this.makeArray(blocks.size()));
		}

		private void makeGoto(HashMap marks, int pos, int target, int size)
		{
			Mark to = this.makeMark(marks, target);
			BasicBlock[] jumps = this.makeArray(to.block);
			this.makeMark(marks, pos, jumps, size, true);
		}

		/*
		 * We could ignore JSR since Java 7 or later does not allow it. See The
		 * JVM Spec. Sec. 4.10.2.5.
		 */
		protected void makeJsr(HashMap marks, int pos, int target, int size) throws BadBytecode
		{
			/*
			 * Mark to = makeMark(marks, target); Mark next = makeMark(marks,
			 * pos + size); BasicBlock[] jumps = makeArray(to.block,
			 * next.block); makeMark(marks, pos, jumps, size, false);
			 */
			throw new JsrBytecode();
		}

		/*
		 * Branch target
		 */
		private Mark makeMark(HashMap table, int pos)
		{
			return this.makeMark0(table, pos, true, true);
		}

		/*
		 * Branch instruction. size > 0
		 */
		private Mark makeMark(HashMap table, int pos, BasicBlock[] jump, int size, boolean always)
		{
			Mark m = this.makeMark0(table, pos, false, false);
			m.setJump(jump, size, always);
			return m;
		}

		private Mark makeMark0(HashMap table, int pos, boolean isBlockBegin, boolean isTarget)
		{
			Integer p = new Integer(pos);
			Mark m = (Mark) table.get(p);
			if (m == null)
			{
				m = new Mark(pos);
				table.put(p, m);
			}

			if (isBlockBegin)
			{
				if (m.block == null)
					m.block = this.makeBlock(pos);

				if (isTarget)
					m.block.incoming++;
			}

			return m;
		}

		private HashMap makeMarks(CodeIterator ci, int begin, int end, ExceptionTable et) throws BadBytecode
		{
			ci.begin();
			ci.move(begin);
			HashMap marks = new HashMap();
			while (ci.hasNext())
			{
				int index = ci.next();
				if (index >= end)
					break;

				int op = ci.byteAt(index);
				if (Opcode.IFEQ <= op && op <= Opcode.IF_ACMPNE || op == Opcode.IFNULL || op == Opcode.IFNONNULL)
				{
					Mark to = this.makeMark(marks, index + ci.s16bitAt(index + 1));
					Mark next = this.makeMark(marks, index + 3);
					this.makeMark(marks, index, this.makeArray(to.block, next.block), 3, false);
				} else if (Opcode.GOTO <= op && op <= Opcode.LOOKUPSWITCH)
					switch (op)
					{
						case Opcode.GOTO:
							this.makeGoto(marks, index, index + ci.s16bitAt(index + 1), 3);
							break;
						case Opcode.JSR:
							this.makeJsr(marks, index, index + ci.s16bitAt(index + 1), 3);
							break;
						case Opcode.RET:
							this.makeMark(marks, index, null, 2, true);
							break;
						case Opcode.TABLESWITCH:
						{
							int pos = (index & ~3) + 4;
							int low = ci.s32bitAt(pos + 4);
							int high = ci.s32bitAt(pos + 8);
							int ncases = high - low + 1;
							BasicBlock[] to = this.makeArray(ncases + 1);
							to[0] = this.makeMark(marks, index + ci.s32bitAt(pos)).block; // default
							// branch
							// target
							int p = pos + 12;
							int n = p + ncases * 4;
							int k = 1;
							while (p < n)
							{
								to[k++] = this.makeMark(marks, index + ci.s32bitAt(p)).block;
								p += 4;
							}
							this.makeMark(marks, index, to, n - index, true);
							break;
						}
						case Opcode.LOOKUPSWITCH:
						{
							int pos = (index & ~3) + 4;
							int ncases = ci.s32bitAt(pos + 4);
							BasicBlock[] to = this.makeArray(ncases + 1);
							to[0] = this.makeMark(marks, index + ci.s32bitAt(pos)).block; // default
							// branch
							// target
							int p = pos + 8 + 4;
							int n = p + ncases * 8 - 4;
							int k = 1;
							while (p < n)
							{
								to[k++] = this.makeMark(marks, index + ci.s32bitAt(p)).block;
								p += 8;
							}
							this.makeMark(marks, index, to, n - index, true);
							break;
						}
					}
				else if (Opcode.IRETURN <= op && op <= Opcode.RETURN || op == Opcode.ATHROW)
					this.makeMark(marks, index, null, 1, true);
				else if (op == Opcode.GOTO_W)
					this.makeGoto(marks, index, index + ci.s32bitAt(index + 1), 5);
				else if (op == Opcode.JSR_W)
					this.makeJsr(marks, index, index + ci.s32bitAt(index + 1), 5);
				else if (op == Opcode.WIDE && ci.byteAt(index + 1) == Opcode.RET)
					this.makeMark(marks, index, null, 4, true);
			}

			if (et != null)
			{
				int i = et.size();
				while (--i >= 0)
				{
					this.makeMark0(marks, et.startPc(i), true, false);
					this.makeMark(marks, et.handlerPc(i));
				}
			}

			return marks;
		}
	}

	/**
	 * A Mark indicates the position of a branch instruction or a branch target.
	 */
	static class Mark implements Comparable
	{
		int				position;
		BasicBlock		block;
		BasicBlock[]	jump;
		boolean			alwaysJmp;			// true if an unconditional branch.
		int				size;						// 0 unless the mark
													// indicates
													// RETURN etc.
		Catch			catcher;

		Mark(int p)
		{
			this.position = p;
			this.block = null;
			this.jump = null;
			this.alwaysJmp = false;
			this.size = 0;
			this.catcher = null;
		}

		@Override
		public int compareTo(Object obj)
		{
			if (obj instanceof Mark)
			{
				int pos = ((Mark) obj).position;
				return this.position - pos;
			}

			return -1;
		}

		void setJump(BasicBlock[] bb, int s, boolean always)
		{
			this.jump = bb;
			this.size = s;
			this.alwaysJmp = always;
		}
	}

	public static BasicBlock find(BasicBlock[] blocks, int pos) throws BadBytecode
	{
		for (int i = 0; i < blocks.length; i++)
		{
			int iPos = blocks[i].position;
			if (iPos <= pos && pos < iPos + blocks[i].length)
				return blocks[i];
		}

		throw new BadBytecode("no basic block at " + pos);
	}

	protected int position, length;

	protected int incoming;			// the number of incoming
	// branches.

	protected BasicBlock[] exit;				// null if the block is a leaf.

	protected boolean stop;				// true if the block ends with
	// an unconditional jump.

	protected Catch toCatch;

	protected BasicBlock(int pos)
	{
		this.position = pos;
		this.length = 0;
		this.incoming = 0;
	}

	@Override
	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		String cname = this.getClass().getName();
		int i = cname.lastIndexOf('.');
		sbuf.append(i < 0 ? cname : cname.substring(i + 1));
		sbuf.append("[");
		this.toString2(sbuf);
		sbuf.append("]");
		return sbuf.toString();
	}

	protected void toString2(StringBuffer sbuf)
	{
		sbuf.append("pos=").append(this.position).append(", len=").append(this.length).append(", in=").append(this.incoming).append(", exit{");
		if (this.exit != null)
			for (int i = 0; i < this.exit.length; i++)
				sbuf.append(this.exit[i].position).append(",");

		sbuf.append("}, {");
		Catch th = this.toCatch;
		while (th != null)
		{
			sbuf.append("(").append(th.body.position).append(", ").append(th.typeIndex).append("), ");
			th = th.next;
		}

		sbuf.append("}");
	}
}
