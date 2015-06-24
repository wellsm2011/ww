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

package javassist.compiler;

import java.util.HashMap;

import javassist.compiler.ast.Declarator;

public final class SymbolTable extends HashMap
{
	private SymbolTable	parent;

	public SymbolTable()
	{
		this(null);
	}

	public SymbolTable(SymbolTable p)
	{
		super();
		this.parent = p;
	}

	public void append(String name, Declarator value)
	{
		this.put(name, value);
	}

	public SymbolTable getParent()
	{
		return this.parent;
	}

	public Declarator lookup(String name)
	{
		Declarator found = (Declarator) this.get(name);
		if (found == null && this.parent != null)
			return this.parent.lookup(name);
		else
			return found;
	}
}
