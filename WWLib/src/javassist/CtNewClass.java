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

package javassist;

import java.io.DataOutputStream;
import java.io.IOException;

import javassist.bytecode.ClassFile;

class CtNewClass extends CtClassType
{
	/*
	 * true if the class is an interface.
	 */
	protected boolean	hasConstructor;

	CtNewClass(String name, ClassPool cp, boolean isInterface, CtClass superclass)
	{
		super(name, cp);
		this.wasChanged = true;
		String superName;
		if (isInterface || superclass == null)
			superName = null;
		else
			superName = superclass.getName();

		this.classfile = new ClassFile(isInterface, name, superName);
		if (isInterface && superclass != null)
			this.classfile.setInterfaces(new String[]
			{ superclass.getName() });

		this.setModifiers(Modifier.setPublic(this.getModifiers()));
		this.hasConstructor = isInterface;
	}

	@Override
	public void addConstructor(CtConstructor c) throws CannotCompileException
	{
		this.hasConstructor = true;
		super.addConstructor(c);
	}

	@Override
	protected void extendToString(StringBuffer buffer)
	{
		if (this.hasConstructor)
			buffer.append("hasConstructor ");

		super.extendToString(buffer);
	}

	/**
	 * Adds constructors inhrited from the super class.
	 *
	 * <p>
	 * After this method is called, the class inherits all the constructors from
	 * the super class. The added constructor calls the super's constructor with
	 * the same signature.
	 */
	public void inheritAllConstructors() throws CannotCompileException, NotFoundException
	{
		CtClass superclazz;
		CtConstructor[] cs;

		superclazz = this.getSuperclass();
		cs = superclazz.getDeclaredConstructors();

		int n = 0;
		for (int i = 0; i < cs.length; ++i)
		{
			CtConstructor c = cs[i];
			int mod = c.getModifiers();
			if (this.isInheritable(mod, superclazz))
			{
				CtConstructor cons = CtNewConstructor.make(c.getParameterTypes(), c.getExceptionTypes(), this);
				cons.setModifiers(mod & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE));
				this.addConstructor(cons);
				++n;
			}
		}

		if (n < 1)
			throw new CannotCompileException("no inheritable constructor in " + superclazz.getName());

	}

	private boolean isInheritable(int mod, CtClass superclazz)
	{
		if (Modifier.isPrivate(mod))
			return false;

		if (Modifier.isPackage(mod))
		{
			String pname = this.getPackageName();
			String pname2 = superclazz.getPackageName();
			if (pname == null)
				return pname2 == null;
			else
				return pname.equals(pname2);
		}

		return true;
	}

	@Override
	public void toBytecode(DataOutputStream out) throws CannotCompileException, IOException
	{
		if (!this.hasConstructor)
			try
			{
				this.inheritAllConstructors();
				this.hasConstructor = true;
			} catch (NotFoundException e)
			{
				throw new CannotCompileException(e);
			}

		super.toBytecode(out);
	}
}
