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
package javassist.bytecode.analysis;

import java.io.PrintStream;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.InstructionPrinter;
import javassist.bytecode.MethodInfo;

/**
 * A utility class for printing a merged view of the frame state and the
 * instructions of a method.
 *
 * @author Jason T. Greene
 */
public final class FramePrinter
{
	/**
	 * Prints all the methods declared in the given class.
	 */
	public static void print(CtClass clazz, PrintStream stream)
	{
		new FramePrinter(stream).print(clazz);
	}

	private final PrintStream	stream;

	/**
	 * Constructs a bytecode printer.
	 */
	public FramePrinter(PrintStream stream)
	{
		this.stream = stream;
	}

	private void addSpacing(int count)
	{
		while (count-- > 0)
			this.stream.print(' ');
	}

	private String getMethodString(CtMethod method)
	{
		try
		{
			return Modifier.toString(method.getModifiers()) + " " + method.getReturnType().getName() + " " + method.getName() + Descriptor.toString(method.getSignature()) + ";";
		} catch (NotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Prints all the methods declared in the given class.
	 */
	public void print(CtClass clazz)
	{
		CtMethod[] methods = clazz.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++)
			this.print(methods[i]);
	}

	/**
	 * Prints the instructions and the frame states of the given method.
	 */
	public void print(CtMethod method)
	{
		this.stream.println("\n" + this.getMethodString(method));
		MethodInfo info = method.getMethodInfo2();
		ConstPool pool = info.getConstPool();
		CodeAttribute code = info.getCodeAttribute();
		if (code == null)
			return;

		Frame[] frames;
		try
		{
			frames = new Analyzer().analyze(method.getDeclaringClass(), info);
		} catch (BadBytecode e)
		{
			throw new RuntimeException(e);
		}

		int spacing = String.valueOf(code.getCodeLength()).length();

		CodeIterator iterator = code.iterator();
		while (iterator.hasNext())
		{
			int pos;
			try
			{
				pos = iterator.next();
			} catch (BadBytecode e)
			{
				throw new RuntimeException(e);
			}

			this.stream.println(pos + ": " + InstructionPrinter.instructionString(iterator, pos, pool));

			this.addSpacing(spacing + 3);
			Frame frame = frames[pos];
			if (frame == null)
			{
				this.stream.println("--DEAD CODE--");
				continue;
			}
			this.printStack(frame);

			this.addSpacing(spacing + 3);
			this.printLocals(frame);
		}

	}

	private void printLocals(Frame frame)
	{
		this.stream.print("locals [");
		int length = frame.localsLength();
		for (int i = 0; i < length; i++)
		{
			if (i > 0)
				this.stream.print(", ");
			Type type = frame.getLocal(i);
			this.stream.print(type == null ? "empty" : type.toString());
		}
		this.stream.println("]");
	}

	private void printStack(Frame frame)
	{
		this.stream.print("stack [");
		int top = frame.getTopIndex();
		for (int i = 0; i <= top; i++)
		{
			if (i > 0)
				this.stream.print(", ");
			Type type = frame.getStack(i);
			this.stream.print(type);
		}
		this.stream.println("]");
	}
}
