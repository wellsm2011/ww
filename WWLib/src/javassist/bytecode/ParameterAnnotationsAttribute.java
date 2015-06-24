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

import javassist.bytecode.AnnotationsAttribute.Copier;
import javassist.bytecode.AnnotationsAttribute.Parser;
import javassist.bytecode.AnnotationsAttribute.Renamer;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationsWriter;

/**
 * A class representing <code>RuntimeVisibleAnnotations_attribute</code> and
 * <code>RuntimeInvisibleAnnotations_attribute</code>.
 *
 * <p>
 * To obtain an ParameterAnnotationAttribute object, invoke
 * <code>getAttribute(ParameterAnnotationsAttribute.invisibleTag)</code> in
 * <code>MethodInfo</code>. The obtained attribute is a runtime invisible
 * annotations attribute. If the parameter is
 * <code>ParameterAnnotationAttribute.visibleTag</code>, then the obtained
 * attribute is a runtime visible one.
 */
public class ParameterAnnotationsAttribute extends AttributeInfo
{
	/**
	 * The name of the <code>RuntimeVisibleParameterAnnotations</code>
	 * attribute.
	 */
	public static final String	visibleTag		= "RuntimeVisibleParameterAnnotations";

	/**
	 * The name of the <code>RuntimeInvisibleParameterAnnotations</code>
	 * attribute.
	 */
	public static final String	invisibleTag	= "RuntimeInvisibleParameterAnnotations";

	/**
	 * @param n
	 *            the attribute name.
	 */
	ParameterAnnotationsAttribute(ConstPool cp, int n, DataInputStream in) throws IOException
	{
		super(cp, n, in);
	}

	/**
	 * Constructs an empty
	 * <code>Runtime(In)VisibleParameterAnnotations_attribute</code>. A new
	 * annotation can be later added to the created attribute by
	 * <code>setAnnotations()</code>.
	 *
	 * @param cp
	 *            constant pool
	 * @param attrname
	 *            attribute name (<code>visibleTag</code> or
	 *            <code>invisibleTag</code>).
	 * @see #setAnnotations(Annotation[][])
	 */
	public ParameterAnnotationsAttribute(ConstPool cp, String attrname)
	{
		this(cp, attrname, new byte[]
		{ 0 });
	}

	/**
	 * Constructs a
	 * <code>Runtime(In)VisibleParameterAnnotations_attribute</code>.
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
	public ParameterAnnotationsAttribute(ConstPool cp, String attrname, byte[] info)
	{
		super(cp, attrname, info);
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
			copier.parameters();
			return new ParameterAnnotationsAttribute(newCp, this.getName(), copier.close());
		} catch (Exception e)
		{
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * Parses the annotations and returns a data structure representing that
	 * parsed annotations. Note that changes of the node values of the returned
	 * tree are not reflected on the annotations represented by this object
	 * unless the tree is copied back to this object by
	 * <code>setAnnotations()</code>.
	 *
	 * @return Each element of the returned array represents an array of
	 *         annotations that are associated with each method parameter.
	 * 
	 * @see #setAnnotations(Annotation[][])
	 */
	public Annotation[][] getAnnotations()
	{
		try
		{
			return new Parser(this.info, this.constPool).parseParameters();
		} catch (Exception e)
		{
			throw new RuntimeException(e.toString());
		}
	}

	@Override
	void getRefClasses(Map classnames)
	{
		this.renameClass(classnames);
	}

	/**
	 * Returns <code>num_parameters</code>.
	 */
	public int numParameters()
	{
		return this.info[0] & 0xff;
	}

	@Override
	void renameClass(Map classnames)
	{
		Renamer renamer = new Renamer(this.info, this.getConstPool(), classnames);
		try
		{
			renamer.parameters();
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
	 * Changes the annotations represented by this object according to the given
	 * array of <code>Annotation</code> objects.
	 *
	 * @param params
	 *            the data structure representing the new annotations. Every
	 *            element of this array is an array of <code>Annotation</code>
	 *            and it represens annotations of each method parameter.
	 */
	public void setAnnotations(Annotation[][] params)
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		AnnotationsWriter writer = new AnnotationsWriter(output, this.constPool);
		try
		{
			int n = params.length;
			writer.numParameters(n);
			for (int i = 0; i < n; ++i)
			{
				Annotation[] anno = params[i];
				writer.numAnnotations(anno.length);
				for (int j = 0; j < anno.length; ++j)
					anno[j].write(writer);
			}

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
		Annotation[][] aa = this.getAnnotations();
		StringBuilder sbuf = new StringBuilder();
		int k = 0;
		while (k < aa.length)
		{
			Annotation[] a = aa[k++];
			int i = 0;
			while (i < a.length)
			{
				sbuf.append(a[i++].toString());
				if (i != a.length)
					sbuf.append(" ");
			}

			if (k != aa.length)
				sbuf.append(", ");
		}

		return sbuf.toString();

	}
}
