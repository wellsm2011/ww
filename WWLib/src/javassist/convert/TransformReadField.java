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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;

public class TransformReadField extends Transformer
{
	static String isField(ClassPool pool, ConstPool cp, CtClass fclass, String fname, boolean is_private, int index)
	{
		if (!cp.getFieldrefName(index).equals(fname))
			return null;

		try
		{
			CtClass c = pool.get(cp.getFieldrefClassName(index));
			if (c == fclass || !is_private && TransformReadField.isFieldInSuper(c, fclass, fname))
				return cp.getFieldrefType(index);
		} catch (NotFoundException e)
		{
		}
		return null;
	}

	static boolean isFieldInSuper(CtClass clazz, CtClass fclass, String fname)
	{
		if (!clazz.subclassOf(fclass))
			return false;

		try
		{
			CtField f = clazz.getField(fname);
			return f.getDeclaringClass() == fclass;
		} catch (NotFoundException e)
		{
		}
		return false;
	}

	protected String	fieldname;
	protected CtClass	fieldClass;

	protected boolean	isPrivate;

	protected String	methodClassname, methodName;

	public TransformReadField(Transformer next, CtField field, String methodClassname, String methodName)
	{
		super(next);
		this.fieldClass = field.getDeclaringClass();
		this.fieldname = field.getName();
		this.methodClassname = methodClassname;
		this.methodName = methodName;
		this.isPrivate = Modifier.isPrivate(field.getModifiers());
	}

	@Override
	public int transform(CtClass tclazz, int pos, CodeIterator iterator, ConstPool cp) throws BadBytecode
	{
		int c = iterator.byteAt(pos);
		if (c == Opcode.GETFIELD || c == Opcode.GETSTATIC)
		{
			int index = iterator.u16bitAt(pos + 1);
			String typedesc = TransformReadField.isField(tclazz.getClassPool(), cp, this.fieldClass, this.fieldname, this.isPrivate, index);
			if (typedesc != null)
			{
				if (c == Opcode.GETSTATIC)
				{
					iterator.move(pos);
					pos = iterator.insertGap(1); // insertGap() may insert 4
													// bytes.
					iterator.writeByte(Opcode.ACONST_NULL, pos);
					pos = iterator.next();
				}

				String type = "(Ljava/lang/Object;)" + typedesc;
				int mi = cp.addClassInfo(this.methodClassname);
				int methodref = cp.addMethodrefInfo(mi, this.methodName, type);
				iterator.writeByte(Opcode.INVOKESTATIC, pos);
				iterator.write16bit(methodref, pos + 1);
				return pos;
			}
		}

		return pos;
	}
}
