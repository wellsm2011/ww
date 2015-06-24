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

import javassist.compiler.CompileError;

/**
 * Thrown when bytecode transformation has failed.
 */
public class CannotCompileException extends Exception
{
	private Throwable	myCause;

	private String		message;

	/**
	 * Constructs a CannotCompileException with a ClassFormatError.
	 */
	public CannotCompileException(ClassFormatError e, String name)
	{
		this("invalid class format: " + name, e);
	}

	/**
	 * Constructs a CannotCompileException with a
	 * <code>ClassNotFoundException</code>.
	 */
	public CannotCompileException(ClassNotFoundException e, String name)
	{
		this("cannot find " + name, e);
	}

	/**
	 * Constructs a CannotCompileException with an <code>CompileError</code>.
	 */
	public CannotCompileException(CompileError e)
	{
		this("[source error] " + e.getMessage(), e);
	}

	/**
	 * Constructs a CannotCompileException with a <code>NotFoundException</code>
	 * .
	 */
	public CannotCompileException(NotFoundException e)
	{
		this("cannot find " + e.getMessage(), e);
	}

	/**
	 * Constructs a CannotCompileException with a message.
	 *
	 * @param msg
	 *            the message.
	 */
	public CannotCompileException(String msg)
	{
		super(msg);
		this.message = msg;
		this.initCause(null);
	}

	/**
	 * Constructs a CannotCompileException with a detailed message and an
	 * <code>Exception</code> representing the cause.
	 *
	 * @param msg
	 *            the message.
	 * @param e
	 *            the cause.
	 */
	public CannotCompileException(String msg, Throwable e)
	{
		this(msg);
		this.initCause(e);
	}

	/**
	 * Constructs a CannotCompileException with an <code>Exception</code>
	 * representing the cause.
	 *
	 * @param e
	 *            the cause.
	 */
	public CannotCompileException(Throwable e)
	{
		super("by " + e.toString());
		this.message = null;
		this.initCause(e);
	}

	/**
	 * Gets the cause of this throwable. It is for JDK 1.3 compatibility.
	 */
	@Override
	public Throwable getCause()
	{
		return this.myCause == this ? null : this.myCause;
	}

	/**
	 * Gets a long message if it is available.
	 */
	public String getReason()
	{
		if (this.message != null)
			return this.message;
		else
			return this.toString();
	}

	/**
	 * Initializes the cause of this throwable. It is for JDK 1.3 compatibility.
	 */
	@Override
	public synchronized Throwable initCause(Throwable cause)
	{
		this.myCause = cause;
		return this;
	}
}
