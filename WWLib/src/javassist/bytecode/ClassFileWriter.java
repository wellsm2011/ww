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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A quick class-file writer. This is useful when a generated class file is
 * simple and the code generation should be fast.
 * <p>
 * Example: <blockquote>
 *
 * <pre>
 * ClassFileWriter cfw = new ClassFileWriter(ClassFile.JAVA_4, 0);
 * ConstPoolWriter cpw = cfw.getConstPool();
 * 
 * FieldWriter fw = cfw.getFieldWriter();
 * fw.add(AccessFlag.PUBLIC, &quot;value&quot;, &quot;I&quot;, null);
 * fw.add(AccessFlag.PUBLIC, &quot;value2&quot;, &quot;J&quot;, null);
 * 
 * int thisClass = cpw.addClassInfo(&quot;sample/Test&quot;);
 * int superClass = cpw.addClassInfo(&quot;java/lang/Object&quot;);
 * 
 * MethodWriter mw = cfw.getMethodWriter();
 * 
 * mw.begin(AccessFlag.PUBLIC, MethodInfo.nameInit, &quot;()V&quot;, null, null);
 * mw.add(Opcode.ALOAD_0);
 * mw.add(Opcode.INVOKESPECIAL);
 * int signature = cpw.addNameAndTypeInfo(MethodInfo.nameInit, &quot;()V&quot;);
 * mw.add16(cpw.addMethodrefInfo(superClass, signature));
 * mw.add(Opcode.RETURN);
 * mw.codeEnd(1, 1);
 * mw.end(null, null);
 * 
 * mw.begin(AccessFlag.PUBLIC, &quot;one&quot;, &quot;()I&quot;, null, null);
 * mw.add(Opcode.ICONST_1);
 * mw.add(Opcode.IRETURN);
 * mw.codeEnd(1, 1);
 * mw.end(null, null);
 * 
 * byte[] classfile = cfw.end(AccessFlag.PUBLIC, thisClass, superClass, null, null);
 * </pre>
 *
 * </blockquote>
 * <p>
 * The code above generates the following class: <blockquote>
 *
 * <pre>
 * package sample;
 * 
 * public class Test
 * {
 * 	public int value;
 * 	public long value2;
 * 
 * 	public Test()
 * 	{
 * 		super();
 * 	}
 * 
 * 	public one()
 * 	{
 * 		return 1;
 * 	}
 * }
 * </pre>
 *
 * </blockquote>
 *
 * @since 3.13
 */
public class ClassFileWriter
{
	/**
	 * This writes attributes.
	 * <p>
	 * For example, the following object writes a synthetic attribute:
	 *
	 * <pre>
	 * ConstPoolWriter cpw = ...;
	 * final int tag = cpw.addUtf8Info("Synthetic");
	 * AttributeWriter aw = new AttributeWriter() {
	 *     public int size() {
	 *         return 1;
	 *     }
	 *     public void write(DataOutputStream out) throws java.io.IOException {
	 *         out.writeShort(tag);
	 *         out.writeInt(0);
	 *     }
	 * };
	 * </pre>
	 */
	public static interface AttributeWriter
	{
		/**
		 * Returns the number of attributes that this writer will write.
		 */
		public int size();

		/**
		 * Writes all the contents of the attributes. The binary representation
		 * of the contents is an array of <code>attribute_info</code>.
		 */
		public void write(DataOutputStream out) throws IOException;
	}

	/**
	 * Constant Pool.
	 */
	public static final class ConstPoolWriter
	{
		ByteStream		output;
		protected int	startPos;
		protected int	num;

		ConstPoolWriter(ByteStream out)
		{
			this.output = out;
			this.startPos = out.getPos();
			this.num = 1;
			this.output.writeShort(1); // number of entries
		}

		/**
		 * Adds a new <code>CONSTANT_Class_info</code> structure.
		 *
		 * @param name
		 *            <code>name_index</code>
		 * @return the index of the added entry.
		 */
		public int addClassInfo(int name)
		{
			this.output.write(ClassInfo.tag);
			this.output.writeShort(name);
			return this.num++;
		}

		/**
		 * Adds a new <code>CONSTANT_Class_info</code> structure.
		 * <p>
		 * This also adds a <code>CONSTANT_Utf8_info</code> structure for
		 * storing the class name.
		 *
		 * @param jvmname
		 *            the JVM-internal representation of a class name. e.g.
		 *            <code>java/lang/Object</code>.
		 * @return the index of the added entry.
		 */
		public int addClassInfo(String jvmname)
		{
			int utf8 = this.addUtf8Info(jvmname);
			this.output.write(ClassInfo.tag);
			this.output.writeShort(utf8);
			return this.num++;
		}

		/**
		 * Makes <code>CONSTANT_Class_info</code> objects for each class name.
		 *
		 * @return an array of indexes indicating
		 *         <code>CONSTANT_Class_info</code>s.
		 */
		public int[] addClassInfo(String[] classNames)
		{
			int n = classNames.length;
			int[] result = new int[n];
			for (int i = 0; i < n; i++)
				result[i] = this.addClassInfo(classNames[i]);

			return result;
		}

		/**
		 * Adds a new <code>CONSTANT_Double_info</code> structure.
		 *
		 * @return the index of the added entry.
		 */
		public int addDoubleInfo(double d)
		{
			this.output.write(DoubleInfo.tag);
			this.output.writeDouble(d);
			int n = this.num;
			this.num += 2;
			return n;
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
			this.output.write(FieldrefInfo.tag);
			this.output.writeShort(classInfo);
			this.output.writeShort(nameAndTypeInfo);
			return this.num++;
		}

		/**
		 * Adds a new <code>CONSTANT_Float_info</code> structure.
		 *
		 * @return the index of the added entry.
		 */
		public int addFloatInfo(float f)
		{
			this.output.write(FloatInfo.tag);
			this.output.writeFloat(f);
			return this.num++;
		}

		/**
		 * Adds a new <code>CONSTANT_Integer_info</code> structure.
		 *
		 * @return the index of the added entry.
		 */
		public int addIntegerInfo(int i)
		{
			this.output.write(IntegerInfo.tag);
			this.output.writeInt(i);
			return this.num++;
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
			this.output.write(InterfaceMethodrefInfo.tag);
			this.output.writeShort(classInfo);
			this.output.writeShort(nameAndTypeInfo);
			return this.num++;
		}

		/**
		 * Adds a new <code>CONSTANT_InvokeDynamic_info</code> structure.
		 *
		 * @param bootstrap
		 *            <code>bootstrap_method_attr_index</code>.
		 * @param nameAndTypeInfo
		 *            <code>name_and_type_index</code>.
		 * @return the index of the added entry.
		 * @since 3.17.1
		 */
		public int addInvokeDynamicInfo(int bootstrap, int nameAndTypeInfo)
		{
			this.output.write(InvokeDynamicInfo.tag);
			this.output.writeShort(bootstrap);
			this.output.writeShort(nameAndTypeInfo);
			return this.num++;
		}

		/**
		 * Adds a new <code>CONSTANT_Long_info</code> structure.
		 *
		 * @return the index of the added entry.
		 */
		public int addLongInfo(long l)
		{
			this.output.write(LongInfo.tag);
			this.output.writeLong(l);
			int n = this.num;
			this.num += 2;
			return n;
		}

		/**
		 * Adds a new <code>CONSTANT_MethodHandle_info</code> structure.
		 *
		 * @param kind
		 *            <code>reference_kind</code> such as
		 *            {@link ConstPool#REF_invokeStatic
		 *            <code>REF_invokeStatic</code>}.
		 * @param index
		 *            <code>reference_index</code>.
		 * @return the index of the added entry.
		 * @since 3.17.1
		 */
		public int addMethodHandleInfo(int kind, int index)
		{
			this.output.write(MethodHandleInfo.tag);
			this.output.write(kind);
			this.output.writeShort(index);
			return this.num++;
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
			this.output.write(MethodrefInfo.tag);
			this.output.writeShort(classInfo);
			this.output.writeShort(nameAndTypeInfo);
			return this.num++;
		}

		/**
		 * Adds a new <code>CONSTANT_MethodType_info</code> structure.
		 *
		 * @param desc
		 *            <code>descriptor_index</code>.
		 * @return the index of the added entry.
		 * @since 3.17.1
		 */
		public int addMethodTypeInfo(int desc)
		{
			this.output.write(MethodTypeInfo.tag);
			this.output.writeShort(desc);
			return this.num++;
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
			this.output.write(NameAndTypeInfo.tag);
			this.output.writeShort(name);
			this.output.writeShort(type);
			return this.num++;
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
			int utf8 = this.addUtf8Info(str);
			this.output.write(StringInfo.tag);
			this.output.writeShort(utf8);
			return this.num++;
		}

		/**
		 * Adds a new <code>CONSTANT_Utf8_info</code> structure.
		 *
		 * @return the index of the added entry.
		 */
		public int addUtf8Info(String utf8)
		{
			this.output.write(Utf8Info.tag);
			this.output.writeUTF(utf8);
			return this.num++;
		}

		/**
		 * Writes the contents of this class pool.
		 */
		void end()
		{
			this.output.writeShort(this.startPos, this.num);
		}
	}

	/**
	 * Field.
	 */
	public static final class FieldWriter
	{
		protected ByteStream		output;
		protected ConstPoolWriter	constPool;
		private int					fieldCount;

		FieldWriter(ConstPoolWriter cp)
		{
			this.output = new ByteStream(128);
			this.constPool = cp;
			this.fieldCount = 0;
		}

		/**
		 * Adds a new field.
		 *
		 * @param accessFlags
		 *            access flags.
		 * @param name
		 *            the field name. an index indicating its
		 *            <code>CONSTANT_Utf8_info</code>.
		 * @param descriptor
		 *            the field type. an index indicating its
		 *            <code>CONSTANT_Utf8_info</code>.
		 * @param aw
		 *            the attributes of the field. may be null.
		 * @see AccessFlag
		 */
		public void add(int accessFlags, int name, int descriptor, AttributeWriter aw)
		{
			++this.fieldCount;
			this.output.writeShort(accessFlags);
			this.output.writeShort(name);
			this.output.writeShort(descriptor);
			ClassFileWriter.writeAttribute(this.output, aw, 0);
		}

		/**
		 * Adds a new field.
		 *
		 * @param accessFlags
		 *            access flags.
		 * @param name
		 *            the field name.
		 * @param descriptor
		 *            the field type.
		 * @param aw
		 *            the attributes of the field. may be null.
		 * @see AccessFlag
		 */
		public void add(int accessFlags, String name, String descriptor, AttributeWriter aw)
		{
			int nameIndex = this.constPool.addUtf8Info(name);
			int descIndex = this.constPool.addUtf8Info(descriptor);
			this.add(accessFlags, nameIndex, descIndex, aw);
		}

		int dataSize()
		{
			return this.output.size();
		}

		int size()
		{
			return this.fieldCount;
		}

		/**
		 * Writes the added fields.
		 */
		void write(OutputStream out) throws IOException
		{
			this.output.writeTo(out);
		}
	}

	/**
	 * Method.
	 */
	public static final class MethodWriter
	{
		protected ByteStream		output;
		protected ConstPoolWriter	constPool;
		private int					methodCount;
		protected int				codeIndex;
		protected int				throwsIndex;
		protected int				stackIndex;

		private int		startPos;
		private boolean	isAbstract;
		private int		catchPos;
		private int		catchCount;

		MethodWriter(ConstPoolWriter cp)
		{
			this.output = new ByteStream(256);
			this.constPool = cp;
			this.methodCount = 0;
			this.codeIndex = 0;
			this.throwsIndex = 0;
			this.stackIndex = 0;
		}

		/**
		 * Appends an 8bit value of bytecode.
		 *
		 * @see Opcode
		 */
		public void add(int b)
		{
			this.output.write(b);
		}

		/**
		 * Appends a 16bit value of bytecode.
		 */
		public void add16(int b)
		{
			this.output.writeShort(b);
		}

		/**
		 * Appends a 32bit value of bytecode.
		 */
		public void add32(int b)
		{
			this.output.writeInt(b);
		}

		/**
		 * Appends an <code>exception_table</code> entry to the
		 * <code>Code_attribute</code>. This method is available only after the
		 * <code>codeEnd</code> method is called.
		 *
		 * @param catchType
		 *            an index indicating a <code>CONSTANT_Class_info</code>.
		 */
		public void addCatch(int startPc, int endPc, int handlerPc, int catchType)
		{
			++this.catchCount;
			this.output.writeShort(startPc);
			this.output.writeShort(endPc);
			this.output.writeShort(handlerPc);
			this.output.writeShort(catchType);
		}

		/**
		 * Appends a invokevirtual, inovkespecial, or invokestatic bytecode.
		 *
		 * @see Opcode
		 */
		public void addInvoke(int opcode, String targetClass, String methodName, String descriptor)
		{
			int target = this.constPool.addClassInfo(targetClass);
			int nt = this.constPool.addNameAndTypeInfo(methodName, descriptor);
			int method = this.constPool.addMethodrefInfo(target, nt);
			this.add(opcode);
			this.add16(method);
		}

		/**
		 * Starts adding a new method.
		 *
		 * @param accessFlags
		 *            access flags.
		 * @param name
		 *            the method name. an index indicating its
		 *            <code>CONSTANT_Utf8_info</code>.
		 * @param descriptor
		 *            the field type. an index indicating its
		 *            <code>CONSTANT_Utf8_info</code>.
		 * @param exceptions
		 *            throws clause. indexes indicating
		 *            <code>CONSTANT_Class_info</code>s. It may be null.
		 * @param aw
		 *            attributes to the <code>Method_info</code>.
		 */
		public void begin(int accessFlags, int name, int descriptor, int[] exceptions, AttributeWriter aw)
		{
			++this.methodCount;
			this.output.writeShort(accessFlags);
			this.output.writeShort(name);
			this.output.writeShort(descriptor);
			this.isAbstract = (accessFlags & AccessFlag.ABSTRACT) != 0;

			int attrCount = this.isAbstract ? 0 : 1;
			if (exceptions != null)
				++attrCount;

			ClassFileWriter.writeAttribute(this.output, aw, attrCount);

			if (exceptions != null)
				this.writeThrows(exceptions);

			if (!this.isAbstract)
			{
				if (this.codeIndex == 0)
					this.codeIndex = this.constPool.addUtf8Info(CodeAttribute.tag);

				this.startPos = this.output.getPos();
				this.output.writeShort(this.codeIndex);
				this.output.writeBlank(12); // attribute_length, maxStack,
				// maxLocals, code_lenth
			}

			this.catchPos = -1;
			this.catchCount = 0;
		}

		/**
		 * Starts Adding a new method.
		 *
		 * @param accessFlags
		 *            access flags.
		 * @param name
		 *            the method name.
		 * @param descriptor
		 *            the method signature.
		 * @param exceptions
		 *            throws clause. It may be null. The class names must be the
		 *            JVM-internal representations like
		 *            <code>java/lang/Exception</code>.
		 * @param aw
		 *            attributes to the <code>Method_info</code>.
		 */
		public void begin(int accessFlags, String name, String descriptor, String[] exceptions, AttributeWriter aw)
		{
			int nameIndex = this.constPool.addUtf8Info(name);
			int descIndex = this.constPool.addUtf8Info(descriptor);
			int[] intfs;
			if (exceptions == null)
				intfs = null;
			else
				intfs = this.constPool.addClassInfo(exceptions);

			this.begin(accessFlags, nameIndex, descIndex, intfs, aw);
		}

		/**
		 * Ends appending bytecode.
		 */
		public void codeEnd(int maxStack, int maxLocals)
		{
			if (!this.isAbstract)
			{
				this.output.writeShort(this.startPos + 6, maxStack);
				this.output.writeShort(this.startPos + 8, maxLocals);
				this.output.writeInt(this.startPos + 10, this.output.getPos() - this.startPos - 14); // code_length
				this.catchPos = this.output.getPos();
				this.catchCount = 0;
				this.output.writeShort(0); // number of catch clauses
			}
		}

		int dataSize()
		{
			return this.output.size();
		}

		/**
		 * Ends adding a new method. The <code>add</code> method must be called
		 * before the <code>end</code> method is called.
		 *
		 * @param smap
		 *            a stack map table. may be null.
		 * @param aw
		 *            attributes to the <code>Code_attribute</code>. may be
		 *            null.
		 */
		public void end(StackMapTable.Writer smap, AttributeWriter aw)
		{
			if (this.isAbstract)
				return;

			// exception_table_length
			this.output.writeShort(this.catchPos, this.catchCount);

			int attrCount = smap == null ? 0 : 1;
			ClassFileWriter.writeAttribute(this.output, aw, attrCount);

			if (smap != null)
			{
				if (this.stackIndex == 0)
					this.stackIndex = this.constPool.addUtf8Info(StackMapTable.tag);

				this.output.writeShort(this.stackIndex);
				byte[] data = smap.toByteArray();
				this.output.writeInt(data.length);
				this.output.write(data);
			}

			// Code attribute_length
			this.output.writeInt(this.startPos + 2, this.output.getPos() - this.startPos - 6);
		}

		int numOfMethods()
		{
			return this.methodCount;
		}

		/**
		 * Returns the length of the bytecode that has been added so far.
		 *
		 * @return the length in bytes.
		 * @since 3.19
		 */
		public int size()
		{
			return this.output.getPos() - this.startPos - 14;
		}

		/**
		 * Writes the added methods.
		 */
		void write(OutputStream out) throws IOException
		{
			this.output.writeTo(out);
		}

		private void writeThrows(int[] exceptions)
		{
			if (this.throwsIndex == 0)
				this.throwsIndex = this.constPool.addUtf8Info(ExceptionsAttribute.tag);

			this.output.writeShort(this.throwsIndex);
			this.output.writeInt(exceptions.length * 2 + 2);
			this.output.writeShort(exceptions.length);
			for (int i = 0; i < exceptions.length; i++)
				this.output.writeShort(exceptions[i]);
		}
	}

	static void writeAttribute(ByteStream bs, AttributeWriter aw, int attrCount)
	{
		if (aw == null)
		{
			bs.writeShort(attrCount);
			return;
		}

		bs.writeShort(aw.size() + attrCount);
		DataOutputStream dos = new DataOutputStream(bs);
		try
		{
			aw.write(dos);
			dos.flush();
		} catch (IOException e)
		{
		}
	}

	private ByteStream output;

	private ConstPoolWriter constPool;

	private FieldWriter fields;

	private MethodWriter methods;

	int thisClass, superClass;

	/**
	 * Constructs a class file writer.
	 *
	 * @param major
	 *            the major version ({@link ClassFile#JAVA_4},
	 *            {@link ClassFile#JAVA_5}, ...).
	 * @param minor
	 *            the minor version (0 for JDK 1.3 and later).
	 */
	public ClassFileWriter(int major, int minor)
	{
		this.output = new ByteStream(512);
		this.output.writeInt(0xCAFEBABE); // magic
		this.output.writeShort(minor);
		this.output.writeShort(major);
		this.constPool = new ConstPoolWriter(this.output);
		this.fields = new FieldWriter(this.constPool);
		this.methods = new MethodWriter(this.constPool);

	}

	/**
	 * Ends writing and writes the contents of the class file into the given
	 * output stream.
	 *
	 * @param accessFlags
	 *            access flags.
	 * @param thisClass
	 *            this class. an index indicating its
	 *            <code>CONSTANT_Class_info</code>.
	 * @param superClass
	 *            super class. an index indicating its
	 *            <code>CONSTANT_Class_info</code>.
	 * @param interfaces
	 *            implemented interfaces. index numbers indicating their
	 *            <code>CONSTATNT_Class_info</code>. It may be null.
	 * @param aw
	 *            attributes of the class file. May be null.
	 * @see AccessFlag
	 */
	public void end(DataOutputStream out, int accessFlags, int thisClass, int superClass, int[] interfaces, AttributeWriter aw) throws IOException
	{
		this.constPool.end();
		this.output.writeTo(out);
		out.writeShort(accessFlags);
		out.writeShort(thisClass);
		out.writeShort(superClass);
		if (interfaces == null)
			out.writeShort(0);
		else
		{
			int n = interfaces.length;
			out.writeShort(n);
			for (int i = 0; i < n; i++)
				out.writeShort(interfaces[i]);
		}

		out.writeShort(this.fields.size());
		this.fields.write(out);

		out.writeShort(this.methods.numOfMethods());
		this.methods.write(out);
		if (aw == null)
			out.writeShort(0);
		else
		{
			out.writeShort(aw.size());
			aw.write(out);
		}
	}

	/**
	 * Ends writing and returns the contents of the class file.
	 *
	 * @param accessFlags
	 *            access flags.
	 * @param thisClass
	 *            this class. an index indicating its
	 *            <code>CONSTANT_Class_info</code>.
	 * @param superClass
	 *            super class. an index indicating its
	 *            <code>CONSTANT_Class_info</code>.
	 * @param interfaces
	 *            implemented interfaces. index numbers indicating their
	 *            <code>ClassInfo</code>. It may be null.
	 * @param aw
	 *            attributes of the class file. May be null.
	 * @see AccessFlag
	 */
	public byte[] end(int accessFlags, int thisClass, int superClass, int[] interfaces, AttributeWriter aw)
	{
		this.constPool.end();
		this.output.writeShort(accessFlags);
		this.output.writeShort(thisClass);
		this.output.writeShort(superClass);
		if (interfaces == null)
			this.output.writeShort(0);
		else
		{
			int n = interfaces.length;
			this.output.writeShort(n);
			for (int i = 0; i < n; i++)
				this.output.writeShort(interfaces[i]);
		}

		this.output.enlarge(this.fields.dataSize() + this.methods.dataSize() + 6);
		try
		{
			this.output.writeShort(this.fields.size());
			this.fields.write(this.output);

			this.output.writeShort(this.methods.numOfMethods());
			this.methods.write(this.output);
		} catch (IOException e)
		{
		}

		ClassFileWriter.writeAttribute(this.output, aw, 0);
		return this.output.toByteArray();
	}

	/**
	 * Returns a constant pool.
	 */
	public ConstPoolWriter getConstPool()
	{
		return this.constPool;
	}

	/**
	 * Returns a filed writer.
	 */
	public FieldWriter getFieldWriter()
	{
		return this.fields;
	}

	/**
	 * Returns a method writer.
	 */
	public MethodWriter getMethodWriter()
	{
		return this.methods;
	}
}
