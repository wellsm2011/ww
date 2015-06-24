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

package javassist.convert;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;
import javassist.bytecode.StackMap;
import javassist.bytecode.StackMapTable;

final public class TransformNew extends Transformer
{
	private int	nested;
	private String	classname, trapClass, trapMethod;

	public TransformNew(Transformer next, String classname, String trapClass, String trapMethod)
	{
		super(next);
		this.classname = classname;
		this.trapClass = trapClass;
		this.trapMethod = trapMethod;
	}

	private int computeMethodref(int typedesc, ConstPool cp)
	{
		int classIndex = cp.addClassInfo(this.trapClass);
		int mnameIndex = cp.addUtf8Info(this.trapMethod);
		typedesc = cp.addUtf8Info(Descriptor.changeReturnType(this.classname, cp.getUtf8Info(typedesc)));
		return cp.addMethodrefInfo(classIndex, cp.addNameAndTypeInfo(mnameIndex, typedesc));
	}

	@Override
	public void initialize(ConstPool cp, CodeAttribute attr)
	{
		this.nested = 0;
	}

	/**
	 * Replace a sequence of NEW classname DUP ... INVOKESPECIAL with NOP NOP
	 * ... INVOKESTATIC trapMethod in trapClass
	 */
	@Override
	public int transform(CtClass clazz, int pos, CodeIterator iterator, ConstPool cp) throws CannotCompileException
	{
		int index;
		int c = iterator.byteAt(pos);
		if (c == Opcode.NEW)
		{
			index = iterator.u16bitAt(pos + 1);
			if (cp.getClassInfo(index).equals(this.classname))
			{
				if (iterator.byteAt(pos + 3) != Opcode.DUP)
					throw new CannotCompileException("NEW followed by no DUP was found");

				iterator.writeByte(Opcode.NOP, pos);
				iterator.writeByte(Opcode.NOP, pos + 1);
				iterator.writeByte(Opcode.NOP, pos + 2);
				iterator.writeByte(Opcode.NOP, pos + 3);
				++this.nested;

				StackMapTable smt = (StackMapTable) iterator.get().getAttribute(StackMapTable.tag);
				if (smt != null)
					smt.removeNew(pos);

				StackMap sm = (StackMap) iterator.get().getAttribute(StackMap.tag);
				if (sm != null)
					sm.removeNew(pos);
			}
		} else if (c == Opcode.INVOKESPECIAL)
		{
			index = iterator.u16bitAt(pos + 1);
			int typedesc = cp.isConstructor(this.classname, index);
			if (typedesc != 0 && this.nested > 0)
			{
				int methodref = this.computeMethodref(typedesc, cp);
				iterator.writeByte(Opcode.INVOKESTATIC, pos);
				iterator.write16bit(methodref, pos + 1);
				--this.nested;
			}
		}

		return pos;
	}
}
