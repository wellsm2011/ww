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
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;

public class TransformBefore extends TransformCall
{
	protected CtClass[]	parameterTypes;
	protected int		locals;
	protected int		maxLocals;
	protected byte[]	saveCode, loadCode;

	public TransformBefore(Transformer next, CtMethod origMethod, CtMethod beforeMethod) throws NotFoundException
	{
		super(next, origMethod, beforeMethod);

		// override
		this.methodDescriptor = origMethod.getMethodInfo2().getDescriptor();

		this.parameterTypes = origMethod.getParameterTypes();
		this.locals = 0;
		this.maxLocals = 0;
		this.saveCode = this.loadCode = null;
	}

	@Override
	public int extraLocals()
	{
		return this.locals;
	}

	@Override
	public void initialize(ConstPool cp, CodeAttribute attr)
	{
		super.initialize(cp, attr);
		this.locals = 0;
		this.maxLocals = attr.getMaxLocals();
		this.saveCode = this.loadCode = null;
	}

	protected void makeCode(CtClass[] paramTypes, ConstPool cp)
	{
		Bytecode save = new Bytecode(cp, 0, 0);
		Bytecode load = new Bytecode(cp, 0, 0);

		int var = this.maxLocals;
		int len = paramTypes == null ? 0 : paramTypes.length;
		load.addAload(var);
		this.makeCode2(save, load, 0, len, paramTypes, var + 1);
		save.addAstore(var);

		this.saveCode = save.get();
		this.loadCode = load.get();
	}

	private void makeCode2(Bytecode save, Bytecode load, int i, int n, CtClass[] paramTypes, int var)
	{
		if (i < n)
		{
			int size = load.addLoad(var, paramTypes[i]);
			this.makeCode2(save, load, i + 1, n, paramTypes, var + size);
			save.addStore(var, paramTypes[i]);
		} else
			this.locals = var - this.maxLocals;
	}

	@Override
	protected int match(int c, int pos, CodeIterator iterator, int typedesc, ConstPool cp) throws BadBytecode
	{
		if (this.newIndex == 0)
		{
			String desc = Descriptor.ofParameters(this.parameterTypes) + 'V';
			desc = Descriptor.insertParameter(this.classname, desc);
			int nt = cp.addNameAndTypeInfo(this.newMethodname, desc);
			int ci = cp.addClassInfo(this.newClassname);
			this.newIndex = cp.addMethodrefInfo(ci, nt);
			this.constPool = cp;
		}

		if (this.saveCode == null)
			this.makeCode(this.parameterTypes, cp);

		return this.match2(pos, iterator);
	}

	protected int match2(int pos, CodeIterator iterator) throws BadBytecode
	{
		iterator.move(pos);
		iterator.insert(this.saveCode);
		iterator.insert(this.loadCode);
		int p = iterator.insertGap(3);
		iterator.writeByte(Opcode.INVOKESTATIC, p);
		iterator.write16bit(this.newIndex, p + 1);
		iterator.insert(this.loadCode);
		return iterator.next();
	}
}
