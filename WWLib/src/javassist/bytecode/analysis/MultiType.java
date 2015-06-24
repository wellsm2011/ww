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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javassist.CtClass;

/**
 * MultiType represents an unresolved type. Whenever two {@code Type} instances
 * are merged, if they share more than one super type (either an interface or a
 * superclass), then a {@code MultiType} is used to represent the possible super
 * types. The goal of a {@code MultiType} is to reduce the set of possible types
 * down to a single resolved type. This is done by eliminating non-assignable
 * types from the typeset when the {@code MultiType} is passed as an argument to
 * {@link Type#isAssignableFrom(Type)}, as well as removing non-intersecting
 * types during a merge.
 *
 * Note: Currently the {@code MultiType} instance is reused as much as possible
 * so that updates are visible from all frames. In addition, all
 * {@code MultiType} merge paths are also updated. This is somewhat hackish, but
 * it appears to handle most scenarios.
 *
 * @author Jason T. Greene
 */

/*
 * TODO - A better, but more involved, approach would be to track the
 * instruction offset that resulted in the creation of this type, and whenever
 * the typeset changes, to force a merge on that position. This would require
 * creating a new MultiType instance every time the typeset changes, and somehow
 * communicating assignment changes to the Analyzer
 */
public class MultiType extends Type
{
	private Map			interfaces;
	private Type		resolved;
	private Type		potentialClass;
	private MultiType	mergeSource;
	private boolean		changed	= false;

	public MultiType(Map interfaces)
	{
		this(interfaces, null);
	}

	public MultiType(Map interfaces, Type potentialClass)
	{
		super(null);
		this.interfaces = interfaces;
		this.potentialClass = potentialClass;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof MultiType))
			return false;

		MultiType multi = (MultiType) o;
		if (this.resolved != null)
			return this.resolved.equals(multi.resolved);
		else if (multi.resolved != null)
			return false;

		return this.interfaces.keySet().equals(multi.interfaces.keySet());
	}

	private Map getAllMultiInterfaces(MultiType type)
	{
		Map map = new HashMap();

		Iterator iter = type.interfaces.values().iterator();
		while (iter.hasNext())
		{
			CtClass intf = (CtClass) iter.next();
			map.put(intf.getName(), intf);
			this.getAllInterfaces(intf, map);
		}

		return map;
	}

	/**
	 * Always returns null since this type is never used for an array.
	 */
	@Override
	public Type getComponent()
	{
		return null;
	}

	/**
	 * Gets the class that corresponds with this type. If this information is
	 * not yet known, java.lang.Object will be returned.
	 */
	@Override
	public CtClass getCtClass()
	{
		if (this.resolved != null)
			return this.resolved.getCtClass();

		return Type.OBJECT.getCtClass();
	}

	/**
	 * Always returns 1, since this type is a reference.
	 */
	@Override
	public int getSize()
	{
		return 1;
	}

	private boolean inMergeSource(MultiType source)
	{
		while (source != null)
		{
			if (source == this)
				return true;

			source = source.mergeSource;
		}

		return false;
	}

	/**
	 * Always reutnrs false since this type is never used for an array
	 */
	@Override
	public boolean isArray()
	{
		return false;
	}

	@Override
	public boolean isAssignableFrom(Type type)
	{
		throw new UnsupportedOperationException("Not implemented");
	}

	public boolean isAssignableTo(Type type)
	{
		if (this.resolved != null)
			return type.isAssignableFrom(this.resolved);

		if (Type.OBJECT.equals(type))
			return true;

		if (this.potentialClass != null && !type.isAssignableFrom(this.potentialClass))
			this.potentialClass = null;

		Map map = this.mergeMultiAndSingle(this, type);

		if (map.size() == 1 && this.potentialClass == null)
		{
			// Update previous merge paths to the same resolved type
			this.resolved = Type.get((CtClass) map.values().iterator().next());
			this.propogateResolved();

			return true;
		}

		// Keep all previous merge paths up to date
		if (map.size() >= 1)
		{
			this.interfaces = map;
			this.propogateState();

			return true;
		}

		if (this.potentialClass != null)
		{
			this.resolved = this.potentialClass;
			this.propogateResolved();

			return true;
		}

		return false;
	}

	/**
	 * Always returns true, since this type is always a reference.
	 *
	 * @return true
	 */
	@Override
	public boolean isReference()
	{
		return true;
	}

	@Override
	public Type merge(Type type)
	{
		if (this == type)
			return this;

		if (type == Type.UNINIT)
			return this;

		if (type == Type.BOGUS)
			return Type.BOGUS;

		if (type == null)
			return this;

		if (this.resolved != null)
			return this.resolved.merge(type);

		if (this.potentialClass != null)
		{
			Type mergePotential = this.potentialClass.merge(type);
			if (!mergePotential.equals(this.potentialClass) || mergePotential.popChanged())
			{
				this.potentialClass = Type.OBJECT.equals(mergePotential) ? null : mergePotential;
				this.changed = true;
			}
		}

		Map merged;

		if (type instanceof MultiType)
		{
			MultiType multi = (MultiType) type;

			if (multi.resolved != null)
				merged = this.mergeMultiAndSingle(this, multi.resolved);
			else
			{
				merged = this.mergeMultiInterfaces(multi, this);
				if (!this.inMergeSource(multi))
					this.mergeSource = multi;
			}
		} else
			merged = this.mergeMultiAndSingle(this, type);

		// Keep all previous merge paths up to date
		if (merged.size() > 1 || merged.size() == 1 && this.potentialClass != null)
		{
			// Check for changes
			if (merged.size() != this.interfaces.size())
				this.changed = true;
			else if (this.changed == false)
			{
				Iterator iter = merged.keySet().iterator();
				while (iter.hasNext())
					if (!this.interfaces.containsKey(iter.next()))
						this.changed = true;
			}

			this.interfaces = merged;
			this.propogateState();

			return this;
		}

		if (merged.size() == 1)
			this.resolved = Type.get((CtClass) merged.values().iterator().next());
		else if (this.potentialClass != null)
			this.resolved = this.potentialClass;
		else
			this.resolved = Type.OBJECT;

		this.propogateResolved();

		return this.resolved;
	}

	private Map mergeMultiAndSingle(MultiType multi, Type single)
	{
		Map map1 = this.getAllMultiInterfaces(multi);
		Map map2 = this.getAllInterfaces(single.getCtClass(), null);

		return this.findCommonInterfaces(map1, map2);
	}

	private Map mergeMultiInterfaces(MultiType type1, MultiType type2)
	{
		Map map1 = this.getAllMultiInterfaces(type1);
		Map map2 = this.getAllMultiInterfaces(type2);

		return this.findCommonInterfaces(map1, map2);
	}

	/**
	 * Returns true if the internal state has changed.
	 */
	@Override
	boolean popChanged()
	{
		boolean changed = this.changed;
		this.changed = false;
		return changed;
	}

	private void propogateResolved()
	{
		MultiType source = this.mergeSource;
		while (source != null)
		{
			source.resolved = this.resolved;
			source = source.mergeSource;
		}
	}

	private void propogateState()
	{
		MultiType source = this.mergeSource;
		while (source != null)
		{
			source.interfaces = this.interfaces;
			source.potentialClass = this.potentialClass;
			source = source.mergeSource;
		}
	}

	@Override
	public String toString()
	{
		if (this.resolved != null)
			return this.resolved.toString();

		StringBuffer buffer = new StringBuffer("{");
		Iterator iter = this.interfaces.keySet().iterator();
		while (iter.hasNext())
		{
			buffer.append(iter.next());
			buffer.append(", ");
		}
		buffer.setLength(buffer.length() - 2);
		if (this.potentialClass != null)
			buffer.append(", *").append(this.potentialClass.toString());
		buffer.append("}");
		return buffer.toString();
	}
}
