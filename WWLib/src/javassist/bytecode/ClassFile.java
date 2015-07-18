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
import java.util.ListIterator;
import java.util.Map;

import javassist.CannotCompileException;

/**
 * <code>ClassFile</code> represents a Java <code>.class</code> file, which
 * consists of a constant pool, methods, fields, and attributes.
 * <p>
 * For example, <blockquote>
 *
 * <pre>
 * ClassFile cf = new ClassFile(false, &quot;test.Foo&quot;, null);
 * cf.setInterfaces(new String[]
 * { &quot;java.lang.Cloneable&quot; });
 * 
 * FieldInfo f = new FieldInfo(cf.getConstPool(), &quot;width&quot;, &quot;I&quot;);
 * f.setAccessFlags(AccessFlag.PUBLIC);
 * cf.addField(f);
 * 
 * cf.write(new DataOutputStream(new FileOutputStream(&quot;Foo.class&quot;)));
 * </pre>
 *
 * </blockquote> This code generates a class file <code>Foo.class</code> for the
 * following class: <blockquote>
 *
 * <pre>
 * package test;
 * 
 * class Foo implements Cloneable
 * {
 * 	public int width;
 * }
 * </pre>
 *
 * </blockquote>
 * </p>
 *
 * @see FieldInfo
 * @see MethodInfo
 * @see ClassFileWriter
 * @see javassist.CtClass#getClassFile()
 * @see javassist.ClassPool#makeClass(ClassFile)
 */
public final class ClassFile
{
	/**
	 * The major version number of class files for JDK 1.1.
	 */
	public static final int	JAVA_1			= 45;
	/**
	 * The major version number of class files for JDK 1.2.
	 */
	public static final int	JAVA_2			= 46;
	/**
	 * The major version number of class files for JDK 1.3.
	 */
	public static final int	JAVA_3			= 47;
	/**
	 * The major version number of class files for JDK 1.4.
	 */
	public static final int	JAVA_4			= 48;
	/**
	 * The major version number of class files for JDK 1.5.
	 */
	public static final int	JAVA_5			= 49;
	/**
	 * The major version number of class files for JDK 1.6.
	 */
	public static final int	JAVA_6			= 50;
	/**
	 * The major version number of class files for JDK 1.7.
	 */
	public static final int	JAVA_7			= 51;
	/**
	 * The major version number of class files for JDK 1.8.
	 */
	public static final int	JAVA_8			= 52;
	/**
	 * The major version number of class files created from scratch. The default
	 * value is 47 (JDK 1.3). It is 49 (JDK 1.5) if the JVM supports
	 * <code>java.lang.StringBuilder</code>. It is 50 (JDK 1.6) if the JVM
	 * supports <code>java.util.zip.DeflaterInputStream</code>. It is 51 (JDK
	 * 1.7) if the JVM supports <code>java.lang.invoke.CallSite</code>.
	 */
	public static int		MAJOR_VERSION	= ClassFile.JAVA_3;

	static
	{
		try
		{
			Class.forName("java.lang.StringBuilder");
			ClassFile.MAJOR_VERSION = ClassFile.JAVA_5;
			Class.forName("java.util.zip.DeflaterInputStream");
			ClassFile.MAJOR_VERSION = ClassFile.JAVA_6;
			Class.forName("java.lang.invoke.CallSite");
			ClassFile.MAJOR_VERSION = ClassFile.JAVA_7;
		} catch (Throwable t)
		{
		}
	}

	private static String getSourcefileName(String qname)
	{
		int index = qname.lastIndexOf('.');
		if (index >= 0)
			qname = qname.substring(index + 1);

		return qname + ".java";
	}

	private static boolean isDuplicated(MethodInfo newMethod, String newName, String newDesc, MethodInfo minfo, ListIterator it)
	{
		if (!minfo.getName().equals(newName))
			return false;

		String desc = minfo.getDescriptor();
		if (!Descriptor.eqParamTypes(desc, newDesc))
			return false;

		if (desc.equals(newDesc))
		{
			if (ClassFile.notBridgeMethod(minfo))
				return true;
			else
			{
				// if the bridge method with the same signature
				// already exists, replace it.
				it.remove();
				return false;
			}
		} else
			return false;
		// return notBridgeMethod(minfo) && notBridgeMethod(newMethod);
	}

	/*
	 * For a bridge method, see Sec. 15.12.4.5 of JLS 3rd Ed.
	 */
	private static boolean notBridgeMethod(MethodInfo minfo)
	{
		return (minfo.getAccessFlags() & AccessFlag.BRIDGE) == 0;
	}

	int major, minor;		// version number

	ConstPool constPool;

	int thisClass;

	int accessFlags;

	int superClass;

	int[] interfaces;

	ArrayList fields;

	ArrayList methods;

	ArrayList attributes;

	String thisclassname;		// not JVM-internal name

	String[] cachedInterfaces;

	String cachedSuperclass;

	/**
	 * Constructs a class file including no members.
	 *
	 * @param isInterface
	 *            true if this is an interface. false if this is a class.
	 * @param classname
	 *            a fully-qualified class name
	 * @param superclass
	 *            a fully-qualified super class name or null.
	 */
	public ClassFile(boolean isInterface, String classname, String superclass)
	{
		this.major = ClassFile.MAJOR_VERSION;
		this.minor = 0; // JDK 1.3 or later
		this.constPool = new ConstPool(classname);
		this.thisClass = this.constPool.getThisClassInfo();
		if (isInterface)
			this.accessFlags = AccessFlag.INTERFACE | AccessFlag.ABSTRACT;
		else
			this.accessFlags = AccessFlag.SUPER;

		this.initSuperclass(superclass);
		this.interfaces = null;
		this.fields = new ArrayList();
		this.methods = new ArrayList();
		this.thisclassname = classname;

		this.attributes = new ArrayList();
		this.attributes.add(new SourceFileAttribute(this.constPool, ClassFile.getSourcefileName(this.thisclassname)));
	}

	/**
	 * Constructs a class file from a byte stream.
	 */
	public ClassFile(DataInputStream in) throws IOException
	{
		this.read(in);
	}

	/**
	 * Appends an attribute. If there is already an attribute with the same
	 * name, the new one substitutes for it.
	 *
	 * @see #getAttributes()
	 */
	public void addAttribute(AttributeInfo info)
	{
		AttributeInfo.remove(this.attributes, info.getName());
		this.attributes.add(info);
	}

	/**
	 * Appends a field to the class.
	 *
	 * @throws DuplicateMemberException
	 *             when the field is already included.
	 */
	public void addField(FieldInfo finfo) throws DuplicateMemberException
	{
		this.testExistingField(finfo.getName(), finfo.getDescriptor());
		this.fields.add(finfo);
	}

	/**
	 * Just appends a field to the class. It does not check field duplication.
	 * Use this method only when minimizing performance overheads is seriously
	 * required.
	 *
	 * @since 3.13
	 */
	public final void addField2(FieldInfo finfo)
	{
		this.fields.add(finfo);
	}

	/**
	 * Appends an interface to the interfaces implemented by the class.
	 */
	public void addInterface(String name)
	{
		this.cachedInterfaces = null;
		int info = this.constPool.addClassInfo(name);
		if (this.interfaces == null)
		{
			this.interfaces = new int[1];
			this.interfaces[0] = info;
		} else
		{
			int n = this.interfaces.length;
			int[] newarray = new int[n + 1];
			System.arraycopy(this.interfaces, 0, newarray, 0, n);
			newarray[n] = info;
			this.interfaces = newarray;
		}
	}

	/**
	 * Appends a method to the class. If there is a bridge method with the same
	 * name and signature, then the bridge method is removed before a new method
	 * is added.
	 *
	 * @throws DuplicateMemberException
	 *             when the method is already included.
	 */
	public void addMethod(MethodInfo minfo) throws DuplicateMemberException
	{
		this.testExistingMethod(minfo);
		this.methods.add(minfo);
	}

	/**
	 * Just appends a method to the class. It does not check method duplication
	 * or remove a bridge method. Use this method only when minimizing
	 * performance overheads is seriously required.
	 *
	 * @since 3.13
	 */
	public final void addMethod2(MethodInfo minfo)
	{
		this.methods.add(minfo);
	}

	/**
	 * Eliminates dead constant pool items. If a method or a field is removed,
	 * the constant pool items used by that method/field become dead items. This
	 * method recreates a constant pool.
	 */
	public void compact()
	{
		ConstPool cp = this.compact0();
		ArrayList list = this.methods;
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			minfo.compact(cp);
		}

		list = this.fields;
		n = list.size();
		for (int i = 0; i < n; ++i)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			finfo.compact(cp);
		}

		this.attributes = AttributeInfo.copyAll(this.attributes, cp);
		this.constPool = cp;
	}

	private ConstPool compact0()
	{
		ConstPool cp = new ConstPool(this.thisclassname);
		this.thisClass = cp.getThisClassInfo();
		String sc = this.getSuperclass();
		if (sc != null)
			this.superClass = cp.addClassInfo(this.getSuperclass());

		if (this.interfaces != null)
		{
			int n = this.interfaces.length;
			for (int i = 0; i < n; ++i)
				this.interfaces[i] = cp.addClassInfo(this.constPool.getClassInfo(this.interfaces[i]));
		}

		return cp;
	}

	/**
	 * Returns access flags.
	 *
	 * @see javassist.bytecode.AccessFlag
	 */
	public int getAccessFlags()
	{
		return this.accessFlags;
	}

	/**
	 * Returns the attribute with the specified name. If there are multiple
	 * attributes with that name, this method returns either of them. It returns
	 * null if the specified attributed is not found.
	 *
	 * @param name
	 *            attribute name
	 * @see #getAttributes()
	 */
	public AttributeInfo getAttribute(String name)
	{
		ArrayList list = this.attributes;
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			AttributeInfo ai = (AttributeInfo) list.get(i);
			if (ai.getName().equals(name))
				return ai;
		}

		return null;
	}

	/**
	 * Returns all the attributes. The returned <code>List</code> object is
	 * shared with this object. If you add a new attribute to the list, the
	 * attribute is also added to the classs file represented by this object. If
	 * you remove an attribute from the list, it is also removed from the class
	 * file.
	 *
	 * @return a list of <code>AttributeInfo</code> objects.
	 * @see AttributeInfo
	 */
	public List getAttributes()
	{
		return this.attributes;
	}

	/**
	 * Returns a constant pool table.
	 */
	public ConstPool getConstPool()
	{
		return this.constPool;
	}

	/**
	 * Returns all the fields declared in the class.
	 *
	 * @return a list of <code>FieldInfo</code>.
	 * @see FieldInfo
	 */
	public List getFields()
	{
		return this.fields;
	}

	/**
	 * Returns access and property flags of this nested class. This method
	 * returns -1 if the class is not a nested class.
	 * <p>
	 * The returned value is obtained from <code>inner_class_access_flags</code>
	 * of the entry representing this nested class itself in
	 * <code>InnerClasses_attribute</code>.
	 */
	public int getInnerAccessFlags()
	{
		InnerClassesAttribute ica = (InnerClassesAttribute) this.getAttribute(InnerClassesAttribute.tag);
		if (ica == null)
			return -1;

		String name = this.getName();
		int n = ica.tableLength();
		for (int i = 0; i < n; ++i)
			if (name.equals(ica.innerClass(i)))
				return ica.accessFlags(i);

		return -1;
	}

	/**
	 * Returns the names of the interfaces implemented by the class. The
	 * returned array is read only.
	 */
	public String[] getInterfaces()
	{
		if (this.cachedInterfaces != null)
			return this.cachedInterfaces;

		String[] rtn = null;
		if (this.interfaces == null)
			rtn = new String[0];
		else
		{
			int n = this.interfaces.length;
			String[] list = new String[n];
			for (int i = 0; i < n; ++i)
				list[i] = this.constPool.getClassInfo(this.interfaces[i]);

			rtn = list;
		}

		this.cachedInterfaces = rtn;
		return rtn;
	}

	/**
	 * Get the Major version.
	 *
	 * @return the major version
	 */
	public int getMajorVersion()
	{
		return this.major;
	}

	/**
	 * Returns the method with the specified name. If there are multiple methods
	 * with that name, this method returns one of them.
	 *
	 * @return null if no such method is found.
	 */
	public MethodInfo getMethod(String name)
	{
		ArrayList list = this.methods;
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			if (minfo.getName().equals(name))
				return minfo;
		}

		return null;
	}

	/**
	 * Returns all the methods declared in the class.
	 *
	 * @return a list of <code>MethodInfo</code>.
	 * @see MethodInfo
	 */
	public List getMethods()
	{
		return this.methods;
	}

	/**
	 * Get the minor version.
	 *
	 * @return the minor version
	 */
	public int getMinorVersion()
	{
		return this.minor;
	}

	/**
	 * Returns the class name.
	 */
	public String getName()
	{
		return this.thisclassname;
	}

	/**
	 * Internal-use only. <code>CtClass.getRefClasses()</code> calls this
	 * method.
	 */
	public final void getRefClasses(Map classnames)
	{
		this.constPool.renameClass(classnames);

		AttributeInfo.getRefClasses(this.attributes, classnames);
		ArrayList list = this.methods;
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			String desc = minfo.getDescriptor();
			Descriptor.rename(desc, classnames);
			AttributeInfo.getRefClasses(minfo.getAttributes(), classnames);
		}

		list = this.fields;
		n = list.size();
		for (int i = 0; i < n; ++i)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			String desc = finfo.getDescriptor();
			Descriptor.rename(desc, classnames);
			AttributeInfo.getRefClasses(finfo.getAttributes(), classnames);
		}
	}

	/**
	 * Returns the source file containing this class.
	 *
	 * @return null if this information is not available.
	 */
	public String getSourceFile()
	{
		SourceFileAttribute sf = (SourceFileAttribute) this.getAttribute(SourceFileAttribute.tag);
		if (sf == null)
			return null;
		else
			return sf.getFileName();
	}

	/**
	 * Returns a static initializer (class initializer), or null if it does not
	 * exist.
	 */
	public MethodInfo getStaticInitializer()
	{
		return this.getMethod(MethodInfo.nameClinit);
	}

	/**
	 * Returns the super class name.
	 */
	public String getSuperclass()
	{
		if (this.cachedSuperclass == null)
			this.cachedSuperclass = this.constPool.getClassInfo(this.superClass);

		return this.cachedSuperclass;
	}

	/**
	 * Returns the index of the constant pool entry representing the super
	 * class.
	 */
	public int getSuperclassId()
	{
		return this.superClass;
	}

	private void initSuperclass(String superclass)
	{
		if (superclass != null)
		{
			this.superClass = this.constPool.addClassInfo(superclass);
			this.cachedSuperclass = superclass;
		} else
		{
			this.superClass = this.constPool.addClassInfo("java.lang.Object");
			this.cachedSuperclass = "java.lang.Object";
		}
	}

	/**
	 * Returns true if this is an abstract class or an interface.
	 */
	public boolean isAbstract()
	{
		return (this.accessFlags & AccessFlag.ABSTRACT) != 0;
	}

	/**
	 * Returns true if this is a final class or interface.
	 */
	public boolean isFinal()
	{
		return (this.accessFlags & AccessFlag.FINAL) != 0;
	}

	/**
	 * Returns true if this is an interface.
	 */
	public boolean isInterface()
	{
		return (this.accessFlags & AccessFlag.INTERFACE) != 0;
	}

	/**
	 * Discards all attributes, associated with both the class file and the
	 * members such as a code attribute and exceptions attribute. The unused
	 * constant pool entries are also discarded (a new packed constant pool is
	 * constructed).
	 */
	public void prune()
	{
		ConstPool cp = this.compact0();
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

		ArrayList list = this.methods;
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			minfo.prune(cp);
		}

		list = this.fields;
		n = list.size();
		for (int i = 0; i < n; ++i)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			finfo.prune(cp);
		}

		this.attributes = newAttributes;
		this.constPool = cp;
	}

	private void read(DataInputStream in) throws IOException
	{
		int i, n;
		int magic = in.readInt();
		if (magic != 0xCAFEBABE)
			throw new IOException("bad magic number: " + Integer.toHexString(magic));

		this.minor = in.readUnsignedShort();
		this.major = in.readUnsignedShort();
		this.constPool = new ConstPool(in);
		this.accessFlags = in.readUnsignedShort();
		this.thisClass = in.readUnsignedShort();
		this.constPool.setThisClassInfo(this.thisClass);
		this.superClass = in.readUnsignedShort();
		n = in.readUnsignedShort();
		if (n == 0)
			this.interfaces = null;
		else
		{
			this.interfaces = new int[n];
			for (i = 0; i < n; ++i)
				this.interfaces[i] = in.readUnsignedShort();
		}

		ConstPool cp = this.constPool;
		n = in.readUnsignedShort();
		this.fields = new ArrayList();
		for (i = 0; i < n; ++i)
			this.addField2(new FieldInfo(cp, in));

		n = in.readUnsignedShort();
		this.methods = new ArrayList();
		for (i = 0; i < n; ++i)
			this.addMethod2(new MethodInfo(cp, in));

		this.attributes = new ArrayList();
		n = in.readUnsignedShort();
		for (i = 0; i < n; ++i)
			this.addAttribute(AttributeInfo.read(cp, in));

		this.thisclassname = this.constPool.getClassInfo(this.thisClass);
	}

	/**
	 * Replaces all occurrences of several class names in the class file.
	 *
	 * @param classnames
	 *            specifies which class name is replaced with which new name.
	 *            Class names must be described with the JVM-internal
	 *            representation like <code>java/lang/Object</code>.
	 * @see #renameClass(String,String)
	 */
	public final void renameClass(Map classnames)
	{
		String jvmNewThisName = (String) classnames.get(Descriptor.toJvmName(this.thisclassname));
		if (jvmNewThisName != null)
			this.thisclassname = Descriptor.toJavaName(jvmNewThisName);

		this.constPool.renameClass(classnames);

		AttributeInfo.renameClass(this.attributes, classnames);
		ArrayList list = this.methods;
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			String desc = minfo.getDescriptor();
			minfo.setDescriptor(Descriptor.rename(desc, classnames));
			AttributeInfo.renameClass(minfo.getAttributes(), classnames);
		}

		list = this.fields;
		n = list.size();
		for (int i = 0; i < n; ++i)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			String desc = finfo.getDescriptor();
			finfo.setDescriptor(Descriptor.rename(desc, classnames));
			AttributeInfo.renameClass(finfo.getAttributes(), classnames);
		}
	}

	/**
	 * Replaces all occurrences of a class name in the class file.
	 * <p>
	 * If class X is substituted for class Y in the class file, X and Y must
	 * have the same signature. If Y provides a method m(), X must provide it
	 * even if X inherits m() from the super class. If this fact is not
	 * guaranteed, the bytecode verifier may cause an error.
	 *
	 * @param oldname
	 *            the replaced class name
	 * @param newname
	 *            the substituted class name
	 */
	public final void renameClass(String oldname, String newname)
	{
		ArrayList list;
		int n;

		if (oldname.equals(newname))
			return;

		if (oldname.equals(this.thisclassname))
			this.thisclassname = newname;

		oldname = Descriptor.toJvmName(oldname);
		newname = Descriptor.toJvmName(newname);
		this.constPool.renameClass(oldname, newname);

		AttributeInfo.renameClass(this.attributes, oldname, newname);
		list = this.methods;
		n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			String desc = minfo.getDescriptor();
			minfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
			AttributeInfo.renameClass(minfo.getAttributes(), oldname, newname);
		}

		list = this.fields;
		n = list.size();
		for (int i = 0; i < n; ++i)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			String desc = finfo.getDescriptor();
			finfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
			AttributeInfo.renameClass(finfo.getAttributes(), oldname, newname);
		}
	}

	/**
	 * Changes access flags.
	 *
	 * @see javassist.bytecode.AccessFlag
	 */
	public void setAccessFlags(int acc)
	{
		if ((acc & AccessFlag.INTERFACE) == 0)
			acc |= AccessFlag.SUPER;

		this.accessFlags = acc;
	}

	/**
	 * Sets the interfaces.
	 *
	 * @param nameList
	 *            the names of the interfaces.
	 */
	public void setInterfaces(String[] nameList)
	{
		this.cachedInterfaces = null;
		if (nameList != null)
		{
			int n = nameList.length;
			this.interfaces = new int[n];
			for (int i = 0; i < n; ++i)
				this.interfaces[i] = this.constPool.addClassInfo(nameList[i]);
		}
	}

	/**
	 * Set the major version.
	 *
	 * @param major
	 *            the major version
	 */
	public void setMajorVersion(int major)
	{
		this.major = major;
	}

	/**
	 * Set the minor version.
	 *
	 * @param minor
	 *            the minor version
	 */
	public void setMinorVersion(int minor)
	{
		this.minor = minor;
	}

	/**
	 * Sets the class name. This method substitutes the new name for all
	 * occurrences of the old class name in the class file.
	 */
	public void setName(String name)
	{
		this.renameClass(this.thisclassname, name);
	}

	/**
	 * Sets the super class.
	 * <p>
	 * The new super class should inherit from the old super class. This method
	 * modifies constructors so that they call constructors declared in the new
	 * super class.
	 */
	public void setSuperclass(String superclass) throws CannotCompileException
	{
		if (superclass == null)
			superclass = "java.lang.Object";

		try
		{
			this.superClass = this.constPool.addClassInfo(superclass);
			ArrayList list = this.methods;
			int n = list.size();
			for (int i = 0; i < n; ++i)
			{
				MethodInfo minfo = (MethodInfo) list.get(i);
				minfo.setSuperclass(superclass);
			}
		} catch (BadBytecode e)
		{
			throw new CannotCompileException(e);
		}
		this.cachedSuperclass = superclass;
	}

	/**
	 * Sets the major and minor version to Java 5. If the major version is older
	 * than 49, Java 5 extensions such as annotations are ignored by the JVM.
	 */
	public void setVersionToJava5()
	{
		this.major = 49;
		this.minor = 0;
	}

	private void testExistingField(String name, String descriptor) throws DuplicateMemberException
	{
		ListIterator it = this.fields.listIterator(0);
		while (it.hasNext())
		{
			FieldInfo minfo = (FieldInfo) it.next();
			if (minfo.getName().equals(name))
				throw new DuplicateMemberException("duplicate field: " + name);
		}
	}

	private void testExistingMethod(MethodInfo newMinfo) throws DuplicateMemberException
	{
		String name = newMinfo.getName();
		String descriptor = newMinfo.getDescriptor();
		ListIterator it = this.methods.listIterator(0);
		while (it.hasNext())
			if (ClassFile.isDuplicated(newMinfo, name, descriptor, (MethodInfo) it.next(), it))
				throw new DuplicateMemberException("duplicate method: " + name + " in " + this.getName());
	}

	/**
	 * Writes a class file represented by this object into an output stream.
	 */
	public void write(DataOutputStream out) throws IOException
	{
		int i, n;

		out.writeInt(0xCAFEBABE); // magic
		out.writeShort(this.minor); // minor version
		out.writeShort(this.major); // major version
		this.constPool.write(out); // constant pool
		out.writeShort(this.accessFlags);
		out.writeShort(this.thisClass);
		out.writeShort(this.superClass);

		if (this.interfaces == null)
			n = 0;
		else
			n = this.interfaces.length;

		out.writeShort(n);
		for (i = 0; i < n; ++i)
			out.writeShort(this.interfaces[i]);

		ArrayList list = this.fields;
		n = list.size();
		out.writeShort(n);
		for (i = 0; i < n; ++i)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			finfo.write(out);
		}

		list = this.methods;
		n = list.size();
		out.writeShort(n);
		for (i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			minfo.write(out);
		}

		out.writeShort(this.attributes.size());
		AttributeInfo.writeAll(this.attributes, out);
	}
}
