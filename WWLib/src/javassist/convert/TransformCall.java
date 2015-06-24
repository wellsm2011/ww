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
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;

public class TransformCall extends Transformer
{
	protected String	classname, methodname, methodDescriptor;
	protected String	newClassname, newMethodname;
	protected boolean	newMethodIsPrivate;

	/* cache */
	protected int		newIndex;
	protected ConstPool	constPool;

	public TransformCall(Transformer next, CtMethod origMethod, CtMethod substMethod)
	{
		this(next, origMethod.getName(), substMethod);
		this.classname = origMethod.getDeclaringClass().getName();
	}

	public TransformCall(Transformer next, String oldMethodName, CtMethod substMethod)
	{
		super(next);
		this.methodname = oldMethodName;
		this.methodDescriptor = substMethod.getMethodInfo2().getDescriptor();
		this.classname = this.newClassname = substMethod.getDeclaringClass().getName();
		this.newMethodname = substMethod.getName();
		this.constPool = null;
		this.newMethodIsPrivate = Modifier.isPrivate(substMethod.getModifiers());
	}

	@Override
	public void initialize(ConstPool cp, CodeAttribute attr)
	{
		if (this.constPool != cp)
			this.newIndex = 0;
	}

	protected int match(int c, int pos, CodeIterator iterator, int typedesc, ConstPool cp) throws BadBytecode
	{
		if (this.newIndex == 0)
		{
			int nt = cp.addNameAndTypeInfo(cp.addUtf8Info(this.newMethodname), typedesc);
			int ci = cp.addClassInfo(this.newClassname);
			if (c == Opcode.INVOKEINTERFACE)
				this.newIndex = cp.addInterfaceMethodrefInfo(ci, nt);
			else
			{
				if (this.newMethodIsPrivate && c == Opcode.INVOKEVIRTUAL)
					iterator.writeByte(Opcode.INVOKESPECIAL, pos);

				this.newIndex = cp.addMethodrefInfo(ci, nt);
			}

			this.constPool = cp;
		}

		iterator.write16bit(this.newIndex, pos + 1);
		return pos;
	}

	private boolean matchClass(String name, ClassPool pool)
	{
		if (this.classname.equals(name))
			return true;

		try
		{
			CtClass clazz = pool.get(name);
			CtClass declClazz = pool.get(this.classname);
			if (clazz.subtypeOf(declClazz))
				try
				{
					CtMethod m = clazz.getMethod(this.methodname, this.methodDescriptor);
					return m.getDeclaringClass().getName().equals(this.classname);
				} catch (NotFoundException e)
				{
					// maybe the original method has been removed.
					return true;
				}
		} catch (NotFoundException e)
		{
			return false;
		}

		return false;
	}

	/**
	 * Modify INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC and INVOKEVIRTUAL so
	 * that a different method is invoked. The class name in the operand of
	 * these instructions might be a subclass of the target class specified by
	 * <code>classname</code>. This method transforms the instruction in that
	 * case unless the subclass overrides the target method.
	 */
	@Override
	public int transform(CtClass clazz, int pos, CodeIterator iterator, ConstPool cp) throws BadBytecode
	{
		int c = iterator.byteAt(pos);
		if (c == Opcode.INVOKEINTERFACE || c == Opcode.INVOKESPECIAL || c == Opcode.INVOKESTATIC || c == Opcode.INVOKEVIRTUAL)
		{
			int index = iterator.u16bitAt(pos + 1);
			String cname = cp.eqMember(this.methodname, this.methodDescriptor, index);
			if (cname != null && this.matchClass(cname, clazz.getClassPool()))
			{
				int ntinfo = cp.getMemberNameAndType(index);
				pos = this.match(c, pos, iterator, cp.getNameAndTypeDescriptor(ntinfo), cp);
			}
		}

		return pos;
	}
}
