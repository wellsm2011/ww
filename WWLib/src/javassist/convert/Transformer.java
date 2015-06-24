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
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * Transformer and its subclasses are used for executing code transformation
 * specified by CodeConverter.
 *
 * @see javassist.CodeConverter
 */
public abstract class Transformer implements Opcode
{
	private Transformer	next;

	public Transformer(Transformer t)
	{
		this.next = t;
	}

	public void clean()
	{
	}

	public int extraLocals()
	{
		return 0;
	}

	public int extraStack()
	{
		return 0;
	}

	public Transformer getNext()
	{
		return this.next;
	}

	public void initialize(ConstPool cp, CodeAttribute attr)
	{
	}

	public void initialize(ConstPool cp, CtClass clazz, MethodInfo minfo) throws CannotCompileException
	{
		this.initialize(cp, minfo.getCodeAttribute());
	}

	public abstract int transform(CtClass clazz, int pos, CodeIterator it, ConstPool cp) throws CannotCompileException, BadBytecode;
}
