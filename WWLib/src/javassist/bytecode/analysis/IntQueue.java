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

import java.util.NoSuchElementException;

class IntQueue
{
	private static class Entry
	{
		private IntQueue.Entry	next;
		private int				value;

		private Entry(int value)
		{
			this.value = value;
		}
	}

	private IntQueue.Entry	head;

	private IntQueue.Entry	tail;

	void add(int value)
	{
		IntQueue.Entry entry = new Entry(value);
		if (this.tail != null)
			this.tail.next = entry;
		this.tail = entry;

		if (this.head == null)
			this.head = entry;
	}

	boolean isEmpty()
	{
		return this.head == null;
	}

	int take()
	{
		if (this.head == null)
			throw new NoSuchElementException();

		int value = this.head.value;
		this.head = this.head.next;
		if (this.head == null)
			this.tail = null;

		return value;
	}
}
