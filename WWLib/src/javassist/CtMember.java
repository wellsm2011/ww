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

/**
 * An instance of <code>CtMember</code> represents a field, a constructor, or a
 * method.
 */
public abstract class CtMember
{
	/*
	 * Make a circular link of CtMembers declared in the same class so that they
	 * are garbage-collected together at the same time.
	 */
	static class Cache extends CtMember
	{
		static int count(CtMember head, CtMember tail)
		{
			int n = 0;
			while (head != tail)
			{
				n++;
				head = head.next;
			}

			return n;
		}

		private CtMember	methodTail;
		private CtMember	consTail;			// constructor tail
		private CtMember	fieldTail;

		Cache(CtClassType decl)
		{
			super(decl);
			this.methodTail = this;
			this.consTail = this;
			this.fieldTail = this;
			this.fieldTail.next = this;
		}

		/*
		 * Both constructors and a class initializer.
		 */
		void addConstructor(CtMember cons)
		{
			cons.next = this.consTail.next;
			this.consTail.next = cons;
			if (this.consTail == this.fieldTail)
				this.fieldTail = cons;

			this.consTail = cons;
		}

		void addField(CtMember field)
		{
			field.next = this; // or fieldTail.next
			this.fieldTail.next = field;
			this.fieldTail = field;
		}

		void addMethod(CtMember method)
		{
			method.next = this.methodTail.next;
			this.methodTail.next = method;
			if (this.methodTail == this.consTail)
			{
				this.consTail = method;
				if (this.methodTail == this.fieldTail)
					this.fieldTail = method;
			}

			this.methodTail = method;
		}

		CtMember consHead()
		{
			return this.methodTail;
		} // may include a static initializer

		@Override
		protected void extendToString(StringBuffer buffer)
		{
		}

		CtMember fieldHead()
		{
			return this.consTail;
		}

		@Override
		public Object getAnnotation(Class clz) throws ClassNotFoundException
		{
			return null;
		}

		@Override
		public Object[] getAnnotations() throws ClassNotFoundException
		{
			return null;
		}

		@Override
		public byte[] getAttribute(String name)
		{
			return null;
		}

		@Override
		public Object[] getAvailableAnnotations()
		{
			return null;
		}

		@Override
		public String getGenericSignature()
		{
			return null;
		}

		@Override
		public int getModifiers()
		{
			return 0;
		}

		@Override
		public String getName()
		{
			return null;
		}

		@Override
		public String getSignature()
		{
			return null;
		}

		@Override
		public boolean hasAnnotation(Class clz)
		{
			return false;
		}

		CtMember lastCons()
		{
			return this.consTail;
		}

		CtMember lastField()
		{
			return this.fieldTail;
		}

		CtMember lastMethod()
		{
			return this.methodTail;
		}

		CtMember methodHead()
		{
			return this;
		}

		void remove(CtMember mem)
		{
			CtMember m = this;
			CtMember node;
			while ((node = m.next) != this)
				if (node == mem)
				{
					m.next = node.next;
					if (node == this.methodTail)
						this.methodTail = m;

					if (node == this.consTail)
						this.consTail = m;

					if (node == this.fieldTail)
						this.fieldTail = m;

					break;
				} else
					m = m.next;
		}

		@Override
		public void setAttribute(String name, byte[] data)
		{
		}

		@Override
		public void setGenericSignature(String sig)
		{
		}

		@Override
		public void setModifiers(int mod)
		{
		}
	}

	CtMember next;			// for internal use

	protected CtClass declaringClass;

	protected CtMember(CtClass clazz)
	{
		this.declaringClass = clazz;
		this.next = null;
	}

	/**
	 * Invoked by {@link #toString()} to add to the buffer and provide the
	 * complete value. Subclasses should invoke this method, adding a space
	 * before each token. The modifiers for the member are provided first;
	 * subclasses should provide additional data such as return type, field or
	 * method name, etc.
	 */
	protected abstract void extendToString(StringBuffer buffer);

	/**
	 * Returns the annotation if the class has the specified annotation class.
	 * For example, if an annotation <code>@Author</code> is associated with
	 * this member, an <code>Author</code> object is returned. The member values
	 * can be obtained by calling methods on the <code>Author</code> object.
	 *
	 * @param clz
	 *            the annotation class.
	 * @return the annotation if found, otherwise <code>null</code>.
	 * @since 3.11
	 */
	public abstract Object getAnnotation(Class clz) throws ClassNotFoundException;

	/**
	 * Returns the annotations associated with this member. For example, if an
	 * annotation <code>@Author</code> is associated with this member, the
	 * returned array contains an <code>Author</code> object. The member values
	 * can be obtained by calling methods on the <code>Author</code> object.
	 *
	 * @return an array of annotation-type objects.
	 * @see CtClass#getAnnotations()
	 */
	public abstract Object[] getAnnotations() throws ClassNotFoundException;

	/**
	 * Obtains a user-defined attribute with the given name. If that attribute
	 * is not found in the class file, this method returns null.
	 * <p>
	 * Note that an attribute is a data block specified by the class file
	 * format. See {@link javassist.bytecode.AttributeInfo}.
	 *
	 * @param name
	 *            attribute name
	 */
	public abstract byte[] getAttribute(String name);

	/**
	 * Returns the annotations associated with this member. This method is
	 * equivalent to <code>getAnnotations()</code> except that, if any
	 * annotations are not on the classpath, they are not included in the
	 * returned array.
	 *
	 * @return an array of annotation-type objects.
	 * @see #getAnnotations()
	 * @see CtClass#getAvailableAnnotations()
	 * @since 3.3
	 */
	public abstract Object[] getAvailableAnnotations();

	/**
	 * Returns the class that declares this member.
	 */
	public CtClass getDeclaringClass()
	{
		return this.declaringClass;
	}

	/**
	 * Returns the generic signature of the member.
	 *
	 * @see javassist.bytecode.SignatureAttribute#toFieldSignature(String)
	 * @see javassist.bytecode.SignatureAttribute#toMethodSignature(String)
	 * @see CtClass#getGenericSignature()
	 * @since 3.17
	 */
	public abstract String getGenericSignature();

	/**
	 * Obtains the modifiers of the member.
	 *
	 * @return modifiers encoded with <code>javassist.Modifier</code>.
	 * @see Modifier
	 */
	public abstract int getModifiers();

	/**
	 * Obtains the name of the member.
	 * <p>
	 * As for constructor names, see <code>getName()</code> in
	 * <code>CtConstructor</code>.
	 *
	 * @see CtConstructor#getName()
	 */
	public abstract String getName();

	/**
	 * Returns the character string representing the signature of the member. If
	 * two members have the same signature (parameter types etc.),
	 * <code>getSignature()</code> returns the same string.
	 */
	public abstract String getSignature();

	/**
	 * Returns true if the class has the specified annotation class.
	 *
	 * @param clz
	 *            the annotation class.
	 * @return <code>true</code> if the annotation is found, otherwise
	 *         <code>false</code>.
	 * @since 3.11
	 */
	public abstract boolean hasAnnotation(Class clz);

	/**
	 * This method is invoked when setName() or replaceClassName() in CtClass is
	 * called.
	 *
	 * @see CtMethod#nameReplaced()
	 */
	void nameReplaced()
	{
	}

	final CtMember next()
	{
		return this.next;
	}

	/**
	 * Adds a user-defined attribute. The attribute is saved in the class file.
	 * <p>
	 * Note that an attribute is a data block specified by the class file
	 * format. See {@link javassist.bytecode.AttributeInfo}.
	 *
	 * @param name
	 *            attribute name
	 * @param data
	 *            attribute value
	 */
	public abstract void setAttribute(String name, byte[] data);

	/**
	 * Sets the generic signature of the member.
	 *
	 * @param sig
	 *            a new generic signature.
	 * @see javassist.bytecode.SignatureAttribute.ObjectType#encode()
	 * @see javassist.bytecode.SignatureAttribute.MethodSignature#encode()
	 * @see CtClass#setGenericSignature(String)
	 * @since 3.17
	 */
	public abstract void setGenericSignature(String sig);

	/**
	 * Sets the encoded modifiers of the member.
	 *
	 * @see Modifier
	 */
	public abstract void setModifiers(int mod);

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer(this.getClass().getName());
		buffer.append("@");
		buffer.append(Integer.toHexString(this.hashCode()));
		buffer.append("[");
		buffer.append(Modifier.toString(this.getModifiers()));
		this.extendToString(buffer);
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * Returns true if this member is accessible from the given class.
	 */
	public boolean visibleFrom(CtClass clazz)
	{
		int mod = this.getModifiers();
		if (Modifier.isPublic(mod))
			return true;
		else if (Modifier.isPrivate(mod))
			return clazz == this.declaringClass;
		else
		{ // package or protected
			String declName = this.declaringClass.getPackageName();
			String fromName = clazz.getPackageName();
			boolean visible;
			if (declName == null)
				visible = fromName == null;
			else
				visible = declName.equals(fromName);

			if (!visible && Modifier.isProtected(mod))
				return clazz.subclassOf(this.declaringClass);

			return visible;
		}
	}
}
