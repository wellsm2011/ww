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

import javassist.CannotCompileException;
import javassist.NotFoundException;

public class CompileError extends Exception
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private Lex					lex;
	private String				reason;

	public CompileError(CannotCompileException e)
	{
		this(e.getReason());
	}

	public CompileError(NotFoundException e)
	{
		this("cannot find " + e.getMessage());
	}

	public CompileError(String s)
	{
		this.reason = s;
		this.lex = null;
	}

	public CompileError(String s, Lex l)
	{
		this.reason = s;
		this.lex = l;
	}

	public Lex getLex()
	{
		return this.lex;
	}

	@Override
	public String getMessage()
	{
		return this.reason;
	}

	@Override
	public String toString()
	{
		return "compile error: " + this.reason;
	}
}
