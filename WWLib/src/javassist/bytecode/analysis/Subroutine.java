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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a nested method subroutine (marked by JSR and RET).
 *
 * @author Jason T. Greene
 */
public class Subroutine
{
	// private Set callers = new HashSet();
	private List	callers	= new ArrayList();
	private Set		access	= new HashSet();
	private int		start;

	public Subroutine(int start, int caller)
	{
		this.start = start;
		this.callers.add(new Integer(caller));
	}

	public void access(int index)
	{
		this.access.add(new Integer(index));
	}

	public Collection accessed()
	{
		return this.access;
	}

	public void addCaller(int caller)
	{
		this.callers.add(new Integer(caller));
	}

	public Collection callers()
	{
		return this.callers;
	}

	public boolean isAccessed(int index)
	{
		return this.access.contains(new Integer(index));
	}

	public int start()
	{
		return this.start;
	}

	@Override
	public String toString()
	{
		return "start = " + this.start + " callers = " + this.callers.toString();
	}
}
