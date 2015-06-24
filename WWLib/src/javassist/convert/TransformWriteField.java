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

import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;

final public class TransformWriteField extends TransformReadField
{
	public TransformWriteField(Transformer next, CtField field, String methodClassname, String methodName)
	{
		super(next, field, methodClassname, methodName);
	}

	@Override
	public int transform(CtClass tclazz, int pos, CodeIterator iterator, ConstPool cp) throws BadBytecode
	{
		int c = iterator.byteAt(pos);
		if (c == Opcode.PUTFIELD || c == Opcode.PUTSTATIC)
		{
			int index = iterator.u16bitAt(pos + 1);
			String typedesc = TransformReadField.isField(tclazz.getClassPool(), cp, this.fieldClass, this.fieldname, this.isPrivate, index);
			if (typedesc != null)
			{
				if (c == Opcode.PUTSTATIC)
				{
					CodeAttribute ca = iterator.get();
					iterator.move(pos);
					char c0 = typedesc.charAt(0);
					if (c0 == 'J' || c0 == 'D')
					{ // long or double
						// insertGap() may insert 4 bytes.
						pos = iterator.insertGap(3);
						iterator.writeByte(Opcode.ACONST_NULL, pos);
						iterator.writeByte(Opcode.DUP_X2, pos + 1);
						iterator.writeByte(Opcode.POP, pos + 2);
						ca.setMaxStack(ca.getMaxStack() + 2);
					} else
					{
						// insertGap() may insert 4 bytes.
						pos = iterator.insertGap(2);
						iterator.writeByte(Opcode.ACONST_NULL, pos);
						iterator.writeByte(Opcode.SWAP, pos + 1);
						ca.setMaxStack(ca.getMaxStack() + 1);
					}

					pos = iterator.next();
				}

				int mi = cp.addClassInfo(this.methodClassname);
				String type = "(Ljava/lang/Object;" + typedesc + ")V";
				int methodref = cp.addMethodrefInfo(mi, this.methodName, type);
				iterator.writeByte(Opcode.INVOKESTATIC, pos);
				iterator.write16bit(methodref, pos + 1);
			}
		}

		return pos;
	}
}
