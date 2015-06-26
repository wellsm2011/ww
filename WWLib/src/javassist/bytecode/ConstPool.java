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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.CtClass;

class ClassInfo extends ConstInfo
{
	static final int	tag	= 7;
	int					name;

	public ClassInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.name = in.readUnsignedShort();
	}

	public ClassInfo(int className, int index)
	{
		super(index);
		this.name = className;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		String classname = src.getUtf8Info(this.name);
		if (map != null)
		{
			String newname = (String) map.get(classname);
			if (newname != null)
				classname = newname;
		}

		return dest.addClassInfo(classname);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof ClassInfo && ((ClassInfo) obj).name == this.name;
	}

	@Override
	public String getClassName(ConstPool cp)
	{
		return cp.getUtf8Info(this.name);
	}

	@Override
	public int getTag()
	{
		return ClassInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return this.name;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("Class #");
		out.println(this.name);
	}

	@Override
	public void renameClass(ConstPool cp, Map map, HashMap cache)
	{
		String oldName = cp.getUtf8Info(this.name);
		String newName = null;
		if (oldName.charAt(0) == '[')
		{
			String s = Descriptor.rename(oldName, map);
			if (oldName != s)
				newName = s;
		} else
		{
			String s = (String) map.get(oldName);
			if (s != null && !s.equals(oldName))
				newName = s;
		}

		if (newName != null)
			if (cache == null)
				this.name = cp.addUtf8Info(newName);
			else
			{
				cache.remove(this);
				this.name = cp.addUtf8Info(newName);
				cache.put(this, this);
			}
	}

	@Override
	public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache)
	{
		String nameStr = cp.getUtf8Info(this.name);
		String newNameStr = null;
		if (nameStr.equals(oldName))
			newNameStr = newName;
		else if (nameStr.charAt(0) == '[')
		{
			String s = Descriptor.rename(nameStr, oldName, newName);
			if (nameStr != s)
				newNameStr = s;
		}

		if (newNameStr != null)
			if (cache == null)
				this.name = cp.addUtf8Info(newNameStr);
			else
			{
				cache.remove(this);
				this.name = cp.addUtf8Info(newNameStr);
				cache.put(this, this);
			}
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(ClassInfo.tag);
		out.writeShort(this.name);
	}
}

abstract class ConstInfo
{
	int	index;

	public ConstInfo(int i)
	{
		this.index = i;
	}

	public abstract int copy(ConstPool src, ConstPool dest, Map classnames);

	// ** classnames is a mapping between JVM names.

	public String getClassName(ConstPool cp)
	{
		return null;
	}

	public abstract int getTag();

	public abstract void print(PrintWriter out);

	public void renameClass(ConstPool cp, Map classnames, HashMap cache)
	{
	}

	public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache)
	{
	}

	@Override
	public String toString()
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(bout);
		this.print(out);
		return bout.toString();
	}

	public abstract void write(DataOutputStream out) throws IOException;
}

/*
 * padding following DoubleInfo or LongInfo.
 */
class ConstInfoPadding extends ConstInfo
{
	public ConstInfoPadding(int i)
	{
		super(i);
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addConstInfoPadding();
	}

	@Override
	public int getTag()
	{
		return 0;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.println("padding");
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
	}
}

/**
 * Constant pool table.
 */
public final class ConstPool
{
	/**
	 * <code>CONSTANT_Class</code>
	 */
	public static final int		CONST_Class					= ClassInfo.tag;
	/**
	 * <code>CONSTANT_Fieldref</code>
	 */
	public static final int		CONST_Fieldref				= FieldrefInfo.tag;
	/**
	 * <code>CONSTANT_Methodref</code>
	 */
	public static final int		CONST_Methodref				= MethodrefInfo.tag;
	/**
	 * <code>CONSTANT_InterfaceMethodref</code>
	 */
	public static final int		CONST_InterfaceMethodref	= InterfaceMethodrefInfo.tag;

	/**
	 * <code>CONSTANT_String</code>
	 */
	public static final int		CONST_String				= StringInfo.tag;

	/**
	 * <code>CONSTANT_Integer</code>
	 */
	public static final int		CONST_Integer				= IntegerInfo.tag;

	/**
	 * <code>CONSTANT_Float</code>
	 */
	public static final int		CONST_Float					= FloatInfo.tag;

	/**
	 * <code>CONSTANT_Long</code>
	 */
	public static final int		CONST_Long					= LongInfo.tag;

	/**
	 * <code>CONSTANT_Double</code>
	 */
	public static final int		CONST_Double				= DoubleInfo.tag;

	/**
	 * <code>CONSTANT_NameAndType</code>
	 */
	public static final int		CONST_NameAndType			= NameAndTypeInfo.tag;

	/**
	 * <code>CONSTANT_Utf8</code>
	 */
	public static final int		CONST_Utf8					= Utf8Info.tag;

	/**
	 * <code>CONSTANT_MethodHandle</code>
	 */
	public static final int		CONST_MethodHandle			= MethodHandleInfo.tag;

	/**
	 * <code>CONSTANT_MethodHandle</code>
	 */
	public static final int		CONST_MethodType			= MethodTypeInfo.tag;

	/**
	 * <code>CONSTANT_MethodHandle</code>
	 */
	public static final int		CONST_InvokeDynamic			= InvokeDynamicInfo.tag;

	/**
	 * Represents the class using this constant pool table.
	 */
	public static final CtClass	THIS						= null;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_getField				= 1;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_getStatic				= 2;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_putField				= 3;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_putStatic				= 4;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_invokeVirtual			= 5;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_invokeStatic			= 6;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_invokeSpecial			= 7;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_newInvokeSpecial		= 8;

	/**
	 * <code>reference_kind</code> of <code>CONSTANT_MethodHandle_info</code>.
	 */
	public static final int		REF_invokeInterface			= 9;

	private static HashMap makeItemsCache(LongVector items)
	{
		HashMap cache = new HashMap();
		int i = 1;
		while (true)
		{
			ConstInfo info = items.elementAt(i++);
			if (info == null)
				break;
			else
				cache.put(info, info);
		}

		return cache;
	}

	LongVector	items;

	int			numOfItems;

	int			thisClassInfo;

	HashMap		itemsCache;

	/**
	 * Constructs a constant pool table from the given byte stream.
	 *
	 * @param in
	 *            byte stream.
	 */
	public ConstPool(DataInputStream in) throws IOException
	{
		this.itemsCache = null;
		this.thisClassInfo = 0;
		/*
		 * read() initializes items and numOfItems, and do addItem(null).
		 */
		this.read(in);
	}

	/**
	 * Constructs a constant pool table.
	 *
	 * @param thisclass
	 *            the name of the class using this constant pool table
	 */
	public ConstPool(String thisclass)
	{
		this.items = new LongVector();
		this.itemsCache = null;
		this.numOfItems = 0;
		this.addItem0(null); // index 0 is reserved by the JVM.
		this.thisClassInfo = this.addClassInfo(thisclass);
	}

	/**
	 * Adds a new <code>CONSTANT_Class_info</code> structure.
	 * <p>
	 * This also adds a <code>CONSTANT_Utf8_info</code> structure for storing
	 * the class name.
	 *
	 * @return the index of the added entry.
	 */
	public int addClassInfo(CtClass c)
	{
		if (c == ConstPool.THIS)
			return this.thisClassInfo;
		else if (!c.isArray())
			return this.addClassInfo(c.getName());
		else
			return this.addClassInfo(Descriptor.toJvmName(c));
	}

	/**
	 * Adds a new <code>CONSTANT_Class_info</code> structure.
	 * <p>
	 * This also adds a <code>CONSTANT_Utf8_info</code> structure for storing
	 * the class name.
	 *
	 * @param qname
	 *            a fully-qualified class name (or the JVM-internal
	 *            representation of that name).
	 * @return the index of the added entry.
	 */
	public int addClassInfo(String qname)
	{
		int utf8 = this.addUtf8Info(Descriptor.toJvmName(qname));
		return this.addItem(new ClassInfo(utf8, this.numOfItems));
	}

	int addConstInfoPadding()
	{
		return this.addItem0(new ConstInfoPadding(this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_Double_info</code> structure.
	 *
	 * @return the index of the added entry.
	 */
	public int addDoubleInfo(double d)
	{
		int i = this.addItem(new DoubleInfo(d, this.numOfItems));
		if (i == this.numOfItems - 1) // if not existing
			this.addConstInfoPadding();

		return i;
	}

	/**
	 * Adds a new <code>CONSTANT_Fieldref_info</code> structure.
	 *
	 * @param classInfo
	 *            <code>class_index</code>
	 * @param nameAndTypeInfo
	 *            <code>name_and_type_index</code>.
	 * @return the index of the added entry.
	 */
	public int addFieldrefInfo(int classInfo, int nameAndTypeInfo)
	{
		return this.addItem(new FieldrefInfo(classInfo, nameAndTypeInfo, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_Fieldref_info</code> structure.
	 * <p>
	 * This also adds a new <code>CONSTANT_NameAndType_info</code> structure.
	 *
	 * @param classInfo
	 *            <code>class_index</code>
	 * @param name
	 *            <code>name_index</code> of
	 *            <code>CONSTANT_NameAndType_info</code>.
	 * @param type
	 *            <code>descriptor_index</code> of
	 *            <code>CONSTANT_NameAndType_info</code>.
	 * @return the index of the added entry.
	 */
	public int addFieldrefInfo(int classInfo, String name, String type)
	{
		int nt = this.addNameAndTypeInfo(name, type);
		return this.addFieldrefInfo(classInfo, nt);
	}

	/**
	 * Adds a new <code>CONSTANT_Float_info</code> structure.
	 *
	 * @return the index of the added entry.
	 */
	public int addFloatInfo(float f)
	{
		return this.addItem(new FloatInfo(f, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_Integer_info</code> structure.
	 *
	 * @return the index of the added entry.
	 */
	public int addIntegerInfo(int i)
	{
		return this.addItem(new IntegerInfo(i, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_InterfaceMethodref_info</code> structure.
	 *
	 * @param classInfo
	 *            <code>class_index</code>
	 * @param nameAndTypeInfo
	 *            <code>name_and_type_index</code>.
	 * @return the index of the added entry.
	 */
	public int addInterfaceMethodrefInfo(int classInfo, int nameAndTypeInfo)
	{
		return this.addItem(new InterfaceMethodrefInfo(classInfo, nameAndTypeInfo, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_InterfaceMethodref_info</code> structure.
	 * <p>
	 * This also adds a new <code>CONSTANT_NameAndType_info</code> structure.
	 *
	 * @param classInfo
	 *            <code>class_index</code>
	 * @param name
	 *            <code>name_index</code> of
	 *            <code>CONSTANT_NameAndType_info</code>.
	 * @param type
	 *            <code>descriptor_index</code> of
	 *            <code>CONSTANT_NameAndType_info</code>.
	 * @return the index of the added entry.
	 */
	public int addInterfaceMethodrefInfo(int classInfo, String name, String type)
	{
		int nt = this.addNameAndTypeInfo(name, type);
		return this.addInterfaceMethodrefInfo(classInfo, nt);
	}

	/**
	 * Adds a new <code>CONSTANT_InvokeDynamic_info</code> structure.
	 *
	 * @param bootstrap
	 *            <code>bootstrap_method_attr_index</code>.
	 * @param nameAndType
	 *            <code>name_and_type_index</code>.
	 * @return the index of the added entry.
	 * @since 3.17
	 */
	public int addInvokeDynamicInfo(int bootstrap, int nameAndType)
	{
		return this.addItem(new InvokeDynamicInfo(bootstrap, nameAndType, this.numOfItems));
	}

	private int addItem(ConstInfo info)
	{
		if (this.itemsCache == null)
			this.itemsCache = ConstPool.makeItemsCache(this.items);

		ConstInfo found = (ConstInfo) this.itemsCache.get(info);
		if (found != null)
			return found.index;
		else
		{
			this.items.addElement(info);
			this.itemsCache.put(info, info);
			return this.numOfItems++;
		}
	}

	private int addItem0(ConstInfo info)
	{
		this.items.addElement(info);
		return this.numOfItems++;
	}

	/**
	 * Adds a new <code>CONSTANT_Long_info</code> structure.
	 *
	 * @return the index of the added entry.
	 */
	public int addLongInfo(long l)
	{
		int i = this.addItem(new LongInfo(l, this.numOfItems));
		if (i == this.numOfItems - 1) // if not existing
			this.addConstInfoPadding();

		return i;
	}

	/**
	 * Adds a new <code>CONSTANT_MethodHandle_info</code> structure.
	 *
	 * @param kind
	 *            <code>reference_kind</code> such as {@link #REF_invokeStatic
	 *            <code>REF_invokeStatic</code>}.
	 * @param index
	 *            <code>reference_index</code>.
	 * @return the index of the added entry.
	 * @since 3.17
	 */
	public int addMethodHandleInfo(int kind, int index)
	{
		return this.addItem(new MethodHandleInfo(kind, index, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_Methodref_info</code> structure.
	 *
	 * @param classInfo
	 *            <code>class_index</code>
	 * @param nameAndTypeInfo
	 *            <code>name_and_type_index</code>.
	 * @return the index of the added entry.
	 */
	public int addMethodrefInfo(int classInfo, int nameAndTypeInfo)
	{
		return this.addItem(new MethodrefInfo(classInfo, nameAndTypeInfo, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_Methodref_info</code> structure.
	 * <p>
	 * This also adds a new <code>CONSTANT_NameAndType_info</code> structure.
	 *
	 * @param classInfo
	 *            <code>class_index</code>
	 * @param name
	 *            <code>name_index</code> of
	 *            <code>CONSTANT_NameAndType_info</code>.
	 * @param type
	 *            <code>descriptor_index</code> of
	 *            <code>CONSTANT_NameAndType_info</code>.
	 * @return the index of the added entry.
	 */
	public int addMethodrefInfo(int classInfo, String name, String type)
	{
		int nt = this.addNameAndTypeInfo(name, type);
		return this.addMethodrefInfo(classInfo, nt);
	}

	/**
	 * Adds a new <code>CONSTANT_MethodType_info</code> structure.
	 *
	 * @param desc
	 *            <code>descriptor_index</code>.
	 * @return the index of the added entry.
	 * @since 3.17
	 */
	public int addMethodTypeInfo(int desc)
	{
		return this.addItem(new MethodTypeInfo(desc, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_NameAndType_info</code> structure.
	 *
	 * @param name
	 *            <code>name_index</code>
	 * @param type
	 *            <code>descriptor_index</code>
	 * @return the index of the added entry.
	 */
	public int addNameAndTypeInfo(int name, int type)
	{
		return this.addItem(new NameAndTypeInfo(name, type, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_NameAndType_info</code> structure.
	 * <p>
	 * This also adds <code>CONSTANT_Utf8_info</code> structures.
	 *
	 * @param name
	 *            <code>name_index</code>
	 * @param type
	 *            <code>descriptor_index</code>
	 * @return the index of the added entry.
	 */
	public int addNameAndTypeInfo(String name, String type)
	{
		return this.addNameAndTypeInfo(this.addUtf8Info(name), this.addUtf8Info(type));
	}

	/**
	 * Adds a new <code>CONSTANT_String_info</code> structure.
	 * <p>
	 * This also adds a new <code>CONSTANT_Utf8_info</code> structure.
	 *
	 * @return the index of the added entry.
	 */
	public int addStringInfo(String str)
	{
		int utf = this.addUtf8Info(str);
		return this.addItem(new StringInfo(utf, this.numOfItems));
	}

	/**
	 * Adds a new <code>CONSTANT_Utf8_info</code> structure.
	 *
	 * @return the index of the added entry.
	 */
	public int addUtf8Info(String utf8)
	{
		return this.addItem(new Utf8Info(utf8, this.numOfItems));
	}

	/**
	 * Copies the n-th item in this ConstPool object into the destination
	 * ConstPool object. The class names that the item refers to are renamed
	 * according to the given map.
	 *
	 * @param n
	 *            the <i>n</i>-th item
	 * @param dest
	 *            destination constant pool table
	 * @param classnames
	 *            the map or null.
	 * @return the index of the copied item into the destination ClassPool.
	 */
	public int copy(int n, ConstPool dest, Map classnames)
	{
		if (n == 0)
			return 0;

		ConstInfo info = this.getItem(n);
		return info.copy(this, dest, classnames);
	}

	/**
	 * Determines whether <code>CONSTANT_Methodref_info</code>,
	 * <code>CONSTANT_Fieldref_info</code>, or
	 * <code>CONSTANT_InterfaceMethodref_info</code> structure at the given
	 * index has the name and the descriptor given as the arguments.
	 *
	 * @param membername
	 *            the member name
	 * @param desc
	 *            the descriptor of the member.
	 * @param index
	 *            the index into the constant pool table
	 * @return the name of the target class specified by the
	 *         <code>..._info</code> structure at <code>index</code>. Otherwise,
	 *         null if that structure does not match the given member name and
	 *         descriptor.
	 */
	public String eqMember(String membername, String desc, int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		NameAndTypeInfo ntinfo = (NameAndTypeInfo) this.getItem(minfo.nameAndTypeIndex);
		if (this.getUtf8Info(ntinfo.memberName).equals(membername) && this.getUtf8Info(ntinfo.typeDescriptor).equals(desc))
			return this.getClassInfo(minfo.classIndex);
		else
			return null; // false
	}

	/**
	 * Reads <code>CONSTANT_Class_info</code> structure at the given index.
	 *
	 * @return a fully-qualified class or interface name specified by
	 *         <code>name_index</code>. If the type is an array type, this
	 *         method returns an encoded name like
	 *         <code>[Ljava.lang.Object;</code> (note that the separators are
	 *         not slashes but dots).
	 * @see javassist.ClassPool#getCtClass(String)
	 */
	public String getClassInfo(int index)
	{
		ClassInfo c = (ClassInfo) this.getItem(index);
		if (c == null)
			return null;
		else
			return Descriptor.toJavaName(this.getUtf8Info(c.name));
	}

	/**
	 * Reads <code>CONSTANT_Class_info</code> structure at the given index.
	 *
	 * @return the descriptor of the type specified by <code>name_index</code>.
	 * @see javassist.ClassPool#getCtClass(String)
	 * @since 3.15
	 */
	public String getClassInfoByDescriptor(int index)
	{
		ClassInfo c = (ClassInfo) this.getItem(index);
		if (c == null)
			return null;
		else
		{
			String className = this.getUtf8Info(c.name);
			if (className.charAt(0) == '[')
				return className;
			else
				return Descriptor.of(className);
		}
	}

	/**
	 * Returns the name of the class using this constant pool table.
	 */
	public String getClassName()
	{
		return this.getClassInfo(this.thisClassInfo);
	}

	/**
	 * Get all the class names.
	 *
	 * @return a set of class names (<code>String</code> objects).
	 */
	public Set getClassNames()
	{
		HashSet result = new HashSet();
		LongVector v = this.items;
		int size = this.numOfItems;
		for (int i = 1; i < size; ++i)
		{
			String className = v.elementAt(i).getClassName(this);
			if (className != null)
				result.add(className);
		}
		return result;
	}

	/**
	 * Reads <code>CONSTANT_Double_info</code> structure at the given index.
	 *
	 * @return the value specified by this entry.
	 */
	public double getDoubleInfo(int index)
	{
		DoubleInfo i = (DoubleInfo) this.getItem(index);
		return i.value;
	}

	/**
	 * Reads the <code>class_index</code> field of the
	 * <code>CONSTANT_Fieldref_info</code> structure at the given index.
	 */
	public int getFieldrefClass(int index)
	{
		FieldrefInfo finfo = (FieldrefInfo) this.getItem(index);
		return finfo.classIndex;
	}

	/**
	 * Reads the <code>class_index</code> field of the
	 * <code>CONSTANT_Fieldref_info</code> structure at the given index.
	 *
	 * @return the name of the class at that <code>class_index</code>.
	 */
	public String getFieldrefClassName(int index)
	{
		FieldrefInfo f = (FieldrefInfo) this.getItem(index);
		if (f == null)
			return null;
		else
			return this.getClassInfo(f.classIndex);
	}

	/**
	 * Reads the <code>name_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure indirectly specified by
	 * the given index.
	 *
	 * @param index
	 *            an index to a <code>CONSTANT_Fieldref_info</code>.
	 * @return the name of the field.
	 */
	public String getFieldrefName(int index)
	{
		FieldrefInfo f = (FieldrefInfo) this.getItem(index);
		if (f == null)
			return null;
		else
		{
			NameAndTypeInfo n = (NameAndTypeInfo) this.getItem(f.nameAndTypeIndex);
			if (n == null)
				return null;
			else
				return this.getUtf8Info(n.memberName);
		}
	}

	/**
	 * Reads the <code>name_and_type_index</code> field of the
	 * <code>CONSTANT_Fieldref_info</code> structure at the given index.
	 */
	public int getFieldrefNameAndType(int index)
	{
		FieldrefInfo finfo = (FieldrefInfo) this.getItem(index);
		return finfo.nameAndTypeIndex;
	}

	/**
	 * Reads the <code>descriptor_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure indirectly specified by
	 * the given index.
	 *
	 * @param index
	 *            an index to a <code>CONSTANT_Fieldref_info</code>.
	 * @return the type descriptor of the field.
	 */
	public String getFieldrefType(int index)
	{
		FieldrefInfo f = (FieldrefInfo) this.getItem(index);
		if (f == null)
			return null;
		else
		{
			NameAndTypeInfo n = (NameAndTypeInfo) this.getItem(f.nameAndTypeIndex);
			if (n == null)
				return null;
			else
				return this.getUtf8Info(n.typeDescriptor);
		}
	}

	/**
	 * Reads <code>CONSTANT_Float_info</code> structure at the given index.
	 *
	 * @return the value specified by this entry.
	 */
	public float getFloatInfo(int index)
	{
		FloatInfo i = (FloatInfo) this.getItem(index);
		return i.value;
	}

	/**
	 * Reads <code>CONSTANT_Integer_info</code> structure at the given index.
	 *
	 * @return the value specified by this entry.
	 */
	public int getIntegerInfo(int index)
	{
		IntegerInfo i = (IntegerInfo) this.getItem(index);
		return i.value;
	}

	/**
	 * Reads the <code>class_index</code> field of the
	 * <code>CONSTANT_InterfaceMethodref_info</code> structure at the given
	 * index.
	 */
	public int getInterfaceMethodrefClass(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		return minfo.classIndex;
	}

	/**
	 * Reads the <code>class_index</code> field of the
	 * <code>CONSTANT_InterfaceMethodref_info</code> structure at the given
	 * index.
	 *
	 * @return the name of the class at that <code>class_index</code>.
	 */
	public String getInterfaceMethodrefClassName(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		return this.getClassInfo(minfo.classIndex);
	}

	/**
	 * Reads the <code>name_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure indirectly specified by
	 * the given index.
	 *
	 * @param index
	 *            an index to a <code>CONSTANT_InterfaceMethodref_info</code>.
	 * @return the name of the method.
	 */
	public String getInterfaceMethodrefName(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		if (minfo == null)
			return null;
		else
		{
			NameAndTypeInfo n = (NameAndTypeInfo) this.getItem(minfo.nameAndTypeIndex);
			if (n == null)
				return null;
			else
				return this.getUtf8Info(n.memberName);
		}
	}

	/**
	 * Reads the <code>name_and_type_index</code> field of the
	 * <code>CONSTANT_InterfaceMethodref_info</code> structure at the given
	 * index.
	 */
	public int getInterfaceMethodrefNameAndType(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		return minfo.nameAndTypeIndex;
	}

	/**
	 * Reads the <code>descriptor_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure indirectly specified by
	 * the given index.
	 *
	 * @param index
	 *            an index to a <code>CONSTANT_InterfaceMethodref_info</code>.
	 * @return the descriptor of the method.
	 */
	public String getInterfaceMethodrefType(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		if (minfo == null)
			return null;
		else
		{
			NameAndTypeInfo n = (NameAndTypeInfo) this.getItem(minfo.nameAndTypeIndex);
			if (n == null)
				return null;
			else
				return this.getUtf8Info(n.typeDescriptor);
		}
	}

	/**
	 * Reads the <code>bootstrap_method_attr_index</code> field of the
	 * <code>CONSTANT_InvokeDynamic_info</code> structure at the given index.
	 *
	 * @since 3.17
	 */
	public int getInvokeDynamicBootstrap(int index)
	{
		InvokeDynamicInfo iv = (InvokeDynamicInfo) this.getItem(index);
		return iv.bootstrap;
	}

	/**
	 * Reads the <code>name_and_type_index</code> field of the
	 * <code>CONSTANT_InvokeDynamic_info</code> structure at the given index.
	 *
	 * @since 3.17
	 */
	public int getInvokeDynamicNameAndType(int index)
	{
		InvokeDynamicInfo iv = (InvokeDynamicInfo) this.getItem(index);
		return iv.nameAndType;
	}

	/**
	 * Reads the <code>descriptor_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure indirectly specified by
	 * the given index.
	 *
	 * @param index
	 *            an index to a <code>CONSTANT_InvokeDynamic_info</code>.
	 * @return the descriptor of the method.
	 * @since 3.17
	 */
	public String getInvokeDynamicType(int index)
	{
		InvokeDynamicInfo iv = (InvokeDynamicInfo) this.getItem(index);
		if (iv == null)
			return null;
		else
		{
			NameAndTypeInfo n = (NameAndTypeInfo) this.getItem(iv.nameAndType);
			if (n == null)
				return null;
			else
				return this.getUtf8Info(n.typeDescriptor);
		}
	}

	ConstInfo getItem(int n)
	{
		return this.items.elementAt(n);
	}

	/**
	 * Reads <code>CONSTANT_Integer_info</code>, <code>_Float_info</code>,
	 * <code>_Long_info</code>, <code>_Double_info</code>, or
	 * <code>_String_info</code> structure. These are used with the LDC
	 * instruction.
	 *
	 * @return a <code>String</code> value or a wrapped primitive-type value.
	 */
	public Object getLdcValue(int index)
	{
		ConstInfo constInfo = this.getItem(index);
		Object value = null;
		if (constInfo instanceof StringInfo)
			value = this.getStringInfo(index);
		else if (constInfo instanceof FloatInfo)
			value = new Float(this.getFloatInfo(index));
		else if (constInfo instanceof IntegerInfo)
			value = new Integer(this.getIntegerInfo(index));
		else if (constInfo instanceof LongInfo)
			value = new Long(this.getLongInfo(index));
		else if (constInfo instanceof DoubleInfo)
			value = new Double(this.getDoubleInfo(index));
		else
			value = null;

		return value;
	}

	/**
	 * Reads <code>CONSTANT_Long_info</code> structure at the given index.
	 *
	 * @return the value specified by this entry.
	 */
	public long getLongInfo(int index)
	{
		LongInfo i = (LongInfo) this.getItem(index);
		return i.value;
	}

	/**
	 * Reads the <code>class_index</code> field of the
	 * <code>CONSTANT_Fieldref_info</code>, <code>CONSTANT_Methodref_info</code>
	 * , or <code>CONSTANT_Interfaceref_info</code>, structure at the given
	 * index.
	 *
	 * @since 3.6
	 */
	public int getMemberClass(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		return minfo.classIndex;
	}

	/**
	 * Reads the <code>name_and_type_index</code> field of the
	 * <code>CONSTANT_Fieldref_info</code>, <code>CONSTANT_Methodref_info</code>
	 * , or <code>CONSTANT_Interfaceref_info</code>, structure at the given
	 * index.
	 *
	 * @since 3.6
	 */
	public int getMemberNameAndType(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		return minfo.nameAndTypeIndex;
	}

	/**
	 * Reads the <code>reference_index</code> field of the
	 * <code>CONSTANT_MethodHandle_info</code> structure at the given index.
	 *
	 * @since 3.17
	 */
	public int getMethodHandleIndex(int index)
	{
		MethodHandleInfo mhinfo = (MethodHandleInfo) this.getItem(index);
		return mhinfo.refIndex;
	}

	/**
	 * Reads the <code>reference_kind</code> field of the
	 * <code>CONSTANT_MethodHandle_info</code> structure at the given index.
	 *
	 * @see #REF_getField
	 * @see #REF_getStatic
	 * @see #REF_invokeInterface
	 * @see #REF_invokeSpecial
	 * @see #REF_invokeStatic
	 * @see #REF_invokeVirtual
	 * @see #REF_newInvokeSpecial
	 * @see #REF_putField
	 * @see #REF_putStatic
	 * @since 3.17
	 */
	public int getMethodHandleKind(int index)
	{
		MethodHandleInfo mhinfo = (MethodHandleInfo) this.getItem(index);
		return mhinfo.refKind;
	}

	/**
	 * Reads the <code>class_index</code> field of the
	 * <code>CONSTANT_Methodref_info</code> structure at the given index.
	 */
	public int getMethodrefClass(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		return minfo.classIndex;
	}

	/**
	 * Reads the <code>class_index</code> field of the
	 * <code>CONSTANT_Methodref_info</code> structure at the given index.
	 *
	 * @return the name of the class at that <code>class_index</code>.
	 */
	public String getMethodrefClassName(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		if (minfo == null)
			return null;
		else
			return this.getClassInfo(minfo.classIndex);
	}

	/**
	 * Reads the <code>name_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure indirectly specified by
	 * the given index.
	 *
	 * @param index
	 *            an index to a <code>CONSTANT_Methodref_info</code>.
	 * @return the name of the method.
	 */
	public String getMethodrefName(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		if (minfo == null)
			return null;
		else
		{
			NameAndTypeInfo n = (NameAndTypeInfo) this.getItem(minfo.nameAndTypeIndex);
			if (n == null)
				return null;
			else
				return this.getUtf8Info(n.memberName);
		}
	}

	/**
	 * Reads the <code>name_and_type_index</code> field of the
	 * <code>CONSTANT_Methodref_info</code> structure at the given index.
	 */
	public int getMethodrefNameAndType(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		return minfo.nameAndTypeIndex;
	}

	/**
	 * Reads the <code>descriptor_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure indirectly specified by
	 * the given index.
	 *
	 * @param index
	 *            an index to a <code>CONSTANT_Methodref_info</code>.
	 * @return the descriptor of the method.
	 */
	public String getMethodrefType(int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		if (minfo == null)
			return null;
		else
		{
			NameAndTypeInfo n = (NameAndTypeInfo) this.getItem(minfo.nameAndTypeIndex);
			if (n == null)
				return null;
			else
				return this.getUtf8Info(n.typeDescriptor);
		}
	}

	/**
	 * Reads the <code>descriptor_index</code> field of the
	 * <code>CONSTANT_MethodType_info</code> structure at the given index.
	 *
	 * @since 3.17
	 */
	public int getMethodTypeInfo(int index)
	{
		MethodTypeInfo mtinfo = (MethodTypeInfo) this.getItem(index);
		return mtinfo.descriptor;
	}

	/**
	 * Reads the <code>descriptor_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure at the given index.
	 */
	public int getNameAndTypeDescriptor(int index)
	{
		NameAndTypeInfo ntinfo = (NameAndTypeInfo) this.getItem(index);
		return ntinfo.typeDescriptor;
	}

	/**
	 * Reads the <code>name_index</code> field of the
	 * <code>CONSTANT_NameAndType_info</code> structure at the given index.
	 */
	public int getNameAndTypeName(int index)
	{
		NameAndTypeInfo ntinfo = (NameAndTypeInfo) this.getItem(index);
		return ntinfo.memberName;
	}

	/**
	 * Returns the number of entries in this table.
	 */
	public int getSize()
	{
		return this.numOfItems;
	}

	/**
	 * Reads <code>CONSTANT_String_info</code> structure at the given index.
	 *
	 * @return the string specified by <code>string_index</code>.
	 */
	public String getStringInfo(int index)
	{
		StringInfo si = (StringInfo) this.getItem(index);
		return this.getUtf8Info(si.string);
	}

	/**
	 * Returns the <code>tag</code> field of the constant pool table entry at
	 * the given index.
	 */
	public int getTag(int index)
	{
		return this.getItem(index).getTag();
	}

	/**
	 * Returns the index of <code>CONSTANT_Class_info</code> structure
	 * specifying the class using this constant pool table.
	 */
	public int getThisClassInfo()
	{
		return this.thisClassInfo;
	}

	/**
	 * Reads <code>CONSTANT_utf8_info</code> structure at the given index.
	 *
	 * @return the string specified by this entry.
	 */
	public String getUtf8Info(int index)
	{
		Utf8Info utf = (Utf8Info) this.getItem(index);
		return utf.string;
	}

	/**
	 * Determines whether <code>CONSTANT_Methodref_info</code> structure at the
	 * given index represents the constructor of the given class.
	 *
	 * @return the <code>descriptor_index</code> specifying the type descriptor
	 *         of the that constructor. If it is not that constructor,
	 *         <code>isConstructor()</code> returns 0.
	 */
	public int isConstructor(String classname, int index)
	{
		return this.isMember(classname, MethodInfo.nameInit, index);
	}

	/**
	 * Determines whether <code>CONSTANT_Methodref_info</code>,
	 * <code>CONSTANT_Fieldref_info</code>, or
	 * <code>CONSTANT_InterfaceMethodref_info</code> structure at the given
	 * index represents the member with the specified name and declaring class.
	 *
	 * @param classname
	 *            the class declaring the member
	 * @param membername
	 *            the member name
	 * @param index
	 *            the index into the constant pool table
	 * @return the <code>descriptor_index</code> specifying the type descriptor
	 *         of that member. If it is not that member, <code>isMember()</code>
	 *         returns 0.
	 */
	public int isMember(String classname, String membername, int index)
	{
		MemberrefInfo minfo = (MemberrefInfo) this.getItem(index);
		if (this.getClassInfo(minfo.classIndex).equals(classname))
		{
			NameAndTypeInfo ntinfo = (NameAndTypeInfo) this.getItem(minfo.nameAndTypeIndex);
			if (this.getUtf8Info(ntinfo.memberName).equals(membername))
				return ntinfo.typeDescriptor;
		}

		return 0; // false
	}

	/**
	 * Prints the contents of the constant pool table.
	 */
	public void print()
	{
		this.print(new PrintWriter(System.out, true));
	}

	/**
	 * Prints the contents of the constant pool table.
	 */
	public void print(PrintWriter out)
	{
		int size = this.numOfItems;
		for (int i = 1; i < size; ++i)
		{
			out.print(i);
			out.print(" ");
			this.items.elementAt(i).print(out);
		}
	}

	void prune()
	{
		this.itemsCache = null;
	}

	private void read(DataInputStream in) throws IOException
	{
		int n = in.readUnsignedShort();

		this.items = new LongVector(n);
		this.numOfItems = 0;
		this.addItem0(null); // index 0 is reserved by the JVM.

		while (--n > 0)
		{ // index 0 is reserved by JVM
			int tag = this.readOne(in);
			if (tag == LongInfo.tag || tag == DoubleInfo.tag)
			{
				this.addConstInfoPadding();
				--n;
			}
		}
	}

	private int readOne(DataInputStream in) throws IOException
	{
		ConstInfo info;
		int tag = in.readUnsignedByte();
		switch (tag)
		{
			case Utf8Info.tag: // 1
				info = new Utf8Info(in, this.numOfItems);
				break;
			case IntegerInfo.tag: // 3
				info = new IntegerInfo(in, this.numOfItems);
				break;
			case FloatInfo.tag: // 4
				info = new FloatInfo(in, this.numOfItems);
				break;
			case LongInfo.tag: // 5
				info = new LongInfo(in, this.numOfItems);
				break;
			case DoubleInfo.tag: // 6
				info = new DoubleInfo(in, this.numOfItems);
				break;
			case ClassInfo.tag: // 7
				info = new ClassInfo(in, this.numOfItems);
				break;
			case StringInfo.tag: // 8
				info = new StringInfo(in, this.numOfItems);
				break;
			case FieldrefInfo.tag: // 9
				info = new FieldrefInfo(in, this.numOfItems);
				break;
			case MethodrefInfo.tag: // 10
				info = new MethodrefInfo(in, this.numOfItems);
				break;
			case InterfaceMethodrefInfo.tag: // 11
				info = new InterfaceMethodrefInfo(in, this.numOfItems);
				break;
			case NameAndTypeInfo.tag: // 12
				info = new NameAndTypeInfo(in, this.numOfItems);
				break;
			case MethodHandleInfo.tag: // 15
				info = new MethodHandleInfo(in, this.numOfItems);
				break;
			case MethodTypeInfo.tag: // 16
				info = new MethodTypeInfo(in, this.numOfItems);
				break;
			case InvokeDynamicInfo.tag: // 18
				info = new InvokeDynamicInfo(in, this.numOfItems);
				break;
			default:
				throw new IOException("invalid constant type: " + tag + " at " + this.numOfItems);
		}

		this.addItem0(info);
		return tag;
	}

	/**
	 * Replaces all occurrences of class names.
	 *
	 * @param classnames
	 *            specifies pairs of replaced and substituted name.
	 */
	public void renameClass(Map classnames)
	{
		LongVector v = this.items;
		int size = this.numOfItems;
		for (int i = 1; i < size; ++i)
		{
			ConstInfo ci = v.elementAt(i);
			ci.renameClass(this, classnames, this.itemsCache);
		}
	}

	/**
	 * Replaces all occurrences of a class name.
	 *
	 * @param oldName
	 *            the replaced name (JVM-internal representation).
	 * @param newName
	 *            the substituted name (JVM-internal representation).
	 */
	public void renameClass(String oldName, String newName)
	{
		LongVector v = this.items;
		int size = this.numOfItems;
		for (int i = 1; i < size; ++i)
		{
			ConstInfo ci = v.elementAt(i);
			ci.renameClass(this, oldName, newName, this.itemsCache);
		}
	}

	void setThisClassInfo(int i)
	{
		this.thisClassInfo = i;
	}

	/**
	 * Writes the contents of the constant pool table.
	 */
	public void write(DataOutputStream out) throws IOException
	{
		out.writeShort(this.numOfItems);
		LongVector v = this.items;
		int size = this.numOfItems;
		for (int i = 1; i < size; ++i)
			v.elementAt(i).write(out);
	}
}

class DoubleInfo extends ConstInfo
{
	static final int	tag	= 6;
	double				value;

	public DoubleInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.value = in.readDouble();
	}

	public DoubleInfo(double d, int index)
	{
		super(index);
		this.value = d;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addDoubleInfo(this.value);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof DoubleInfo && ((DoubleInfo) obj).value == this.value;
	}

	@Override
	public int getTag()
	{
		return DoubleInfo.tag;
	}

	@Override
	public int hashCode()
	{
		long v = Double.doubleToLongBits(this.value);
		return (int) (v ^ v >>> 32);
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("Double ");
		out.println(this.value);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(DoubleInfo.tag);
		out.writeDouble(this.value);
	}
}

class FieldrefInfo extends MemberrefInfo
{
	static final int	tag	= 9;

	public FieldrefInfo(DataInputStream in, int thisIndex) throws IOException
	{
		super(in, thisIndex);
	}

	public FieldrefInfo(int cindex, int ntindex, int thisIndex)
	{
		super(cindex, ntindex, thisIndex);
	}

	@Override
	protected int copy2(ConstPool dest, int cindex, int ntindex)
	{
		return dest.addFieldrefInfo(cindex, ntindex);
	}

	@Override
	public int getTag()
	{
		return FieldrefInfo.tag;
	}

	@Override
	public String getTagName()
	{
		return "Field";
	}
}

class FloatInfo extends ConstInfo
{
	static final int	tag	= 4;
	float				value;

	public FloatInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.value = in.readFloat();
	}

	public FloatInfo(float f, int index)
	{
		super(index);
		this.value = f;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addFloatInfo(this.value);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof FloatInfo && ((FloatInfo) obj).value == this.value;
	}

	@Override
	public int getTag()
	{
		return FloatInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return Float.floatToIntBits(this.value);
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("Float ");
		out.println(this.value);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(FloatInfo.tag);
		out.writeFloat(this.value);
	}
}

class IntegerInfo extends ConstInfo
{
	static final int	tag	= 3;
	int					value;

	public IntegerInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.value = in.readInt();
	}

	public IntegerInfo(int v, int index)
	{
		super(index);
		this.value = v;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addIntegerInfo(this.value);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof IntegerInfo && ((IntegerInfo) obj).value == this.value;
	}

	@Override
	public int getTag()
	{
		return IntegerInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return this.value;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("Integer ");
		out.println(this.value);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(IntegerInfo.tag);
		out.writeInt(this.value);
	}
}

class InterfaceMethodrefInfo extends MemberrefInfo
{
	static final int	tag	= 11;

	public InterfaceMethodrefInfo(DataInputStream in, int thisIndex) throws IOException
	{
		super(in, thisIndex);
	}

	public InterfaceMethodrefInfo(int cindex, int ntindex, int thisIndex)
	{
		super(cindex, ntindex, thisIndex);
	}

	@Override
	protected int copy2(ConstPool dest, int cindex, int ntindex)
	{
		return dest.addInterfaceMethodrefInfo(cindex, ntindex);
	}

	@Override
	public int getTag()
	{
		return InterfaceMethodrefInfo.tag;
	}

	@Override
	public String getTagName()
	{
		return "Interface";
	}
}

class InvokeDynamicInfo extends ConstInfo
{
	static final int	tag	= 18;
	int					bootstrap, nameAndType;

	public InvokeDynamicInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.bootstrap = in.readUnsignedShort();
		this.nameAndType = in.readUnsignedShort();
	}

	public InvokeDynamicInfo(int bootstrapMethod, int ntIndex, int index)
	{
		super(index);
		this.bootstrap = bootstrapMethod;
		this.nameAndType = ntIndex;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addInvokeDynamicInfo(this.bootstrap, src.getItem(this.nameAndType).copy(src, dest, map));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof InvokeDynamicInfo)
		{
			InvokeDynamicInfo iv = (InvokeDynamicInfo) obj;
			return iv.bootstrap == this.bootstrap && iv.nameAndType == this.nameAndType;
		} else
			return false;
	}

	@Override
	public int getTag()
	{
		return InvokeDynamicInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return this.bootstrap << 16 ^ this.nameAndType;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("InvokeDynamic #");
		out.print(this.bootstrap);
		out.print(", name&type #");
		out.println(this.nameAndType);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(InvokeDynamicInfo.tag);
		out.writeShort(this.bootstrap);
		out.writeShort(this.nameAndType);
	}
}

class LongInfo extends ConstInfo
{
	static final int	tag	= 5;
	long				value;

	public LongInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.value = in.readLong();
	}

	public LongInfo(long l, int index)
	{
		super(index);
		this.value = l;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addLongInfo(this.value);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof LongInfo && ((LongInfo) obj).value == this.value;
	}

	@Override
	public int getTag()
	{
		return LongInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return (int) (this.value ^ this.value >>> 32);
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("Long ");
		out.println(this.value);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(LongInfo.tag);
		out.writeLong(this.value);
	}
}

abstract class MemberrefInfo extends ConstInfo
{
	int	classIndex;
	int	nameAndTypeIndex;

	public MemberrefInfo(DataInputStream in, int thisIndex) throws IOException
	{
		super(thisIndex);
		this.classIndex = in.readUnsignedShort();
		this.nameAndTypeIndex = in.readUnsignedShort();
	}

	public MemberrefInfo(int cindex, int ntindex, int thisIndex)
	{
		super(thisIndex);
		this.classIndex = cindex;
		this.nameAndTypeIndex = ntindex;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		int classIndex2 = src.getItem(this.classIndex).copy(src, dest, map);
		int ntIndex2 = src.getItem(this.nameAndTypeIndex).copy(src, dest, map);
		return this.copy2(dest, classIndex2, ntIndex2);
	}

	abstract protected int copy2(ConstPool dest, int cindex, int ntindex);

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof MemberrefInfo)
		{
			MemberrefInfo mri = (MemberrefInfo) obj;
			return mri.classIndex == this.classIndex && mri.nameAndTypeIndex == this.nameAndTypeIndex && mri.getClass() == this.getClass();
		} else
			return false;
	}

	public abstract String getTagName();

	@Override
	public int hashCode()
	{
		return this.classIndex << 16 ^ this.nameAndTypeIndex;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print(this.getTagName() + " #");
		out.print(this.classIndex);
		out.print(", name&type #");
		out.println(this.nameAndTypeIndex);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(this.getTag());
		out.writeShort(this.classIndex);
		out.writeShort(this.nameAndTypeIndex);
	}
}

class MethodHandleInfo extends ConstInfo
{
	static final int	tag	= 15;
	int					refKind, refIndex;

	public MethodHandleInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.refKind = in.readUnsignedByte();
		this.refIndex = in.readUnsignedShort();
	}

	public MethodHandleInfo(int kind, int referenceIndex, int index)
	{
		super(index);
		this.refKind = kind;
		this.refIndex = referenceIndex;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addMethodHandleInfo(this.refKind, src.getItem(this.refIndex).copy(src, dest, map));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof MethodHandleInfo)
		{
			MethodHandleInfo mh = (MethodHandleInfo) obj;
			return mh.refKind == this.refKind && mh.refIndex == this.refIndex;
		} else
			return false;
	}

	@Override
	public int getTag()
	{
		return MethodHandleInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return this.refKind << 16 ^ this.refIndex;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("MethodHandle #");
		out.print(this.refKind);
		out.print(", index #");
		out.println(this.refIndex);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(MethodHandleInfo.tag);
		out.writeByte(this.refKind);
		out.writeShort(this.refIndex);
	}
}

class MethodrefInfo extends MemberrefInfo
{
	static final int	tag	= 10;

	public MethodrefInfo(DataInputStream in, int thisIndex) throws IOException
	{
		super(in, thisIndex);
	}

	public MethodrefInfo(int cindex, int ntindex, int thisIndex)
	{
		super(cindex, ntindex, thisIndex);
	}

	@Override
	protected int copy2(ConstPool dest, int cindex, int ntindex)
	{
		return dest.addMethodrefInfo(cindex, ntindex);
	}

	@Override
	public int getTag()
	{
		return MethodrefInfo.tag;
	}

	@Override
	public String getTagName()
	{
		return "Method";
	}
}

class MethodTypeInfo extends ConstInfo
{
	static final int	tag	= 16;
	int					descriptor;

	public MethodTypeInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.descriptor = in.readUnsignedShort();
	}

	public MethodTypeInfo(int desc, int index)
	{
		super(index);
		this.descriptor = desc;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		String desc = src.getUtf8Info(this.descriptor);
		desc = Descriptor.rename(desc, map);
		return dest.addMethodTypeInfo(dest.addUtf8Info(desc));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof MethodTypeInfo)
			return ((MethodTypeInfo) obj).descriptor == this.descriptor;
		else
			return false;
	}

	@Override
	public int getTag()
	{
		return MethodTypeInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return this.descriptor;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("MethodType #");
		out.println(this.descriptor);
	}

	@Override
	public void renameClass(ConstPool cp, Map map, HashMap cache)
	{
		String desc = cp.getUtf8Info(this.descriptor);
		String desc2 = Descriptor.rename(desc, map);
		if (desc != desc2)
			if (cache == null)
				this.descriptor = cp.addUtf8Info(desc2);
			else
			{
				cache.remove(this);
				this.descriptor = cp.addUtf8Info(desc2);
				cache.put(this, this);
			}
	}

	@Override
	public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache)
	{
		String desc = cp.getUtf8Info(this.descriptor);
		String desc2 = Descriptor.rename(desc, oldName, newName);
		if (desc != desc2)
			if (cache == null)
				this.descriptor = cp.addUtf8Info(desc2);
			else
			{
				cache.remove(this);
				this.descriptor = cp.addUtf8Info(desc2);
				cache.put(this, this);
			}
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(MethodTypeInfo.tag);
		out.writeShort(this.descriptor);
	}
}

class NameAndTypeInfo extends ConstInfo
{
	static final int	tag	= 12;
	int					memberName;
	int					typeDescriptor;

	public NameAndTypeInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.memberName = in.readUnsignedShort();
		this.typeDescriptor = in.readUnsignedShort();
	}

	public NameAndTypeInfo(int name, int type, int index)
	{
		super(index);
		this.memberName = name;
		this.typeDescriptor = type;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		String mname = src.getUtf8Info(this.memberName);
		String tdesc = src.getUtf8Info(this.typeDescriptor);
		tdesc = Descriptor.rename(tdesc, map);
		return dest.addNameAndTypeInfo(dest.addUtf8Info(mname), dest.addUtf8Info(tdesc));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof NameAndTypeInfo)
		{
			NameAndTypeInfo nti = (NameAndTypeInfo) obj;
			return nti.memberName == this.memberName && nti.typeDescriptor == this.typeDescriptor;
		} else
			return false;
	}

	@Override
	public int getTag()
	{
		return NameAndTypeInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return this.memberName << 16 ^ this.typeDescriptor;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("NameAndType #");
		out.print(this.memberName);
		out.print(", type #");
		out.println(this.typeDescriptor);
	}

	@Override
	public void renameClass(ConstPool cp, Map map, HashMap cache)
	{
		String type = cp.getUtf8Info(this.typeDescriptor);
		String type2 = Descriptor.rename(type, map);
		if (type != type2)
			if (cache == null)
				this.typeDescriptor = cp.addUtf8Info(type2);
			else
			{
				cache.remove(this);
				this.typeDescriptor = cp.addUtf8Info(type2);
				cache.put(this, this);
			}
	}

	@Override
	public void renameClass(ConstPool cp, String oldName, String newName, HashMap cache)
	{
		String type = cp.getUtf8Info(this.typeDescriptor);
		String type2 = Descriptor.rename(type, oldName, newName);
		if (type != type2)
			if (cache == null)
				this.typeDescriptor = cp.addUtf8Info(type2);
			else
			{
				cache.remove(this);
				this.typeDescriptor = cp.addUtf8Info(type2);
				cache.put(this, this);
			}
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(NameAndTypeInfo.tag);
		out.writeShort(this.memberName);
		out.writeShort(this.typeDescriptor);
	}
}

class StringInfo extends ConstInfo
{
	static final int	tag	= 8;
	int					string;

	public StringInfo(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.string = in.readUnsignedShort();
	}

	public StringInfo(int str, int index)
	{
		super(index);
		this.string = str;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addStringInfo(src.getUtf8Info(this.string));
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof StringInfo && ((StringInfo) obj).string == this.string;
	}

	@Override
	public int getTag()
	{
		return StringInfo.tag;
	}

	@Override
	public int hashCode()
	{
		return this.string;
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("String #");
		out.println(this.string);
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(StringInfo.tag);
		out.writeShort(this.string);
	}
}

class Utf8Info extends ConstInfo
{
	static final int	tag	= 1;
	String				string;

	public Utf8Info(DataInputStream in, int index) throws IOException
	{
		super(index);
		this.string = in.readUTF();
	}

	public Utf8Info(String utf8, int index)
	{
		super(index);
		this.string = utf8;
	}

	@Override
	public int copy(ConstPool src, ConstPool dest, Map map)
	{
		return dest.addUtf8Info(this.string);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Utf8Info && ((Utf8Info) obj).string.equals(this.string);
	}

	@Override
	public int getTag()
	{
		return Utf8Info.tag;
	}

	@Override
	public int hashCode()
	{
		return this.string.hashCode();
	}

	@Override
	public void print(PrintWriter out)
	{
		out.print("UTF8 \"");
		out.print(this.string);
		out.println("\"");
	}

	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeByte(Utf8Info.tag);
		out.writeUTF(this.string);
	}
}
