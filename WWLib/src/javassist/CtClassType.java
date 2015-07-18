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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ConstantAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.EnclosingMethodAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.InnerClassesAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.compiler.AccessorMaker;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.expr.ExprEditor;

/**
 * Class types.
 */
class CtClassType extends CtClass
{
	private static final int GET_THRESHOLD = 2;	// see compress()

	static Object getAnnotationType(Class clz, ClassPool cp, AnnotationsAttribute a1, AnnotationsAttribute a2) throws ClassNotFoundException
	{
		Annotation[] anno1, anno2;

		if (a1 == null)
			anno1 = null;
		else
			anno1 = a1.getAnnotations();

		if (a2 == null)
			anno2 = null;
		else
			anno2 = a2.getAnnotations();

		String typeName = clz.getName();
		if (anno1 != null)
			for (int i = 0; i < anno1.length; i++)
				if (anno1[i].getTypeName().equals(typeName))
					return CtClassType.toAnnoType(anno1[i], cp);

		if (anno2 != null)
			for (int i = 0; i < anno2.length; i++)
				if (anno2[i].getTypeName().equals(typeName))
					return CtClassType.toAnnoType(anno2[i], cp);

		return null;
	}

	private static void getFields(ArrayList alist, CtClass cc)
	{
		int i, num;
		if (cc == null)
			return;

		try
		{
			CtClassType.getFields(alist, cc.getSuperclass());
		} catch (NotFoundException e)
		{
		}

		try
		{
			CtClass[] ifs = cc.getInterfaces();
			num = ifs.length;
			for (i = 0; i < num; ++i)
				CtClassType.getFields(alist, ifs[i]);
		} catch (NotFoundException e)
		{
		}

		CtMember.Cache memCache = ((CtClassType) cc).getMembers();
		CtMember field = memCache.fieldHead();
		CtMember tail = memCache.lastField();
		while (field != tail)
		{
			field = field.next();
			if (!Modifier.isPrivate(field.getModifiers()))
				alist.add(field);
		}
	}

	private static CtMethod getMethod0(CtClass cc, String name, String desc)
	{
		if (cc instanceof CtClassType)
		{
			CtMember.Cache memCache = ((CtClassType) cc).getMembers();
			CtMember mth = memCache.methodHead();
			CtMember mthTail = memCache.lastMethod();

			while (mth != mthTail)
			{
				mth = mth.next();
				if (mth.getName().equals(name) && ((CtMethod) mth).getMethodInfo2().getDescriptor().equals(desc))
					return (CtMethod) mth;
			}
		}

		try
		{
			CtClass s = cc.getSuperclass();
			if (s != null)
			{
				CtMethod m = CtClassType.getMethod0(s, name, desc);
				if (m != null)
					return m;
			}
		} catch (NotFoundException e)
		{
		}

		try
		{
			CtClass[] ifs = cc.getInterfaces();
			int size = ifs.length;
			for (int i = 0; i < size; ++i)
			{
				CtMethod m = CtClassType.getMethod0(ifs[i], name, desc);
				if (m != null)
					return m;
			}
		} catch (NotFoundException e)
		{
		}
		return null;
	}

	private static void getMethods0(HashMap h, CtClass cc)
	{
		try
		{
			CtClass[] ifs = cc.getInterfaces();
			int size = ifs.length;
			for (int i = 0; i < size; ++i)
				CtClassType.getMethods0(h, ifs[i]);
		} catch (NotFoundException e)
		{
		}

		try
		{
			CtClass s = cc.getSuperclass();
			if (s != null)
				CtClassType.getMethods0(h, s);
		} catch (NotFoundException e)
		{
		}

		if (cc instanceof CtClassType)
		{
			CtMember.Cache memCache = ((CtClassType) cc).getMembers();
			CtMember mth = memCache.methodHead();
			CtMember mthTail = memCache.lastMethod();

			while (mth != mthTail)
			{
				mth = mth.next();
				if (!Modifier.isPrivate(mth.getModifiers()))
					h.put(((CtMethod) mth).getStringRep(), mth);
			}
		}
	}

	static boolean hasAnnotationType(Class clz, ClassPool cp, AnnotationsAttribute a1, AnnotationsAttribute a2)
	{
		Annotation[] anno1, anno2;

		if (a1 == null)
			anno1 = null;
		else
			anno1 = a1.getAnnotations();

		if (a2 == null)
			anno2 = null;
		else
			anno2 = a2.getAnnotations();

		String typeName = clz.getName();
		if (anno1 != null)
			for (int i = 0; i < anno1.length; i++)
				if (anno1[i].getTypeName().equals(typeName))
					return true;

		if (anno2 != null)
			for (int i = 0; i < anno2.length; i++)
				if (anno2[i].getTypeName().equals(typeName))
					return true;

		return false;
	}

	private static void insertAuxInitializer(CodeAttribute codeAttr, Bytecode initializer, int stacksize) throws BadBytecode
	{
		CodeIterator it = codeAttr.iterator();
		int index = it.skipSuperConstructor();
		if (index < 0)
		{
			index = it.skipThisConstructor();
			if (index >= 0)
				return; // this() is called.

			// Neither this() or super() is called.
		}

		int pos = it.insertEx(initializer.get());
		it.insert(initializer.getExceptionTable(), pos);
		int maxstack = codeAttr.getMaxStack();
		if (maxstack < stacksize)
			codeAttr.setMaxStack(stacksize);
	}

	private static boolean isPubCons(CtConstructor cons)
	{
		return !Modifier.isPrivate(cons.getModifiers()) && cons.isConstructor();
	}

	private static boolean notFindInArray(String prefix, String[] values)
	{
		int len = values.length;
		for (int i = 0; i < len; i++)
			if (values[i].startsWith(prefix))
				return false;

		return true;
	}

	static Object[] toAnnotationType(boolean ignoreNotFound, ClassPool cp, AnnotationsAttribute a1, AnnotationsAttribute a2) throws ClassNotFoundException
	{
		Annotation[] anno1, anno2;
		int size1, size2;

		if (a1 == null)
		{
			anno1 = null;
			size1 = 0;
		} else
		{
			anno1 = a1.getAnnotations();
			size1 = anno1.length;
		}

		if (a2 == null)
		{
			anno2 = null;
			size2 = 0;
		} else
		{
			anno2 = a2.getAnnotations();
			size2 = anno2.length;
		}

		if (!ignoreNotFound)
		{
			Object[] result = new Object[size1 + size2];
			for (int i = 0; i < size1; i++)
				result[i] = CtClassType.toAnnoType(anno1[i], cp);

			for (int j = 0; j < size2; j++)
				result[j + size1] = CtClassType.toAnnoType(anno2[j], cp);

			return result;
		} else
		{
			ArrayList annotations = new ArrayList();
			for (int i = 0; i < size1; i++)
				try
				{
					annotations.add(CtClassType.toAnnoType(anno1[i], cp));
				} catch (ClassNotFoundException e)
				{
				}
			for (int j = 0; j < size2; j++)
				try
				{
					annotations.add(CtClassType.toAnnoType(anno2[j], cp));
				} catch (ClassNotFoundException e)
				{
				}

			return annotations.toArray();
		}
	}

	static Object[][] toAnnotationType(boolean ignoreNotFound, ClassPool cp, ParameterAnnotationsAttribute a1, ParameterAnnotationsAttribute a2, MethodInfo minfo) throws ClassNotFoundException
	{
		int numParameters = 0;
		if (a1 != null)
			numParameters = a1.numParameters();
		else if (a2 != null)
			numParameters = a2.numParameters();
		else
			numParameters = Descriptor.numOfParameters(minfo.getDescriptor());

		Object[][] result = new Object[numParameters][];
		for (int i = 0; i < numParameters; i++)
		{
			Annotation[] anno1, anno2;
			int size1, size2;

			if (a1 == null)
			{
				anno1 = null;
				size1 = 0;
			} else
			{
				anno1 = a1.getAnnotations()[i];
				size1 = anno1.length;
			}

			if (a2 == null)
			{
				anno2 = null;
				size2 = 0;
			} else
			{
				anno2 = a2.getAnnotations()[i];
				size2 = anno2.length;
			}

			if (!ignoreNotFound)
			{
				result[i] = new Object[size1 + size2];
				for (int j = 0; j < size1; ++j)
					result[i][j] = CtClassType.toAnnoType(anno1[j], cp);

				for (int j = 0; j < size2; ++j)
					result[i][j + size1] = CtClassType.toAnnoType(anno2[j], cp);
			} else
			{
				ArrayList annotations = new ArrayList();
				for (int j = 0; j < size1; j++)
					try
					{
						annotations.add(CtClassType.toAnnoType(anno1[j], cp));
					} catch (ClassNotFoundException e)
					{
					}
				for (int j = 0; j < size2; j++)
					try
					{
						annotations.add(CtClassType.toAnnoType(anno2[j], cp));
					} catch (ClassNotFoundException e)
					{
					}

				result[i] = annotations.toArray();
			}
		}

		return result;
	}

	private static Object toAnnoType(Annotation anno, ClassPool cp) throws ClassNotFoundException
	{
		try
		{
			ClassLoader cl = cp.getClassLoader();
			return anno.toAnnotationType(cl, cp);
		} catch (ClassNotFoundException e)
		{
			ClassLoader cl2 = cp.getClass().getClassLoader();
			try
			{
				return anno.toAnnotationType(cl2, cp);
			} catch (ClassNotFoundException e2)
			{
				try
				{
					Class<?> clazz = cp.get(anno.getTypeName()).toClass();
					return javassist.bytecode.annotation.AnnotationImpl.make(clazz.getClassLoader(), clazz, cp, anno);
				} catch (Throwable e3)
				{
					throw new ClassNotFoundException(anno.getTypeName());
				}
			}
		}
	}

	ClassPool		classPool;
	boolean			wasChanged;
	private boolean	wasFrozen;

	boolean wasPruned;

	boolean gcConstPool;						// if true, the
	// constant pool
	// entries will
	// be garbage
	// collected.

	ClassFile classfile;

	byte[] rawClassfile;						// backup
	// storage

	private WeakReference memberCache;

	private AccessorMaker accessors;

	private FieldInitLink fieldInitializers;

	private Hashtable hiddenMethods;						// must be
	// synchronous

	private int uniqueNumberSeed;

	private boolean doPruning = ClassPool.doPruning;

	private int getCount;

	CtClassType(ClassFile cf, ClassPool cp)
	{
		this((String) null, cp);
		this.classfile = cf;
		this.qualifiedName = this.classfile.getName();
	}

	CtClassType(InputStream ins, ClassPool cp) throws IOException
	{
		this((String) null, cp);
		this.classfile = new ClassFile(new DataInputStream(ins));
		this.qualifiedName = this.classfile.getName();
	}

	CtClassType(String name, ClassPool cp)
	{
		super(name);
		this.classPool = cp;
		this.wasChanged = this.wasFrozen = this.wasPruned = this.gcConstPool = false;
		this.classfile = null;
		this.rawClassfile = null;
		this.memberCache = null;
		this.accessors = null;
		this.fieldInitializers = null;
		this.hiddenMethods = null;
		this.uniqueNumberSeed = 0;
		this.getCount = 0;
	}

	@Override
	public void addConstructor(CtConstructor c) throws CannotCompileException
	{
		this.checkModify();
		if (c.getDeclaringClass() != this)
			throw new CannotCompileException("cannot add");

		this.getMembers().addConstructor(c);
		this.getClassFile2().addMethod(c.getMethodInfo2());
	}

	@Override
	public void addField(CtField f, CtField.Initializer init) throws CannotCompileException
	{
		this.checkModify();
		if (f.getDeclaringClass() != this)
			throw new CannotCompileException("cannot add");

		if (init == null)
			init = f.getInit();

		if (init != null)
		{
			init.check(f.getSignature());
			int mod = f.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isFinal(mod))
				try
				{
					ConstPool cp = this.getClassFile2().getConstPool();
					int index = init.getConstantValue(cp, f.getType());
					if (index != 0)
					{
						f.getFieldInfo2().addAttribute(new ConstantAttribute(cp, index));
						init = null;
					}
				} catch (NotFoundException e)
				{
				}
		}

		this.getMembers().addField(f);
		this.getClassFile2().addField(f.getFieldInfo2());

		if (init != null)
		{
			FieldInitLink fil = new FieldInitLink(f, init);
			FieldInitLink link = this.fieldInitializers;
			if (link == null)
				this.fieldInitializers = fil;
			else
			{
				while (link.next != null)
					link = link.next;

				link.next = fil;
			}
		}
	}

	@Override
	public void addField(CtField f, String init) throws CannotCompileException
	{
		this.addField(f, CtField.Initializer.byExpr(init));
	}

	@Override
	public void addInterface(CtClass anInterface)
	{
		this.checkModify();
		if (anInterface != null)
			this.getClassFile2().addInterface(anInterface.getName());
	}

	@Override
	public void addMethod(CtMethod m) throws CannotCompileException
	{
		this.checkModify();
		if (m.getDeclaringClass() != this)
			throw new CannotCompileException("bad declaring class");

		int mod = m.getModifiers();
		if ((this.getModifiers() & Modifier.INTERFACE) != 0)
		{
			m.setModifiers(mod | Modifier.PUBLIC);
			if ((mod & Modifier.ABSTRACT) == 0)
				throw new CannotCompileException("an interface method must be abstract: " + m.toString());
		}

		this.getMembers().addMethod(m);
		this.getClassFile2().addMethod(m.getMethodInfo2());
		if ((mod & Modifier.ABSTRACT) != 0)
			this.setModifiers(this.getModifiers() | Modifier.ABSTRACT);
	}

	private CtField checkGetField(CtField f, String name, String desc) throws NotFoundException
	{
		if (f == null)
		{
			String msg = "field: " + name;
			if (desc != null)
				msg += " type " + desc;

			throw new NotFoundException(msg + " in " + this.getName());
		} else
			return f;
	}

	@Override
	void checkModify() throws RuntimeException
	{
		if (this.isFrozen())
		{
			String msg = this.getName() + " class is frozen";
			if (this.wasPruned)
				msg += " and pruned";

			throw new RuntimeException(msg);
		}

		this.wasChanged = true;
	}

	/*
	 * See also checkModified()
	 */
	private void checkPruned(String method)
	{
		if (this.wasPruned)
			throw new RuntimeException(method + "(): " + this.getName() + " was pruned.");
	}

	/**
	 * Invoked from ClassPool#compress(). It releases the class files that have
	 * not been recently used if they are unmodified.
	 */
	@Override
	void compress()
	{
		if (this.getCount < CtClassType.GET_THRESHOLD)
			if (!this.isModified() && ClassPool.releaseUnmodifiedClassFile)
				this.removeClassFile();
			else if (this.isFrozen() && !this.wasPruned)
				this.saveClassFile();

		this.getCount = 0;
	}

	@Override
	public void defrost()
	{
		this.checkPruned("defrost");
		this.wasFrozen = false;
	}

	private void dumpClassFile(ClassFile cf) throws IOException
	{
		DataOutputStream dump = this.makeFileOutput(CtClass.debugDump);
		try
		{
			cf.write(dump);
		} finally
		{
			dump.close();
		}
	}

	@Override
	protected void extendToString(StringBuffer buffer)
	{
		if (this.wasChanged)
			buffer.append("changed ");

		if (this.wasFrozen)
			buffer.append("frozen ");

		if (this.wasPruned)
			buffer.append("pruned ");

		buffer.append(Modifier.toString(this.getModifiers()));
		buffer.append(" class ");
		buffer.append(this.getName());

		try
		{
			CtClass ext = this.getSuperclass();
			if (ext != null)
			{
				String name = ext.getName();
				if (!name.equals("java.lang.Object"))
					buffer.append(" extends " + ext.getName());
			}
		} catch (NotFoundException e)
		{
			buffer.append(" extends ??");
		}

		try
		{
			CtClass[] intf = this.getInterfaces();
			if (intf.length > 0)
				buffer.append(" implements ");

			for (int i = 0; i < intf.length; ++i)
			{
				buffer.append(intf[i].getName());
				buffer.append(", ");
			}
		} catch (NotFoundException e)
		{
			buffer.append(" extends ??");
		}

		CtMember.Cache memCache = this.getMembers();
		this.exToString(buffer, " fields=", memCache.fieldHead(), memCache.lastField());
		this.exToString(buffer, " constructors=", memCache.consHead(), memCache.lastCons());
		this.exToString(buffer, " methods=", memCache.methodHead(), memCache.lastMethod());
	}

	private void exToString(StringBuffer buffer, String msg, CtMember head, CtMember tail)
	{
		buffer.append(msg);
		while (head != tail)
		{
			head = head.next();
			buffer.append(head);
			buffer.append(", ");
		}
	}

	@Override
	public void freeze()
	{
		this.wasFrozen = true;
	}

	@Override
	public AccessorMaker getAccessorMaker()
	{
		if (this.accessors == null)
			this.accessors = new AccessorMaker(this);

		return this.accessors;
	}

	@Override
	public Object getAnnotation(Class clz) throws ClassNotFoundException
	{
		ClassFile cf = this.getClassFile2();
		AnnotationsAttribute ainfo = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.invisibleTag);
		AnnotationsAttribute ainfo2 = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.visibleTag);
		return CtClassType.getAnnotationType(clz, this.getClassPool(), ainfo, ainfo2);
	}

	@Override
	public Object[] getAnnotations() throws ClassNotFoundException
	{
		return this.getAnnotations(false);
	}

	private Object[] getAnnotations(boolean ignoreNotFound) throws ClassNotFoundException
	{
		ClassFile cf = this.getClassFile2();
		AnnotationsAttribute ainfo = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.invisibleTag);
		AnnotationsAttribute ainfo2 = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.visibleTag);
		return CtClassType.toAnnotationType(ignoreNotFound, this.getClassPool(), ainfo, ainfo2);
	}

	@Override
	public byte[] getAttribute(String name)
	{
		AttributeInfo ai = this.getClassFile2().getAttribute(name);
		if (ai == null)
			return null;
		else
			return ai.get();
	}

	@Override
	public Object[] getAvailableAnnotations()
	{
		try
		{
			return this.getAnnotations(true);
		} catch (ClassNotFoundException e)
		{
			throw new RuntimeException("Unexpected exception ", e);
		}
	}

	@Override
	public ClassFile getClassFile2()
	{
		ClassFile cfile = this.classfile;
		if (cfile != null)
			return cfile;

		this.classPool.compress();
		if (this.rawClassfile != null)
			try
			{
				this.classfile = new ClassFile(new DataInputStream(new ByteArrayInputStream(this.rawClassfile)));
				this.rawClassfile = null;
				this.getCount = CtClassType.GET_THRESHOLD;
				return this.classfile;
			} catch (IOException e)
			{
				throw new RuntimeException(e.toString(), e);
			}

		InputStream fin = null;
		try
		{
			fin = this.classPool.openClassfile(this.getName());
			if (fin == null)
				throw new NotFoundException(this.getName());

			fin = new BufferedInputStream(fin);
			ClassFile cf = new ClassFile(new DataInputStream(fin));
			if (!cf.getName().equals(this.qualifiedName))
				throw new RuntimeException("cannot find " + this.qualifiedName + ": " + cf.getName() + " found in " + this.qualifiedName.replace('.', '/') + ".class");

			this.classfile = cf;
			return cf;
		} catch (NotFoundException e)
		{
			throw new RuntimeException(e.toString(), e);
		} catch (IOException e)
		{
			throw new RuntimeException(e.toString(), e);
		} finally
		{
			if (fin != null)
				try
				{
					fin.close();
				} catch (IOException e)
				{
				}
		}
	}

	@Override
	public CtConstructor getClassInitializer()
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember cons = memCache.consHead();
		CtMember consTail = memCache.lastCons();

		while (cons != consTail)
		{
			cons = cons.next();
			CtConstructor cc = (CtConstructor) cons;
			if (cc.isClassInitializer())
				return cc;
		}

		return null;
	}

	@Override
	public ClassPool getClassPool()
	{
		return this.classPool;
	}

	@Override
	public CtConstructor getConstructor(String desc) throws NotFoundException
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember cons = memCache.consHead();
		CtMember consTail = memCache.lastCons();

		while (cons != consTail)
		{
			cons = cons.next();
			CtConstructor cc = (CtConstructor) cons;
			if (cc.getMethodInfo2().getDescriptor().equals(desc) && cc.isConstructor())
				return cc;
		}

		return super.getConstructor(desc);
	}

	@Override
	public CtConstructor[] getConstructors()
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember cons = memCache.consHead();
		CtMember consTail = memCache.lastCons();

		int n = 0;
		CtMember mem = cons;
		while (mem != consTail)
		{
			mem = mem.next();
			if (CtClassType.isPubCons((CtConstructor) mem))
				n++;
		}

		CtConstructor[] result = new CtConstructor[n];
		int i = 0;
		mem = cons;
		while (mem != consTail)
		{
			mem = mem.next();
			CtConstructor cc = (CtConstructor) mem;
			if (CtClassType.isPubCons(cc))
				result[i++] = cc;
		}

		return result;
	}

	@Override
	public CtBehavior[] getDeclaredBehaviors()
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember cons = memCache.consHead();
		CtMember consTail = memCache.lastCons();
		int cnum = CtMember.Cache.count(cons, consTail);
		CtMember mth = memCache.methodHead();
		CtMember mthTail = memCache.lastMethod();
		int mnum = CtMember.Cache.count(mth, mthTail);

		CtBehavior[] cb = new CtBehavior[cnum + mnum];
		int i = 0;
		while (cons != consTail)
		{
			cons = cons.next();
			cb[i++] = (CtBehavior) cons;
		}

		while (mth != mthTail)
		{
			mth = mth.next();
			cb[i++] = (CtBehavior) mth;
		}

		return cb;
	}

	@Override
	public CtConstructor[] getDeclaredConstructors()
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember cons = memCache.consHead();
		CtMember consTail = memCache.lastCons();

		int n = 0;
		CtMember mem = cons;
		while (mem != consTail)
		{
			mem = mem.next();
			CtConstructor cc = (CtConstructor) mem;
			if (cc.isConstructor())
				n++;
		}

		CtConstructor[] result = new CtConstructor[n];
		int i = 0;
		mem = cons;
		while (mem != consTail)
		{
			mem = mem.next();
			CtConstructor cc = (CtConstructor) mem;
			if (cc.isConstructor())
				result[i++] = cc;
		}

		return result;
	}

	@Override
	public CtField getDeclaredField(String name) throws NotFoundException
	{
		return this.getDeclaredField(name, null);
	}

	@Override
	public CtField getDeclaredField(String name, String desc) throws NotFoundException
	{
		CtField f = this.getDeclaredField2(name, desc);
		return this.checkGetField(f, name, desc);
	}

	private CtField getDeclaredField2(String name, String desc)
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember field = memCache.fieldHead();
		CtMember tail = memCache.lastField();
		while (field != tail)
		{
			field = field.next();
			if (field.getName().equals(name) && (desc == null || desc.equals(field.getSignature())))
				return (CtField) field;
		}

		return null;
	}

	@Override
	public CtField[] getDeclaredFields()
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember field = memCache.fieldHead();
		CtMember tail = memCache.lastField();
		int num = CtMember.Cache.count(field, tail);
		CtField[] cfs = new CtField[num];
		int i = 0;
		while (field != tail)
		{
			field = field.next();
			cfs[i++] = (CtField) field;
		}

		return cfs;
	}

	@Override
	public CtMethod getDeclaredMethod(String name) throws NotFoundException
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember mth = memCache.methodHead();
		CtMember mthTail = memCache.lastMethod();
		while (mth != mthTail)
		{
			mth = mth.next();
			if (mth.getName().equals(name))
				return (CtMethod) mth;
		}

		throw new NotFoundException(name + "(..) is not found in " + this.getName());
	}

	@Override
	public CtMethod getDeclaredMethod(String name, CtClass[] params) throws NotFoundException
	{
		String desc = Descriptor.ofParameters(params);
		CtMember.Cache memCache = this.getMembers();
		CtMember mth = memCache.methodHead();
		CtMember mthTail = memCache.lastMethod();

		while (mth != mthTail)
		{
			mth = mth.next();
			if (mth.getName().equals(name) && ((CtMethod) mth).getMethodInfo2().getDescriptor().startsWith(desc))
				return (CtMethod) mth;
		}

		throw new NotFoundException(name + "(..) is not found in " + this.getName());
	}

	@Override
	public CtMethod[] getDeclaredMethods()
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember mth = memCache.methodHead();
		CtMember mthTail = memCache.lastMethod();
		int num = CtMember.Cache.count(mth, mthTail);
		CtMethod[] cms = new CtMethod[num];
		int i = 0;
		while (mth != mthTail)
		{
			mth = mth.next();
			cms[i++] = (CtMethod) mth;
		}

		return cms;
	}

	@Override
	public CtMethod[] getDeclaredMethods(String name) throws NotFoundException
	{
		CtMember.Cache memCache = this.getMembers();
		CtMember mth = memCache.methodHead();
		CtMember mthTail = memCache.lastMethod();
		ArrayList<CtMethod> methods = new ArrayList<CtMethod>();
		while (mth != mthTail)
		{
			mth = mth.next();
			if (mth.getName().equals(name))
				methods.add((CtMethod) mth);
		}

		return methods.toArray(new CtMethod[methods.size()]);
	}

	@Override
	public CtClass getDeclaringClass() throws NotFoundException
	{
		ClassFile cf = this.getClassFile2();
		InnerClassesAttribute ica = (InnerClassesAttribute) cf.getAttribute(InnerClassesAttribute.tag);
		if (ica == null)
			return null;

		String name = this.getName();
		int n = ica.tableLength();
		for (int i = 0; i < n; ++i)
			if (name.equals(ica.innerClass(i)))
			{
				String outName = ica.outerClass(i);
				if (outName != null)
					return this.classPool.get(outName);
				else
				{
					// maybe anonymous or local class.
					EnclosingMethodAttribute ema = (EnclosingMethodAttribute) cf.getAttribute(EnclosingMethodAttribute.tag);
					if (ema != null)
						return this.classPool.get(ema.className());
				}
			}

		return null;
	}

	@Override
	public CtBehavior getEnclosingBehavior() throws NotFoundException
	{
		ClassFile cf = this.getClassFile2();
		EnclosingMethodAttribute ema = (EnclosingMethodAttribute) cf.getAttribute(EnclosingMethodAttribute.tag);
		if (ema == null)
			return null;
		else
		{
			CtClass enc = this.classPool.get(ema.className());
			String name = ema.methodName();
			if (MethodInfo.nameInit.equals(name))
				return enc.getConstructor(ema.methodDescriptor());
			else if (MethodInfo.nameClinit.equals(name))
				return enc.getClassInitializer();
			else
				return enc.getMethod(name, ema.methodDescriptor());
		}
	}

	@Override
	public CtField getField(String name, String desc) throws NotFoundException
	{
		CtField f = this.getField2(name, desc);
		return this.checkGetField(f, name, desc);
	}

	@Override
	CtField getField2(String name, String desc)
	{
		CtField df = this.getDeclaredField2(name, desc);
		if (df != null)
			return df;

		try
		{
			CtClass[] ifs = this.getInterfaces();
			int num = ifs.length;
			for (int i = 0; i < num; ++i)
			{
				CtField f = ifs[i].getField2(name, desc);
				if (f != null)
					return f;
			}

			CtClass s = this.getSuperclass();
			if (s != null)
				return s.getField2(name, desc);
		} catch (NotFoundException e)
		{
		}
		return null;
	}

	@Override
	public CtField[] getFields()
	{
		ArrayList alist = new ArrayList();
		CtClassType.getFields(alist, this);
		return (CtField[]) alist.toArray(new CtField[alist.size()]);
	}

	@Override
	public String getGenericSignature()
	{
		SignatureAttribute sa = (SignatureAttribute) this.getClassFile2().getAttribute(SignatureAttribute.tag);
		return sa == null ? null : sa.getSignature();
	}

	Hashtable getHiddenMethods()
	{
		if (this.hiddenMethods == null)
			this.hiddenMethods = new Hashtable();

		return this.hiddenMethods;
	}

	@Override
	public CtClass[] getInterfaces() throws NotFoundException
	{
		String[] ifs = this.getClassFile2().getInterfaces();
		int num = ifs.length;
		CtClass[] ifc = new CtClass[num];
		for (int i = 0; i < num; ++i)
			ifc[i] = this.classPool.get(ifs[i]);

		return ifc;
	}

	protected synchronized CtMember.Cache getMembers()
	{
		CtMember.Cache cache = null;
		if (this.memberCache == null || (cache = (CtMember.Cache) this.memberCache.get()) == null)
		{
			cache = new CtMember.Cache(this);
			this.makeFieldCache(cache);
			this.makeBehaviorCache(cache);
			this.memberCache = new WeakReference(cache);
		}

		return cache;
	}

	@Override
	public CtMethod getMethod(String name, String desc) throws NotFoundException
	{
		CtMethod m = CtClassType.getMethod0(this, name, desc);
		if (m != null)
			return m;
		else
			throw new NotFoundException(name + "(..) is not found in " + this.getName());
	}

	@Override
	public CtMethod[] getMethods()
	{
		HashMap h = new HashMap();
		CtClassType.getMethods0(h, this);
		return (CtMethod[]) h.values().toArray(new CtMethod[h.size()]);
	}

	@Override
	public int getModifiers()
	{
		ClassFile cf = this.getClassFile2();
		int acc = cf.getAccessFlags();
		acc = AccessFlag.clear(acc, AccessFlag.SUPER);
		int inner = cf.getInnerAccessFlags();
		if (inner != -1 && (inner & AccessFlag.STATIC) != 0)
			acc |= AccessFlag.STATIC;

		return AccessFlag.toModifier(acc);
	}

	@Override
	public CtClass[] getNestedClasses() throws NotFoundException
	{
		ClassFile cf = this.getClassFile2();
		InnerClassesAttribute ica = (InnerClassesAttribute) cf.getAttribute(InnerClassesAttribute.tag);
		if (ica == null)
			return new CtClass[0];

		String thisName = cf.getName() + "$";
		int n = ica.tableLength();
		ArrayList list = new ArrayList(n);
		for (int i = 0; i < n; i++)
		{
			String name = ica.innerClass(i);
			if (name != null)
				if (name.startsWith(thisName))
					// if it is an immediate nested class
					if (name.lastIndexOf('$') < thisName.length())
						list.add(this.classPool.get(name));
		}

		return (CtClass[]) list.toArray(new CtClass[list.size()]);
	}

	@Override
	public CtClass getSuperclass() throws NotFoundException
	{
		String supername = this.getClassFile2().getSuperclass();
		if (supername == null)
			return null;
		else
			return this.classPool.get(supername);
	}

	int getUniqueNumber()
	{
		return this.uniqueNumberSeed++;
	}

	@Override
	public URL getURL() throws NotFoundException
	{
		URL url = this.classPool.find(this.getName());
		if (url == null)
			throw new NotFoundException(this.getName());
		else
			return url;
	}

	@Override
	public boolean hasAnnotation(Class clz)
	{
		ClassFile cf = this.getClassFile2();
		AnnotationsAttribute ainfo = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.invisibleTag);
		AnnotationsAttribute ainfo2 = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.visibleTag);
		return CtClassType.hasAnnotationType(clz, this.getClassPool(), ainfo, ainfo2);
	}

	/**
	 * Returns null if members are not cached.
	 */
	protected CtMember.Cache hasMemberCache()
	{
		if (this.memberCache != null)
			return (CtMember.Cache) this.memberCache.get();
		else
			return null;
	}

	/*
	 * Inherited from CtClass. Called by get() in ClassPool.
	 * @see javassist.CtClass#incGetCounter()
	 * @see #toBytecode(DataOutputStream)
	 */
	@Override
	final void incGetCounter()
	{
		++this.getCount;
	}

	@Override
	public void instrument(CodeConverter converter) throws CannotCompileException
	{
		this.checkModify();
		ClassFile cf = this.getClassFile2();
		ConstPool cp = cf.getConstPool();
		List list = cf.getMethods();
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			converter.doit(this, minfo, cp);
		}
	}

	@Override
	public void instrument(ExprEditor editor) throws CannotCompileException
	{
		this.checkModify();
		ClassFile cf = this.getClassFile2();
		List list = cf.getMethods();
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			editor.doit(this, minfo);
		}
	}

	@Override
	public boolean isAnnotation()
	{
		return Modifier.isAnnotation(this.getModifiers());
	}

	@Override
	public boolean isEnum()
	{
		return Modifier.isEnum(this.getModifiers());
	}

	@Override
	public boolean isFrozen()
	{
		return this.wasFrozen;
	}

	@Override
	public boolean isInterface()
	{
		return Modifier.isInterface(this.getModifiers());
	}

	@Override
	public boolean isModified()
	{
		return this.wasChanged;
	}

	private void makeBehaviorCache(CtMember.Cache cache)
	{
		List list = this.getClassFile2().getMethods();
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			if (minfo.isMethod())
			{
				CtMethod newMethod = new CtMethod(minfo, this);
				cache.addMethod(newMethod);
			} else
			{
				CtConstructor newCons = new CtConstructor(minfo, this);
				cache.addConstructor(newCons);
			}
		}
	}

	@Override
	public CtConstructor makeClassInitializer() throws CannotCompileException
	{
		CtConstructor clinit = this.getClassInitializer();
		if (clinit != null)
			return clinit;

		this.checkModify();
		ClassFile cf = this.getClassFile2();
		Bytecode code = new Bytecode(cf.getConstPool(), 0, 0);
		this.modifyClassConstructor(cf, code, 0, 0);
		return this.getClassInitializer();
	}

	private void makeFieldCache(CtMember.Cache cache)
	{
		List list = this.getClassFile2().getFields();
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			CtField newField = new CtField(finfo, this);
			cache.addField(newField);
		}
	}

	private int makeFieldInitializer(Bytecode code, CtClass[] parameters) throws CannotCompileException, NotFoundException
	{
		int stacksize = 0;
		Javac jv = new Javac(code, this);
		try
		{
			jv.recordParams(parameters, false);
		} catch (CompileError e)
		{
			throw new CannotCompileException(e);
		}

		for (FieldInitLink fi = this.fieldInitializers; fi != null; fi = fi.next)
		{
			CtField f = fi.field;
			if (!Modifier.isStatic(f.getModifiers()))
			{
				int s = fi.init.compile(f.getType(), f.getName(), code, parameters, jv);
				if (stacksize < s)
					stacksize = s;
			}
		}

		return stacksize;
	}

	private void makeMemberList(HashMap table)
	{
		int mod = this.getModifiers();
		if (Modifier.isAbstract(mod) || Modifier.isInterface(mod))
			try
			{
				CtClass[] ifs = this.getInterfaces();
				int size = ifs.length;
				for (int i = 0; i < size; i++)
				{
					CtClass ic = ifs[i];
					if (ic != null && ic instanceof CtClassType)
						((CtClassType) ic).makeMemberList(table);
				}
			} catch (NotFoundException e)
			{
			}

		try
		{
			CtClass s = this.getSuperclass();
			if (s != null && s instanceof CtClassType)
				((CtClassType) s).makeMemberList(table);
		} catch (NotFoundException e)
		{
		}

		List list = this.getClassFile2().getMethods();
		int n = list.size();
		for (int i = 0; i < n; i++)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			table.put(minfo.getName(), this);
		}

		list = this.getClassFile2().getFields();
		n = list.size();
		for (int i = 0; i < n; i++)
		{
			FieldInfo finfo = (FieldInfo) list.get(i);
			table.put(finfo.getName(), this);
		}
	}

	@Override
	public CtClass makeNestedClass(String name, boolean isStatic)
	{
		if (!isStatic)
			throw new RuntimeException("sorry, only nested static class is supported");

		this.checkModify();
		CtClass c = this.classPool.makeNestedClass(this.getName() + "$" + name);
		ClassFile cf = this.getClassFile2();
		ClassFile cf2 = c.getClassFile2();
		InnerClassesAttribute ica = (InnerClassesAttribute) cf.getAttribute(InnerClassesAttribute.tag);
		if (ica == null)
		{
			ica = new InnerClassesAttribute(cf.getConstPool());
			cf.addAttribute(ica);
		}

		ica.append(c.getName(), this.getName(), name, cf2.getAccessFlags() & ~AccessFlag.SUPER | AccessFlag.STATIC);
		cf2.addAttribute(ica.copy(cf2.getConstPool(), null));
		return c;
	}

	@Override
	public String makeUniqueName(String prefix)
	{
		HashMap table = new HashMap();
		this.makeMemberList(table);
		Set keys = table.keySet();
		String[] methods = new String[keys.size()];
		keys.toArray(methods);

		if (CtClassType.notFindInArray(prefix, methods))
			return prefix;

		int i = 100;
		String name;
		do
		{
			if (i > 999)
				throw new RuntimeException("too many unique name");

			name = prefix + i++;
		} while (!CtClassType.notFindInArray(name, methods));
		return name;
	}

	private void modifyClassConstructor(ClassFile cf) throws CannotCompileException, NotFoundException
	{
		if (this.fieldInitializers == null)
			return;

		Bytecode code = new Bytecode(cf.getConstPool(), 0, 0);
		Javac jv = new Javac(code, this);
		int stacksize = 0;
		boolean doInit = false;
		for (FieldInitLink fi = this.fieldInitializers; fi != null; fi = fi.next)
		{
			CtField f = fi.field;
			if (Modifier.isStatic(f.getModifiers()))
			{
				doInit = true;
				int s = fi.init.compileIfStatic(f.getType(), f.getName(), code, jv);
				if (stacksize < s)
					stacksize = s;
			}
		}

		if (doInit)   // need an initializer for static fileds.
			this.modifyClassConstructor(cf, code, stacksize, 0);
	}

	private void modifyClassConstructor(ClassFile cf, Bytecode code, int stacksize, int localsize) throws CannotCompileException
	{
		MethodInfo m = cf.getStaticInitializer();
		if (m == null)
		{
			code.add(Opcode.RETURN);
			code.setMaxStack(stacksize);
			code.setMaxLocals(localsize);
			m = new MethodInfo(cf.getConstPool(), "<clinit>", "()V");
			m.setAccessFlags(AccessFlag.STATIC);
			m.setCodeAttribute(code.toCodeAttribute());
			cf.addMethod(m);
			CtMember.Cache cache = this.hasMemberCache();
			if (cache != null)
				cache.addConstructor(new CtConstructor(m, this));
		} else
		{
			CodeAttribute codeAttr = m.getCodeAttribute();
			if (codeAttr == null)
				throw new CannotCompileException("empty <clinit>");

			try
			{
				CodeIterator it = codeAttr.iterator();
				int pos = it.insertEx(code.get());
				it.insert(code.getExceptionTable(), pos);
				int maxstack = codeAttr.getMaxStack();
				if (maxstack < stacksize)
					codeAttr.setMaxStack(stacksize);

				int maxlocals = codeAttr.getMaxLocals();
				if (maxlocals < localsize)
					codeAttr.setMaxLocals(localsize);
			} catch (BadBytecode e)
			{
				throw new CannotCompileException(e);
			}
		}

		try
		{
			m.rebuildStackMapIf6(this.classPool, cf);
		} catch (BadBytecode e)
		{
			throw new CannotCompileException(e);
		}
	}

	private void modifyConstructors(ClassFile cf) throws CannotCompileException, NotFoundException
	{
		if (this.fieldInitializers == null)
			return;

		ConstPool cp = cf.getConstPool();
		List list = cf.getMethods();
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			MethodInfo minfo = (MethodInfo) list.get(i);
			if (minfo.isConstructor())
			{
				CodeAttribute codeAttr = minfo.getCodeAttribute();
				if (codeAttr != null)
					try
					{
						Bytecode init = new Bytecode(cp, 0, codeAttr.getMaxLocals());
						CtClass[] params = Descriptor.getParameterTypes(minfo.getDescriptor(), this.classPool);
						int stacksize = this.makeFieldInitializer(init, params);
						CtClassType.insertAuxInitializer(codeAttr, init, stacksize);
						minfo.rebuildStackMapIf6(this.classPool, cf);
					} catch (BadBytecode e)
					{
						throw new CannotCompileException(e);
					}
			}
		}
	}

	/*
	 * flush cached names.
	 */
	private void nameReplaced()
	{
		CtMember.Cache cache = this.hasMemberCache();
		if (cache != null)
		{
			CtMember mth = cache.methodHead();
			CtMember tail = cache.lastMethod();
			while (mth != tail)
			{
				mth = mth.next();
				mth.nameReplaced();
			}
		}
	}

	/**
	 * @see javassist.CtClass#prune()
	 * @see javassist.CtClass#stopPruning(boolean)
	 */
	@Override
	public void prune()
	{
		if (this.wasPruned)
			return;

		this.wasPruned = this.wasFrozen = true;
		this.getClassFile2().prune();
	}

	@Override
	public void rebuildClassFile()
	{
		this.gcConstPool = true;
	}

	private synchronized void removeClassFile()
	{
		if (this.classfile != null && !this.isModified() && this.hasMemberCache() == null)
			this.classfile = null;
	}

	@Override
	public void removeConstructor(CtConstructor m) throws NotFoundException
	{
		this.checkModify();
		MethodInfo mi = m.getMethodInfo2();
		ClassFile cf = this.getClassFile2();
		if (cf.getMethods().remove(mi))
		{
			this.getMembers().remove(m);
			this.gcConstPool = true;
		} else
			throw new NotFoundException(m.toString());
	}

	@Override
	public void removeField(CtField f) throws NotFoundException
	{
		this.checkModify();
		FieldInfo fi = f.getFieldInfo2();
		ClassFile cf = this.getClassFile2();
		if (cf.getFields().remove(fi))
		{
			this.getMembers().remove(f);
			this.gcConstPool = true;
		} else
			throw new NotFoundException(f.toString());
	}

	@Override
	public void removeMethod(CtMethod m) throws NotFoundException
	{
		this.checkModify();
		MethodInfo mi = m.getMethodInfo2();
		ClassFile cf = this.getClassFile2();
		if (cf.getMethods().remove(mi))
		{
			this.getMembers().remove(m);
			this.gcConstPool = true;
		} else
			throw new NotFoundException(m.toString());
	}

	@Override
	public void replaceClassName(ClassMap classnames) throws RuntimeException
	{
		String oldClassName = this.getName();
		String newClassName = (String) classnames.get(Descriptor.toJvmName(oldClassName));
		if (newClassName != null)
		{
			newClassName = Descriptor.toJavaName(newClassName);
			// check this in advance although classNameChanged() below does.
			this.classPool.checkNotFrozen(newClassName);
		}

		super.replaceClassName(classnames);
		ClassFile cf = this.getClassFile2();
		cf.renameClass(classnames);
		this.nameReplaced();

		if (newClassName != null)
		{
			super.setName(newClassName);
			this.classPool.classNameChanged(oldClassName, this);
		}
	}

	@Override
	public void replaceClassName(String oldname, String newname) throws RuntimeException
	{
		String thisname = this.getName();
		if (thisname.equals(oldname))
			this.setName(newname);
		else
		{
			super.replaceClassName(oldname, newname);
			this.getClassFile2().renameClass(oldname, newname);
			this.nameReplaced();
		}
	}

	/**
	 * Converts a ClassFile object into a byte array for saving memory space.
	 */
	private synchronized void saveClassFile()
	{
		/*
		 * getMembers() and releaseClassFile() are also synchronized.
		 */
		if (this.classfile == null || this.hasMemberCache() != null)
			return;

		ByteArrayOutputStream barray = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(barray);
		try
		{
			this.classfile.write(out);
			barray.close();
			this.rawClassfile = barray.toByteArray();
			this.classfile = null;
		} catch (IOException e)
		{
		}
	}

	@Override
	public void setAttribute(String name, byte[] data)
	{
		this.checkModify();
		ClassFile cf = this.getClassFile2();
		cf.addAttribute(new AttributeInfo(cf.getConstPool(), name, data));
	}

	void setClassPool(ClassPool cp)
	{
		this.classPool = cp;
	}

	@Override
	public void setGenericSignature(String sig)
	{
		ClassFile cf = this.getClassFile();
		SignatureAttribute sa = new SignatureAttribute(cf.getConstPool(), sig);
		cf.addAttribute(sa);
	}

	@Override
	public void setInterfaces(CtClass[] list)
	{
		this.checkModify();
		String[] ifs;
		if (list == null)
			ifs = new String[0];
		else
		{
			int num = list.length;
			ifs = new String[num];
			for (int i = 0; i < num; ++i)
				ifs[i] = list[i].getName();
		}

		this.getClassFile2().setInterfaces(ifs);
	}

	@Override
	public void setModifiers(int mod)
	{
		ClassFile cf = this.getClassFile2();
		if (Modifier.isStatic(mod))
		{
			int flags = cf.getInnerAccessFlags();
			if (flags != -1 && (flags & AccessFlag.STATIC) != 0)
				mod = mod & ~Modifier.STATIC;
			else
				throw new RuntimeException("cannot change " + this.getName() + " into a static class");
		}

		this.checkModify();
		cf.setAccessFlags(AccessFlag.of(mod));
	}

	@Override
	public void setName(String name) throws RuntimeException
	{
		String oldname = this.getName();
		if (name.equals(oldname))
			return;

		// check this in advance although classNameChanged() below does.
		this.classPool.checkNotFrozen(name);
		ClassFile cf = this.getClassFile2();
		super.setName(name);
		cf.setName(name);
		this.nameReplaced();
		this.classPool.classNameChanged(oldname, this);
	}

	// Methods used by CtNewWrappedMethod

	@Override
	public void setSuperclass(CtClass clazz) throws CannotCompileException
	{
		this.checkModify();
		if (this.isInterface())
			this.addInterface(clazz);
		else
			this.getClassFile2().setSuperclass(clazz.getName());
	}

	@Override
	public boolean stopPruning(boolean stop)
	{
		boolean prev = !this.doPruning;
		this.doPruning = !stop;
		return prev;
	}

	@Override
	public boolean subclassOf(CtClass superclass)
	{
		if (superclass == null)
			return false;

		String superName = superclass.getName();
		CtClass curr = this;
		try
		{
			while (curr != null)
			{
				if (curr.getName().equals(superName))
					return true;

				curr = curr.getSuperclass();
			}
		} catch (Exception ignored)
		{
		}
		return false;
	}

	@Override
	public boolean subtypeOf(CtClass clazz) throws NotFoundException
	{
		int i;
		String cname = clazz.getName();
		if (this == clazz || this.getName().equals(cname))
			return true;

		ClassFile file = this.getClassFile2();
		String supername = file.getSuperclass();
		if (supername != null && supername.equals(cname))
			return true;

		String[] ifs = file.getInterfaces();
		int num = ifs.length;
		for (i = 0; i < num; ++i)
			if (ifs[i].equals(cname))
				return true;

		if (supername != null && this.classPool.get(supername).subtypeOf(clazz))
			return true;

		for (i = 0; i < num; ++i)
			if (this.classPool.get(ifs[i]).subtypeOf(clazz))
				return true;

		return false;
	}

	@Override
	public void toBytecode(DataOutputStream out) throws CannotCompileException, IOException
	{
		try
		{
			if (this.isModified())
			{
				this.checkPruned("toBytecode");
				ClassFile cf = this.getClassFile2();
				if (this.gcConstPool)
				{
					cf.compact();
					this.gcConstPool = false;
				}

				this.modifyClassConstructor(cf);
				this.modifyConstructors(cf);
				if (CtClass.debugDump != null)
					this.dumpClassFile(cf);

				cf.write(out);
				out.flush();
				this.fieldInitializers = null;
				if (this.doPruning)
				{
					// to save memory
					cf.prune();
					this.wasPruned = true;
				}
			} else
				this.classPool.writeClassfile(this.getName(), out);
			// to save memory
			// classfile = null;

			this.getCount = 0;
			this.wasFrozen = true;
		} catch (NotFoundException e)
		{
			throw new CannotCompileException(e);
		} catch (IOException e)
		{
			throw new CannotCompileException(e);
		}
	}
}

class FieldInitLink
{
	FieldInitLink		next;
	CtField				field;
	CtField.Initializer	init;

	FieldInitLink(CtField f, CtField.Initializer i)
	{
		this.next = null;
		this.field = f;
		this.init = i;
	}
}
