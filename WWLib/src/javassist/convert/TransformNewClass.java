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
import javassist.bytecode.Opcode;

final public class TransformNewClass extends Transformer
{
	private int	nested;
	private String	classname, newClassName;
	private int		newClassIndex, newMethodNTIndex, newMethodIndex;

	public TransformNewClass(Transformer next, String classname, String newClassName)
	{
		super(next);
		this.classname = classname;
		this.newClassName = newClassName;
	}

	@Override
	public void initialize(ConstPool cp, CodeAttribute attr)
	{
		this.nested = 0;
		this.newClassIndex = this.newMethodNTIndex = this.newMethodIndex = 0;
	}

	/**
	 * Modifies a sequence of NEW classname DUP ... INVOKESPECIAL
	 * classname:method
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

				if (this.newClassIndex == 0)
					this.newClassIndex = cp.addClassInfo(this.newClassName);

				iterator.write16bit(this.newClassIndex, pos + 1);
				++this.nested;
			}
		} else if (c == Opcode.INVOKESPECIAL)
		{
			index = iterator.u16bitAt(pos + 1);
			int typedesc = cp.isConstructor(this.classname, index);
			if (typedesc != 0 && this.nested > 0)
			{
				int nt = cp.getMethodrefNameAndType(index);
				if (this.newMethodNTIndex != nt)
				{
					this.newMethodNTIndex = nt;
					this.newMethodIndex = cp.addMethodrefInfo(this.newClassIndex, nt);
				}

				iterator.write16bit(this.newMethodIndex, pos + 1);
				--this.nested;
			}
		}

		return pos;
	}
}
