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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.AnnotationsWriter;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * A class representing <code>RuntimeVisibleAnnotations_attribute</code> and
 * <code>RuntimeInvisibleAnnotations_attribute</code>.
 * <p>
 * To obtain an AnnotationAttribute object, invoke
 * <code>getAttribute(AnnotationsAttribute.visibleTag)</code> in
 * <code>ClassFile</code>, <code>MethodInfo</code>, or <code>FieldInfo</code>.
 * The obtained attribute is a runtime visible annotations attribute. If the
 * parameter is <code>AnnotationAttribute.invisibleTag</code>, then the obtained
 * attribute is a runtime invisible one.
 * <p>
 * For example,
 *
 * <pre>
 * import javassist.bytecode.annotation.Annotation;
 *    :
 * CtMethod m = ... ;
 * MethodInfo minfo = m.getMethodInfo();
 * AnnotationsAttribute attr = (AnnotationsAttribute)
 *         minfo.getAttribute(AnnotationsAttribute.invisibleTag);
 * Annotation an = attr.getAnnotation("Author");
 * String s = ((StringMemberValue)an.getMemberValue("name")).getValue();
 * System.out.println("@Author(name=" + s + ")");
 * </pre>
 * <p>
 * This code snippet retrieves an annotation of the type <code>Author</code>
 * from the <code>MethodInfo</code> object specified by <code>minfo</code>.
 * Then, it prints the value of <code>name</code> in <code>Author</code>.
 * <p>
 * If the annotation type <code>Author</code> is annotated by a meta annotation:
 *
 * <pre>
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * </pre>
 * <p>
 * Then <code>Author</code> is visible at runtime. Therefore, the third
 * statement of the code snippet above must be changed into:
 *
 * <pre>
 * AnnotationsAttribute	attr	= (AnnotationsAttribute) minfo.getAttribute(AnnotationsAttribute.visibleTag);
 * </pre>
 * <p>
 * The attribute tag must be <code>visibleTag</code> instead of
 * <code>invisibleTag</code>.
 * <p>
 * If the member value of an annotation is not specified, the default value is
 * used as that member value. If so, <code>getMemberValue()</code> in
 * <code>Annotation</code> returns <code>null</code> since the default value is
 * not included in the <code>AnnotationsAttribute</code>. It is included in the
 * <code>AnnotationDefaultAttribute</code> of the method declared in the
 * annotation type.
 * <p>
 * If you want to record a new AnnotationAttribute object, execute the following
 * snippet:
 *
 * <pre>
 * ClassFile cf = ... ;
 * ConstPool cp = cf.getConstPool();
 * AnnotationsAttribute attr
 *     = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
 * Annotation a = new Annotation("Author", cp);
 * a.addMemberValue("name", new StringMemberValue("Chiba", cp));
 * attr.setAnnotation(a);
 * cf.addAttribute(attr);
 * cf.setVersionToJava5();
 * </pre>
 * <p>
 * The last statement is necessary if the class file was produced by
 * <code>javac</code> of JDK 1.4 or earlier. Otherwise, it is not necessary.
 *
 * @see AnnotationDefaultAttribute
 * @see javassist.bytecode.annotation.Annotation
 */
public class AnnotationsAttribute extends AttributeInfo
{
	static class Copier extends Walker
	{
		ByteArrayOutputStream	output;
		AnnotationsWriter		writer;
		ConstPool				srcPool, destPool;
		Map						classnames;

		/**
		 * Constructs a copier. This copier renames some class names into the
		 * new names specified by <code>map</code> when it copies an annotation
		 * attribute.
		 *
		 * @param info
		 *            the source attribute.
		 * @param src
		 *            the constant pool of the source class.
		 * @param dest
		 *            the constant pool of the destination class.
		 * @param map
		 *            pairs of replaced and substituted class names. It can be
		 *            null.
		 */
		Copier(byte[] info, ConstPool src, ConstPool dest, Map map)
		{
			this(info, src, dest, map, true);
		}

		Copier(byte[] info, ConstPool src, ConstPool dest, Map map, boolean makeWriter)
		{
			super(info);
			this.output = new ByteArrayOutputStream();
			if (makeWriter)
				this.writer = new AnnotationsWriter(this.output, dest);

			this.srcPool = src;
			this.destPool = dest;
			this.classnames = map;
		}

		@Override
		int annotation(int pos, int type, int numPairs) throws Exception
		{
			this.writer.annotation(this.copyType(type), numPairs);
			return super.annotation(pos, type, numPairs);
		}

		@Override
		int annotationArray(int pos, int num) throws Exception
		{
			this.writer.numAnnotations(num);
			return super.annotationArray(pos, num);
		}

		@Override
		int annotationMemberValue(int pos) throws Exception
		{
			this.writer.annotationValue();
			return super.annotationMemberValue(pos);
		}

		@Override
		int arrayMemberValue(int pos, int num) throws Exception
		{
			this.writer.arrayValue(num);
			return super.arrayMemberValue(pos, num);
		}

		@Override
		void classMemberValue(int pos, int index) throws Exception
		{
			this.writer.classInfoIndex(this.copyType(index));
			super.classMemberValue(pos, index);
		}

		byte[] close() throws IOException
		{
			this.writer.close();
			return this.output.toByteArray();
		}

		@Override
		void constValueMember(int tag, int index) throws Exception
		{
			this.writer.constValueIndex(tag, this.copy(index));
			super.constValueMember(tag, index);
		}

		/**
		 * Copies a constant pool entry into the destination constant pool and
		 * returns the index of the copied entry.
		 *
		 * @param srcIndex
		 *            the index of the copied entry into the source constant
		 *            pool.
		 * @return the index of the copied item into the destination constant
		 *         pool.
		 */
		int copy(int srcIndex)
		{
			return this.srcPool.copy(srcIndex, this.destPool, this.classnames);
		}

		/**
		 * Copies a constant pool entry into the destination constant pool and
		 * returns the index of the copied entry. That entry must be a Utf8Info
		 * representing a class name in the L<class name>; form.
		 *
		 * @param srcIndex
		 *            the index of the copied entry into the source constant
		 *            pool.
		 * @return the index of the copied item into the destination constant
		 *         pool.
		 */
		int copyType(int srcIndex)
		{
			String name = this.srcPool.getUtf8Info(srcIndex);
			String newName = Descriptor.rename(name, this.classnames);
			return this.destPool.addUtf8Info(newName);
		}

		@Override
		void enumMemberValue(int pos, int typeNameIndex, int constNameIndex) throws Exception
		{
			this.writer.enumConstValue(this.copyType(typeNameIndex), this.copy(constNameIndex));
			super.enumMemberValue(pos, typeNameIndex, constNameIndex);
		}

		@Override
		int memberValuePair(int pos, int nameIndex) throws Exception
		{
			this.writer.memberValuePair(this.copy(nameIndex));
			return super.memberValuePair(pos, nameIndex);
		}

		@Override
		void parameters(int numParam, int pos) throws Exception
		{
			this.writer.numParameters(numParam);
			super.parameters(numParam, pos);
		}
	}

	static class Parser extends Walker
	{
		ConstPool		pool;
		Annotation[][]	allParams;		// all parameters
		Annotation[]	allAnno;		// all annotations
		Annotation		currentAnno;	// current annotation
		MemberValue		currentMember;	// current member

		/**
		 * Constructs a parser. This parser constructs a parse tree of the
		 * annotations.
		 *
		 * @param info
		 *            the attribute.
		 * @param src
		 *            the constant pool.
		 */
		Parser(byte[] info, ConstPool cp)
		{
			super(info);
			this.pool = cp;
		}

		@Override
		int annotation(int pos, int type, int numPairs) throws Exception
		{
			this.currentAnno = new Annotation(type, this.pool);
			return super.annotation(pos, type, numPairs);
		}

		@Override
		int annotationArray(int pos, int num) throws Exception
		{
			Annotation[] array = new Annotation[num];
			for (int i = 0; i < num; ++i)
			{
				pos = this.annotation(pos);
				array[i] = this.currentAnno;
			}

			this.allAnno = array;
			return pos;
		}

		@Override
		int annotationMemberValue(int pos) throws Exception
		{
			Annotation anno = this.currentAnno;
			pos = super.annotationMemberValue(pos);
			this.currentMember = new AnnotationMemberValue(this.currentAnno, this.pool);
			this.currentAnno = anno;
			return pos;
		}

		@Override
		int arrayMemberValue(int pos, int num) throws Exception
		{
			ArrayMemberValue amv = new ArrayMemberValue(this.pool);
			MemberValue[] elements = new MemberValue[num];
			for (int i = 0; i < num; ++i)
			{
				pos = this.memberValue(pos);
				elements[i] = this.currentMember;
			}

			amv.setValue(elements);
			this.currentMember = amv;
			return pos;
		}

		@Override
		void classMemberValue(int pos, int index) throws Exception
		{
			this.currentMember = new ClassMemberValue(index, this.pool);
			super.classMemberValue(pos, index);
		}

		@Override
		void constValueMember(int tag, int index) throws Exception
		{
			MemberValue m;
			ConstPool cp = this.pool;
			switch (tag)
			{
				case 'B':
					m = new ByteMemberValue(index, cp);
					break;
				case 'C':
					m = new CharMemberValue(index, cp);
					break;
				case 'D':
					m = new DoubleMemberValue(index, cp);
					break;
				case 'F':
					m = new FloatMemberValue(index, cp);
					break;
				case 'I':
					m = new IntegerMemberValue(index, cp);
					break;
				case 'J':
					m = new LongMemberValue(index, cp);
					break;
				case 'S':
					m = new ShortMemberValue(index, cp);
					break;
				case 'Z':
					m = new BooleanMemberValue(index, cp);
					break;
				case 's':
					m = new StringMemberValue(index, cp);
					break;
				default:
					throw new RuntimeException("unknown tag:" + tag);
			}

			this.currentMember = m;
			super.constValueMember(tag, index);
		}

		@Override
		void enumMemberValue(int pos, int typeNameIndex, int constNameIndex) throws Exception
		{
			this.currentMember = new EnumMemberValue(typeNameIndex, constNameIndex, this.pool);
			super.enumMemberValue(pos, typeNameIndex, constNameIndex);
		}

		@Override
		int memberValuePair(int pos, int nameIndex) throws Exception
		{
			pos = super.memberValuePair(pos, nameIndex);
			this.currentAnno.addMemberValue(nameIndex, this.currentMember);
			return pos;
		}

		@Override
		void parameters(int numParam, int pos) throws Exception
		{
			Annotation[][] params = new Annotation[numParam][];
			for (int i = 0; i < numParam; ++i)
			{
				pos = this.annotationArray(pos);
				params[i] = this.allAnno;
			}

			this.allParams = params;
		}

		Annotation[] parseAnnotations() throws Exception
		{
			this.annotationArray();
			return this.allAnno;
		}

		MemberValue parseMemberValue() throws Exception
		{
			this.memberValue(0);
			return this.currentMember;
		}

		Annotation[][] parseParameters() throws Exception
		{
			this.parameters();
			return this.allParams;
		}
	}

	static class Renamer extends Walker
	{
		ConstPool	cpool;
		Map			classnames;

		/**
		 * Constructs a renamer. It renames some class names into the new names
		 * specified by <code>map</code>.
		 *
		 * @param info
		 *            the annotations attribute.
		 * @param cp
		 *            the constant pool.
		 * @param map
		 *            pairs of replaced and substituted class names. It can be
		 *            null.
		 */
		Renamer(byte[] info, ConstPool cp, Map map)
		{
			super(info);
			this.cpool = cp;
			this.classnames = map;
		}

		@Override
		int annotation(int pos, int type, int numPairs) throws Exception
		{
			this.renameType(pos - 4, type);
			return super.annotation(pos, type, numPairs);
		}

		@Override
		void classMemberValue(int pos, int index) throws Exception
		{
			this.renameType(pos + 1, index);
			super.classMemberValue(pos, index);
		}

		@Override
		void enumMemberValue(int pos, int typeNameIndex, int constNameIndex) throws Exception
		{
			this.renameType(pos + 1, typeNameIndex);
			super.enumMemberValue(pos, typeNameIndex, constNameIndex);
		}

		private void renameType(int pos, int index)
		{
			String name = this.cpool.getUtf8Info(index);
			String newName = Descriptor.rename(name, this.classnames);
			if (!name.equals(newName))
			{
				int index2 = this.cpool.addUtf8Info(newName);
				ByteArray.write16bit(index2, this.info, pos);
			}
		}
	}

	static class Walker
	{
		byte[]	info;

		Walker(byte[] attrInfo)
		{
			this.info = attrInfo;
		}

		final int annotation(int pos) throws Exception
		{
			int type = ByteArray.readU16bit(this.info, pos);
			int numPairs = ByteArray.readU16bit(this.info, pos + 2);
			return this.annotation(pos + 4, type, numPairs);
		}

		int annotation(int pos, int type, int numPairs) throws Exception
		{
			for (int j = 0; j < numPairs; ++j)
				pos = this.memberValuePair(pos);

			return pos;
		}

		final void annotationArray() throws Exception
		{
			this.annotationArray(0);
		}

		final int annotationArray(int pos) throws Exception
		{
			int num = ByteArray.readU16bit(this.info, pos);
			return this.annotationArray(pos + 2, num);
		}

		int annotationArray(int pos, int num) throws Exception
		{
			for (int i = 0; i < num; ++i)
				pos = this.annotation(pos);

			return pos;
		}

		/**
		 * {@code annotation_value}
		 */
		int annotationMemberValue(int pos) throws Exception
		{
			return this.annotation(pos);
		}

		/**
		 * {@code array_value}
		 */
		int arrayMemberValue(int pos, int num) throws Exception
		{
			for (int i = 0; i < num; ++i)
				pos = this.memberValue(pos);

			return pos;
		}

		/**
		 * {@code class_info_index}
		 */
		void classMemberValue(int pos, int index) throws Exception
		{
		}

		/**
		 * {@code const_value_index}
		 */
		void constValueMember(int tag, int index) throws Exception
		{
		}

		/**
		 * {@code enum_const_value}
		 */
		void enumMemberValue(int pos, int typeNameIndex, int constNameIndex) throws Exception
		{
		}

		/**
		 * {@code element_value}
		 */
		final int memberValue(int pos) throws Exception
		{
			int tag = this.info[pos] & 0xff;
			if (tag == 'e')
			{
				int typeNameIndex = ByteArray.readU16bit(this.info, pos + 1);
				int constNameIndex = ByteArray.readU16bit(this.info, pos + 3);
				this.enumMemberValue(pos, typeNameIndex, constNameIndex);
				return pos + 5;
			} else if (tag == 'c')
			{
				int index = ByteArray.readU16bit(this.info, pos + 1);
				this.classMemberValue(pos, index);
				return pos + 3;
			} else if (tag == '@')
				return this.annotationMemberValue(pos + 1);
			else if (tag == '[')
			{
				int num = ByteArray.readU16bit(this.info, pos + 1);
				return this.arrayMemberValue(pos + 3, num);
			} else
			{ // primitive types or String.
				int index = ByteArray.readU16bit(this.info, pos + 1);
				this.constValueMember(tag, index);
				return pos + 3;
			}
		}

		/**
		 * {@code element_value_paris}
		 */
		final int memberValuePair(int pos) throws Exception
		{
			int nameIndex = ByteArray.readU16bit(this.info, pos);
			return this.memberValuePair(pos + 2, nameIndex);
		}

		/**
		 * {@code element_value_paris[]}
		 */
		int memberValuePair(int pos, int nameIndex) throws Exception
		{
			return this.memberValue(pos);
		}

		final void parameters() throws Exception
		{
			int numParam = this.info[0] & 0xff;
			this.parameters(numParam, 1);
		}

		void parameters(int numParam, int pos) throws Exception
		{
			for (int i = 0; i < numParam; ++i)
				pos = this.annotationArray(pos);
		}
	}

	/**
	 * The name of the <code>RuntimeVisibleAnnotations</code> attribute.
	 */
	public static final String	visibleTag		= "RuntimeVisibleAnnotations";

	/**
	 * The name of the <code>RuntimeInvisibleAnnotations</code> attribute.
	 */
	public static final String	invisibleTag	= "RuntimeInvisibleAnnotations";

	/**
	 * @param n
	 *            the attribute name.
	 */
	AnnotationsAttribute(ConstPool cp, int n, DataInputStream in) throws IOException
	{
		super(cp, n, in);
	}

	/**
	 * Constructs an empty <code>Runtime(In)VisibleAnnotations_attribute</code>.
	 * A new annotation can be later added to the created attribute by
	 * <code>setAnnotations()</code>.
	 *
	 * @param cp
	 *            constant pool
	 * @param attrname
	 *            attribute name (<code>visibleTag</code> or
	 *            <code>invisibleTag</code>).
	 * @see #setAnnotations(Annotation[])
	 */
	public AnnotationsAttribute(ConstPool cp, String attrname)
	{
		this(cp, attrname, new byte[]
		{ 0, 0 });
	}

	/**
	 * Constructs a <code>Runtime(In)VisibleAnnotations_attribute</code>.
	 *
	 * @param cp
	 *            constant pool
	 * @param attrname
	 *            attribute name (<code>visibleTag</code> or
	 *            <code>invisibleTag</code>).
	 * @param info
	 *            the contents of this attribute. It does not include
	 *            <code>attribute_name_index</code> or
	 *            <code>attribute_length</code>.
	 */
	public AnnotationsAttribute(ConstPool cp, String attrname, byte[] info)
	{
		super(cp, attrname, info);
	}

	/**
	 * Adds an annotation. If there is an annotation with the same type, it is
	 * removed before the new annotation is added.
	 *
	 * @param annotation
	 *            the added annotation.
	 */
	public void addAnnotation(Annotation annotation)
	{
		String type = annotation.getTypeName();
		Annotation[] annotations = this.getAnnotations();
		for (int i = 0; i < annotations.length; i++)
			if (annotations[i].getTypeName().equals(type))
			{
				annotations[i] = annotation;
				this.setAnnotations(annotations);
				return;
			}

		Annotation[] newlist = new Annotation[annotations.length + 1];
		System.arraycopy(annotations, 0, newlist, 0, annotations.length);
		newlist[annotations.length] = annotation;
		this.setAnnotations(newlist);
	}

	/**
	 * Copies this attribute and returns a new copy.
	 */
	@Override
	public AttributeInfo copy(ConstPool newCp, Map classnames)
	{
		Copier copier = new Copier(this.info, this.constPool, newCp, classnames);
		try
		{
			copier.annotationArray();
			return new AnnotationsAttribute(newCp, this.getName(), copier.close());
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parses the annotations and returns a data structure representing the
	 * annotation with the specified type. See also
	 * <code>getAnnotations()</code> as to the returned data structure.
	 *
	 * @param type
	 *            the annotation type.
	 * @return null if the specified annotation type is not included.
	 * @see #getAnnotations()
	 */
	public Annotation getAnnotation(String type)
	{
		Annotation[] annotations = this.getAnnotations();
		for (int i = 0; i < annotations.length; i++)
			if (annotations[i].getTypeName().equals(type))
				return annotations[i];

		return null;
	}

	/**
	 * Parses the annotations and returns a data structure representing that
	 * parsed annotations. Note that changes of the node values of the returned
	 * tree are not reflected on the annotations represented by this object
	 * unless the tree is copied back to this object by
	 * <code>setAnnotations()</code>.
	 *
	 * @see #setAnnotations(Annotation[])
	 */
	public Annotation[] getAnnotations()
	{
		try
		{
			return new Parser(this.info, this.constPool).parseAnnotations();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	void getRefClasses(Map classnames)
	{
		this.renameClass(classnames);
	}

	/**
	 * Returns <code>num_annotations</code>.
	 */
	public int numAnnotations()
	{
		return ByteArray.readU16bit(this.info, 0);
	}

	@Override
	void renameClass(Map classnames)
	{
		Renamer renamer = new Renamer(this.info, this.getConstPool(), classnames);
		try
		{
			renamer.annotationArray();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param oldname
	 *            a JVM class name.
	 * @param newname
	 *            a JVM class name.
	 */
	@Override
	void renameClass(String oldname, String newname)
	{
		HashMap map = new HashMap();
		map.put(oldname, newname);
		this.renameClass(map);
	}

	/**
	 * Changes the annotations. A call to this method is equivalent to:
	 *
	 * <pre>
	 * setAnnotations(new Annotation[]
	 * { annotation })
	 * </pre>
	 *
	 * @param annotation
	 *            the data structure representing the new annotation.
	 */
	public void setAnnotation(Annotation annotation)
	{
		this.setAnnotations(new Annotation[]
		{ annotation });
	}

	/**
	 * Changes the annotations represented by this object according to the given
	 * array of <code>Annotation</code> objects.
	 *
	 * @param annotations
	 *            the data structure representing the new annotations.
	 */
	public void setAnnotations(Annotation[] annotations)
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		AnnotationsWriter writer = new AnnotationsWriter(output, this.constPool);
		try
		{
			int n = annotations.length;
			writer.numAnnotations(n);
			for (int i = 0; i < n; ++i)
				annotations[i].write(writer);

			writer.close();
		} catch (IOException e)
		{
			throw new RuntimeException(e); // should never reach here.
		}

		this.set(output.toByteArray());
	}

	/**
	 * Returns a string representation of this object.
	 */
	@Override
	public String toString()
	{
		Annotation[] a = this.getAnnotations();
		StringBuilder sbuf = new StringBuilder();
		int i = 0;
		while (i < a.length)
		{
			sbuf.append(a[i++].toString());
			if (i != a.length)
				sbuf.append(", ");
		}

		return sbuf.toString();
	}
}
