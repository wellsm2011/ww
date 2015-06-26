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
import java.util.List;

/**
 * <code>field_info</code> structure.
 * <p>
 * The following code adds a public field <code>width</code> of <code>int</code>
 * type: <blockquote>
 *
 * <pre>
 * ClassFile cf = ...
 * FieldInfo f = new FieldInfo(cf.getConstPool(), "width", "I");
 * f.setAccessFlags(AccessFlag.PUBLIC);
 * cf.addField(f);
 * </pre>
 *
 * </blockquote>
 *
 * @see javassist.CtField#getFieldInfo()
 */
public final class FieldInfo
{
	ConstPool	constPool;
	int			accessFlags;
	int			name;
	String		cachedName;
	String		cachedType;
	int			descriptor;
	ArrayList	attribute;		// may be null.

	private FieldInfo(ConstPool cp)
	{
		this.constPool = cp;
		this.accessFlags = 0;
		this.attribute = null;
	}

	FieldInfo(ConstPool cp, DataInputStream in) throws IOException
	{
		this(cp);
		this.read(in);
	}

	/**
	 * Constructs a <code>field_info</code> structure.
	 *
	 * @param cp
	 *            a constant pool table
	 * @param fieldName
	 *            field name
	 * @param desc
	 *            field descriptor
	 * @see Descriptor
	 */
	public FieldInfo(ConstPool cp, String fieldName, String desc)
	{
		this(cp);
		this.name = cp.addUtf8Info(fieldName);
		this.cachedName = fieldName;
		this.descriptor = cp.addUtf8Info(desc);
	}

	/**
	 * Appends an attribute. If there is already an attribute with the same
	 * name, the new one substitutes for it.
	 *
	 * @see #getAttributes()
	 */
	public void addAttribute(AttributeInfo info)
	{
		if (this.attribute == null)
			this.attribute = new ArrayList();

		AttributeInfo.remove(this.attribute, info.getName());
		this.attribute.add(info);
	}

	/**
	 * Copies all constant pool items to a given new constant pool and replaces
	 * the original items with the new ones. This is used for garbage collecting
	 * the items of removed fields and methods.
	 *
	 * @param cp
	 *            the destination
	 */
	void compact(ConstPool cp)
	{
		this.name = cp.addUtf8Info(this.getName());
		this.descriptor = cp.addUtf8Info(this.getDescriptor());
		this.attribute = AttributeInfo.copyAll(this.attribute, cp);
		this.constPool = cp;
	}

	/**
	 * Returns the access flags.
	 *
	 * @see AccessFlag
	 */
	public int getAccessFlags()
	{
		return this.accessFlags;
	}

	/**
	 * Returns the attribute with the specified name. It returns null if the
	 * specified attribute is not found.
	 *
	 * @param name
	 *            attribute name
	 * @see #getAttributes()
	 */
	public AttributeInfo getAttribute(String name)
	{
		return AttributeInfo.lookup(this.attribute, name);
	}

	/**
	 * Returns all the attributes. The returned <code>List</code> object is
	 * shared with this object. If you add a new attribute to the list, the
	 * attribute is also added to the field represented by this object. If you
	 * remove an attribute from the list, it is also removed from the field.
	 *
	 * @return a list of <code>AttributeInfo</code> objects.
	 * @see AttributeInfo
	 */
	public List getAttributes()
	{
		if (this.attribute == null)
			this.attribute = new ArrayList();

		return this.attribute;
	}

	/**
	 * Finds a ConstantValue attribute and returns the index into the
	 * <code>constant_pool</code> table.
	 *
	 * @return 0 if a ConstantValue attribute is not found.
	 */
	public int getConstantValue()
	{
		if ((this.accessFlags & AccessFlag.STATIC) == 0)
			return 0;

		ConstantAttribute attr = (ConstantAttribute) this.getAttribute(ConstantAttribute.tag);
		if (attr == null)
			return 0;
		else
			return attr.getConstantValue();
	}

	/**
	 * Returns the constant pool table used by this <code>field_info</code>.
	 */
	public ConstPool getConstPool()
	{
		return this.constPool;
	}

	/**
	 * Returns the field descriptor.
	 *
	 * @see Descriptor
	 */
	public String getDescriptor()
	{
		return this.constPool.getUtf8Info(this.descriptor);
	}

	/**
	 * Returns the field name.
	 */
	public String getName()
	{
		if (this.cachedName == null)
			this.cachedName = this.constPool.getUtf8Info(this.name);

		return this.cachedName;
	}

	void prune(ConstPool cp)
	{
		ArrayList newAttributes = new ArrayList();
		AttributeInfo invisibleAnnotations = this.getAttribute(AnnotationsAttribute.invisibleTag);
		if (invisibleAnnotations != null)
		{
			invisibleAnnotations = invisibleAnnotations.copy(cp, null);
			newAttributes.add(invisibleAnnotations);
		}

		AttributeInfo visibleAnnotations = this.getAttribute(AnnotationsAttribute.visibleTag);
		if (visibleAnnotations != null)
		{
			visibleAnnotations = visibleAnnotations.copy(cp, null);
			newAttributes.add(visibleAnnotations);
		}

		AttributeInfo signature = this.getAttribute(SignatureAttribute.tag);
		if (signature != null)
		{
			signature = signature.copy(cp, null);
			newAttributes.add(signature);
		}

		int index = this.getConstantValue();
		if (index != 0)
		{
			index = this.constPool.copy(index, cp, null);
			newAttributes.add(new ConstantAttribute(cp, index));
		}

		this.attribute = newAttributes;
		this.name = cp.addUtf8Info(this.getName());
		this.descriptor = cp.addUtf8Info(this.getDescriptor());
		this.constPool = cp;
	}

	private void read(DataInputStream in) throws IOException
	{
		this.accessFlags = in.readUnsignedShort();
		this.name = in.readUnsignedShort();
		this.descriptor = in.readUnsignedShort();
		int n = in.readUnsignedShort();
		this.attribute = new ArrayList();
		for (int i = 0; i < n; ++i)
			this.attribute.add(AttributeInfo.read(this.constPool, in));
	}

	/**
	 * Sets the access flags.
	 *
	 * @see AccessFlag
	 */
	public void setAccessFlags(int acc)
	{
		this.accessFlags = acc;
	}

	/**
	 * Sets the field descriptor.
	 *
	 * @see Descriptor
	 */
	public void setDescriptor(String desc)
	{
		if (!desc.equals(this.getDescriptor()))
			this.descriptor = this.constPool.addUtf8Info(desc);
	}

	/**
	 * Sets the field name.
	 */
	public void setName(String newName)
	{
		this.name = this.constPool.addUtf8Info(newName);
		this.cachedName = newName;
	}

	/**
	 * Returns a string representation of the object.
	 */
	@Override
	public String toString()
	{
		return this.getName() + " " + this.getDescriptor();
	}

	void write(DataOutputStream out) throws IOException
	{
		out.writeShort(this.accessFlags);
		out.writeShort(this.name);
		out.writeShort(this.descriptor);
		if (this.attribute == null)
			out.writeShort(0);
		else
		{
			out.writeShort(this.attribute.size());
			AttributeInfo.writeAll(this.attribute, out);
		}
	}
}
