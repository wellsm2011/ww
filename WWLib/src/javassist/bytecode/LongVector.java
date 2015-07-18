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

package javassist.bytecode;

final class LongVector
{
	static final int		ASIZE	= 128;
	static final int		ABITS	= 7;			// ASIZE = 2^ABITS
	static final int		VSIZE	= 8;
	private ConstInfo[][]	objects;
	private int				elements;

	public LongVector()
	{
		this.objects = new ConstInfo[LongVector.VSIZE][];
		this.elements = 0;
	}

	public LongVector(int initialSize)
	{
		int vsize = (initialSize >> LongVector.ABITS & ~(LongVector.VSIZE - 1)) + LongVector.VSIZE;
		this.objects = new ConstInfo[vsize][];
		this.elements = 0;
	}

	public void addElement(ConstInfo value)
	{
		int nth = this.elements >> LongVector.ABITS;
		int offset = this.elements & LongVector.ASIZE - 1;
		int len = this.objects.length;
		if (nth >= len)
		{
			ConstInfo[][] newObj = new ConstInfo[len + LongVector.VSIZE][];
			System.arraycopy(this.objects, 0, newObj, 0, len);
			this.objects = newObj;
		}

		if (this.objects[nth] == null)
			this.objects[nth] = new ConstInfo[LongVector.ASIZE];

		this.objects[nth][offset] = value;
		this.elements++;
	}

	public int capacity()
	{
		return this.objects.length * LongVector.ASIZE;
	}

	public ConstInfo elementAt(int i)
	{
		if (i < 0 || this.elements <= i)
			return null;

		return this.objects[i >> LongVector.ABITS][i & LongVector.ASIZE - 1];
	}

	public int size()
	{
		return this.elements;
	}
}
