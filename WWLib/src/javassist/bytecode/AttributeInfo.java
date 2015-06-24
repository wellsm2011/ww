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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

// Note: if you define a new subclass of AttributeInfo, then
//       update AttributeInfo.read(), .copy(), and (maybe) write().

/**
 * <code>attribute_info</code> structure.
 */
public class AttributeInfo
{
	static ArrayList copyAll(ArrayList list, ConstPool cp)
	{
		if (list == null)
			return null;

		ArrayList newList = new ArrayList();
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			AttributeInfo attr = (AttributeInfo) list.get(i);
			newList.add(attr.copy(cp, null));
		}

		return newList;
	}

	static int getLength(ArrayList list)
	{
		int size = 0;
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			AttributeInfo attr = (AttributeInfo) list.get(i);
			size += attr.length();
		}

		return size;
	}

	static void getRefClasses(List attributes, Map classnames)
	{
		Iterator iterator = attributes.iterator();
		while (iterator.hasNext())
		{
			AttributeInfo ai = (AttributeInfo) iterator.next();
			ai.getRefClasses(classnames);
		}
	}

	static AttributeInfo lookup(ArrayList list, String name)
	{
		if (list == null)
			return null;

		ListIterator iterator = list.listIterator();
		while (iterator.hasNext())
		{
			AttributeInfo ai = (AttributeInfo) iterator.next();
			if (ai.getName().equals(name))
				return ai;
		}

		return null; // no such attribute
	}

	static AttributeInfo read(ConstPool cp, DataInputStream in) throws IOException
	{
		int name = in.readUnsignedShort();
		String nameStr = cp.getUtf8Info(name);
		char first = nameStr.charAt(0);
		if (first < 'M')
		{
			if (first < 'E')
			{
				if (nameStr.equals(AnnotationDefaultAttribute.tag))
					return new AnnotationDefaultAttribute(cp, name, in);
				else if (nameStr.equals(BootstrapMethodsAttribute.tag))
					return new BootstrapMethodsAttribute(cp, name, in);
				else if (nameStr.equals(CodeAttribute.tag))
					return new CodeAttribute(cp, name, in);
				else if (nameStr.equals(ConstantAttribute.tag))
					return new ConstantAttribute(cp, name, in);
				else if (nameStr.equals(DeprecatedAttribute.tag))
					return new DeprecatedAttribute(cp, name, in);
			} else if (nameStr.equals(EnclosingMethodAttribute.tag))
				return new EnclosingMethodAttribute(cp, name, in);
			else if (nameStr.equals(ExceptionsAttribute.tag))
				return new ExceptionsAttribute(cp, name, in);
			else if (nameStr.equals(InnerClassesAttribute.tag))
				return new InnerClassesAttribute(cp, name, in);
			else if (nameStr.equals(LineNumberAttribute.tag))
				return new LineNumberAttribute(cp, name, in);
			else if (nameStr.equals(LocalVariableAttribute.tag))
				return new LocalVariableAttribute(cp, name, in);
			else if (nameStr.equals(LocalVariableTypeAttribute.tag))
				return new LocalVariableTypeAttribute(cp, name, in);
		} else if (first < 'S')
		{
			/*
			 * Note that the names of Annotations attributes begin with 'R'.
			 */
			if (nameStr.equals(MethodParametersAttribute.tag))
				return new MethodParametersAttribute(cp, name, in);
			else if (nameStr.equals(AnnotationsAttribute.visibleTag) || nameStr.equals(AnnotationsAttribute.invisibleTag))
				// RuntimeVisibleAnnotations or RuntimeInvisibleAnnotations
				return new AnnotationsAttribute(cp, name, in);
			else if (nameStr.equals(ParameterAnnotationsAttribute.visibleTag) || nameStr.equals(ParameterAnnotationsAttribute.invisibleTag))
				return new ParameterAnnotationsAttribute(cp, name, in);
			else if (nameStr.equals(TypeAnnotationsAttribute.visibleTag) || nameStr.equals(TypeAnnotationsAttribute.invisibleTag))
				return new TypeAnnotationsAttribute(cp, name, in);
		} else if (nameStr.equals(SignatureAttribute.tag))
			return new SignatureAttribute(cp, name, in);
		else if (nameStr.equals(SourceFileAttribute.tag))
			return new SourceFileAttribute(cp, name, in);
		else if (nameStr.equals(SyntheticAttribute.tag))
			return new SyntheticAttribute(cp, name, in);
		else if (nameStr.equals(StackMap.tag))
			return new StackMap(cp, name, in);
		else if (nameStr.equals(StackMapTable.tag))
			return new StackMapTable(cp, name, in);

		return new AttributeInfo(cp, name, in);
	}

	static synchronized void remove(ArrayList list, String name)
	{
		if (list == null)
			return;

		ListIterator iterator = list.listIterator();
		while (iterator.hasNext())
		{
			AttributeInfo ai = (AttributeInfo) iterator.next();
			if (ai.getName().equals(name))
				iterator.remove();
		}
	}

	static void renameClass(List attributes, Map classnames)
	{
		Iterator iterator = attributes.iterator();
		while (iterator.hasNext())
		{
			AttributeInfo ai = (AttributeInfo) iterator.next();
			ai.renameClass(classnames);
		}
	}

	static void renameClass(List attributes, String oldname, String newname)
	{
		Iterator iterator = attributes.iterator();
		while (iterator.hasNext())
		{
			AttributeInfo ai = (AttributeInfo) iterator.next();
			ai.renameClass(oldname, newname);
		}
	}

	static void writeAll(ArrayList list, DataOutputStream out) throws IOException
	{
		if (list == null)
			return;

		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			AttributeInfo attr = (AttributeInfo) list.get(i);
			attr.write(out);
		}
	}

	protected ConstPool	constPool;

	int					name;

	byte[]				info;

	protected AttributeInfo(ConstPool cp, int attrname, byte[] attrinfo)
	{
		this.constPool = cp;
		this.name = attrname;
		this.info = attrinfo;
	}

	protected AttributeInfo(ConstPool cp, int n, DataInputStream in) throws IOException
	{
		this.constPool = cp;
		this.name = n;
		int len = in.readInt();
		this.info = new byte[len];
		if (len > 0)
			in.readFully(this.info);
	}

	protected AttributeInfo(ConstPool cp, String attrname)
	{
		this(cp, attrname, (byte[]) null);
	}

	/**
	 * Constructs an <code>attribute_info</code> structure.
	 *
	 * @param cp
	 *            constant pool table
	 * @param attrname
	 *            attribute name
	 * @param attrinfo
	 *            <code>info</code> field of <code>attribute_info</code>
	 *            structure.
	 */
	public AttributeInfo(ConstPool cp, String attrname, byte[] attrinfo)
	{
		this(cp, cp.addUtf8Info(attrname), attrinfo);
	}

	/**
	 * Makes a copy. Class names are replaced according to the given
	 * <code>Map</code> object.
	 *
	 * @param newCp
	 *            the constant pool table used by the new copy.
	 * @param classnames
	 *            pairs of replaced and substituted class names.
	 */
	public AttributeInfo copy(ConstPool newCp, Map classnames)
	{
		int s = this.info.length;
		byte[] srcInfo = this.info;
		byte[] newInfo = new byte[s];
		for (int i = 0; i < s; ++i)
			newInfo[i] = srcInfo[i];

		return new AttributeInfo(newCp, this.getName(), newInfo);
	}

	/**
	 * Returns the <code>info</code> field of this <code>attribute_info</code>
	 * structure.
	 *
	 * <p>
	 * This method is not available if the object is an instance of
	 * <code>CodeAttribute</code>.
	 */
	public byte[] get()
	{
		return this.info;
	}

	/**
	 * Returns a constant pool table.
	 */
	public ConstPool getConstPool()
	{
		return this.constPool;
	}

	/**
	 * Returns an attribute name.
	 */
	public String getName()
	{
		return this.constPool.getUtf8Info(this.name);
	}

	void getRefClasses(Map classnames)
	{
	}

	/**
	 * Returns the length of this <code>attribute_info</code> structure. The
	 * returned value is <code>attribute_length + 6</code>.
	 */
	public int length()
	{
		return this.info.length + 6;
	}

	void renameClass(Map classnames)
	{
	}

	/*
	 * The following two methods are used to implement ClassFile.renameClass().
	 * Only CodeAttribute, LocalVariableAttribute, AnnotationsAttribute, and
	 * SignatureAttribute override these methods.
	 */
	void renameClass(String oldname, String newname)
	{
	}

	/**
	 * Sets the <code>info</code> field of this <code>attribute_info</code>
	 * structure.
	 *
	 * <p>
	 * This method is not available if the object is an instance of
	 * <code>CodeAttribute</code>.
	 */
	public void set(byte[] newinfo)
	{
		this.info = newinfo;
	}

	void write(DataOutputStream out) throws IOException
	{
		out.writeShort(this.name);
		out.writeInt(this.info.length);
		if (this.info.length > 0)
			out.write(this.info);
	}
}
