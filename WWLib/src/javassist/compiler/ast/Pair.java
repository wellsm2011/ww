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

package javassist.compiler.ast;

import javassist.compiler.CompileError;

/**
 * A node of a a binary tree. This class provides concrete methods overriding
 * abstract methods in ASTree.
 */
public class Pair extends ASTree
{
	protected ASTree	left, right;

	public Pair(ASTree _left, ASTree _right)
	{
		this.left = _left;
		this.right = _right;
	}

	@Override
	public void accept(Visitor v) throws CompileError
	{
		v.atPair(this);
	}

	@Override
	public ASTree getLeft()
	{
		return this.left;
	}

	@Override
	public ASTree getRight()
	{
		return this.right;
	}

	@Override
	public void setLeft(ASTree _left)
	{
		this.left = _left;
	}

	@Override
	public void setRight(ASTree _right)
	{
		this.right = _right;
	}

	@Override
	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("(<Pair> ");
		sbuf.append(this.left == null ? "<null>" : this.left.toString());
		sbuf.append(" . ");
		sbuf.append(this.right == null ? "<null>" : this.right.toString());
		sbuf.append(')');
		return sbuf.toString();
	}
}
