package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javassist.bytecode.annotation.TypeAnnotationsWriter;

/**
 * A class representing {@code RuntimeVisibleTypeAnnotations} attribute and
 * {@code RuntimeInvisibleTypeAnnotations} attribute.
 *
 * @since 3.19
 */
public class TypeAnnotationsAttribute extends AttributeInfo
{
	static class Copier extends AnnotationsAttribute.Copier
	{
		SubCopier	sub;

		Copier(byte[] attrInfo, ConstPool src, ConstPool dest, Map map)
		{
			super(attrInfo, src, dest, map, false);
			TypeAnnotationsWriter w = new TypeAnnotationsWriter(this.output, dest);
			this.writer = w;
			this.sub = new SubCopier(attrInfo, src, dest, map, w);
		}

		@Override
		int annotationArray(int pos, int num) throws Exception
		{
			this.writer.numAnnotations(num);
			for (int i = 0; i < num; i++)
			{
				int targetType = this.info[pos] & 0xff;
				pos = this.sub.targetInfo(pos + 1, targetType);
				pos = this.sub.typePath(pos);
				pos = this.annotation(pos);
			}

			return pos;
		}
	}

	static class Renamer extends AnnotationsAttribute.Renamer
	{
		SubWalker	sub;

		Renamer(byte[] attrInfo, ConstPool cp, Map map)
		{
			super(attrInfo, cp, map);
			this.sub = new SubWalker(attrInfo);
		}

		@Override
		int annotationArray(int pos, int num) throws Exception
		{
			for (int i = 0; i < num; i++)
			{
				int targetType = this.info[pos] & 0xff;
				pos = this.sub.targetInfo(pos + 1, targetType);
				pos = this.sub.typePath(pos);
				pos = this.annotation(pos);
			}

			return pos;
		}
	}

	static class SubCopier extends SubWalker
	{
		ConstPool				srcPool, destPool;
		Map						classnames;
		TypeAnnotationsWriter	writer;

		SubCopier(byte[] attrInfo, ConstPool src, ConstPool dest, Map map, TypeAnnotationsWriter w)
		{
			super(attrInfo);
			this.srcPool = src;
			this.destPool = dest;
			this.classnames = map;
			this.writer = w;
		}

		@Override
		void catchTarget(int pos, int exceptionTableIndex) throws Exception
		{
			this.writer.catchTarget(exceptionTableIndex);
		}

		@Override
		void emptyTarget(int pos, int targetType) throws Exception
		{
			this.writer.emptyTarget(targetType);
		}

		@Override
		void formalParameterTarget(int pos, int formalParameterIndex) throws Exception
		{
			this.writer.formalParameterTarget(formalParameterIndex);
		}

		@Override
		int localvarTarget(int pos, int targetType, int tableLength) throws Exception
		{
			this.writer.localVarTarget(targetType, tableLength);
			return super.localvarTarget(pos, targetType, tableLength);
		}

		@Override
		void localvarTarget(int pos, int targetType, int startPc, int length, int index) throws Exception
		{
			this.writer.localVarTargetTable(startPc, length, index);
		}

		@Override
		void offsetTarget(int pos, int targetType, int offset) throws Exception
		{
			this.writer.offsetTarget(targetType, offset);
		}

		@Override
		void supertypeTarget(int pos, int superTypeIndex) throws Exception
		{
			this.writer.supertypeTarget(superTypeIndex);
		}

		@Override
		void throwsTarget(int pos, int throwsTypeIndex) throws Exception
		{
			this.writer.throwsTarget(throwsTypeIndex);
		}

		@Override
		void typeArgumentTarget(int pos, int targetType, int offset, int typeArgumentIndex) throws Exception
		{
			this.writer.typeArgumentTarget(targetType, offset, typeArgumentIndex);
		}

		@Override
		void typeParameterBoundTarget(int pos, int targetType, int typeParameterIndex, int boundIndex) throws Exception
		{
			this.writer.typeParameterBoundTarget(targetType, typeParameterIndex, boundIndex);
		}

		@Override
		void typeParameterTarget(int pos, int targetType, int typeParameterIndex) throws Exception
		{
			this.writer.typeParameterTarget(targetType, typeParameterIndex);
		}

		@Override
		int typePath(int pos, int pathLength) throws Exception
		{
			this.writer.typePath(pathLength);
			return super.typePath(pos, pathLength);
		}

		@Override
		void typePath(int pos, int typePathKind, int typeArgumentIndex) throws Exception
		{
			this.writer.typePathPath(typePathKind, typeArgumentIndex);
		}
	}

	static class SubWalker
	{
		byte[]	info;

		SubWalker(byte[] attrInfo)
		{
			this.info = attrInfo;
		}

		void catchTarget(int pos, int exceptionTableIndex) throws Exception
		{
		}

		void emptyTarget(int pos, int targetType) throws Exception
		{
		}

		void formalParameterTarget(int pos, int formalParameterIndex) throws Exception
		{
		}

		int localvarTarget(int pos, int targetType, int tableLength) throws Exception
		{
			for (int i = 0; i < tableLength; i++)
			{
				int start = ByteArray.readU16bit(this.info, pos);
				int length = ByteArray.readU16bit(this.info, pos + 2);
				int index = ByteArray.readU16bit(this.info, pos + 4);
				this.localvarTarget(pos, targetType, start, length, index);
				pos += 6;
			}

			return pos;
		}

		void localvarTarget(int pos, int targetType, int startPc, int length, int index) throws Exception
		{
		}

		void offsetTarget(int pos, int targetType, int offset) throws Exception
		{
		}

		void supertypeTarget(int pos, int superTypeIndex) throws Exception
		{
		}

		final int targetInfo(int pos, int type) throws Exception
		{
			switch (type)
			{
				case 0x00:
				case 0x01:
				{
					int index = this.info[pos] & 0xff;
					this.typeParameterTarget(pos, type, index);
					return pos + 1;
				}
				case 0x10:
				{
					int index = ByteArray.readU16bit(this.info, pos);
					this.supertypeTarget(pos, index);
					return pos + 2;
				}
				case 0x11:
				case 0x12:
				{
					int param = this.info[pos] & 0xff;
					int bound = this.info[pos + 1] & 0xff;
					this.typeParameterBoundTarget(pos, type, param, bound);
					return pos + 2;
				}
				case 0x13:
				case 0x14:
				case 0x15:
					this.emptyTarget(pos, type);
					return pos;
				case 0x16:
				{
					int index = this.info[pos] & 0xff;
					this.formalParameterTarget(pos, index);
					return pos + 1;
				}
				case 0x17:
				{
					int index = ByteArray.readU16bit(this.info, pos);
					this.throwsTarget(pos, index);
					return pos + 2;
				}
				case 0x40:
				case 0x41:
				{
					int len = ByteArray.readU16bit(this.info, pos);
					return this.localvarTarget(pos + 2, type, len);
				}
				case 0x42:
				{
					int index = ByteArray.readU16bit(this.info, pos);
					this.catchTarget(pos, index);
					return pos + 2;
				}
				case 0x43:
				case 0x44:
				case 0x45:
				case 0x46:
				{
					int offset = ByteArray.readU16bit(this.info, pos);
					this.offsetTarget(pos, type, offset);
					return pos + 2;
				}
				case 0x47:
				case 0x48:
				case 0x49:
				case 0x4a:
				case 0x4b:
				{
					int offset = ByteArray.readU16bit(this.info, pos);
					int index = this.info[pos + 2] & 0xff;
					this.typeArgumentTarget(pos, type, offset, index);
					return pos + 3;
				}
				default:
					throw new RuntimeException("invalid target type: " + type);
			}
		}

		void throwsTarget(int pos, int throwsTypeIndex) throws Exception
		{
		}

		void typeArgumentTarget(int pos, int targetType, int offset, int typeArgumentIndex) throws Exception
		{
		}

		void typeParameterBoundTarget(int pos, int targetType, int typeParameterIndex, int boundIndex) throws Exception
		{
		}

		void typeParameterTarget(int pos, int targetType, int typeParameterIndex) throws Exception
		{
		}

		final int typePath(int pos) throws Exception
		{
			int len = this.info[pos++] & 0xff;
			return this.typePath(pos, len);
		}

		int typePath(int pos, int pathLength) throws Exception
		{
			for (int i = 0; i < pathLength; i++)
			{
				int kind = this.info[pos] & 0xff;
				int index = this.info[pos + 1] & 0xff;
				this.typePath(pos, kind, index);
				pos += 2;
			}

			return pos;
		}

		void typePath(int pos, int typePathKind, int typeArgumentIndex) throws Exception
		{
		}
	}

	/**
	 * To visit each elements of the type annotation attribute, call
	 * {@code annotationArray()}.
	 *
	 * @see #annotationArray()
	 */
	static class TAWalker extends AnnotationsAttribute.Walker
	{
		SubWalker	subWalker;

		TAWalker(byte[] attrInfo)
		{
			super(attrInfo);
			this.subWalker = new SubWalker(attrInfo);
		}

		@Override
		int annotationArray(int pos, int num) throws Exception
		{
			for (int i = 0; i < num; i++)
			{
				int targetType = this.info[pos] & 0xff;
				pos = this.subWalker.targetInfo(pos + 1, targetType);
				pos = this.subWalker.typePath(pos);
				pos = this.annotation(pos);
			}

			return pos;
		}
	}

	/**
	 * The name of the {@code RuntimeVisibleTypeAnnotations} attribute.
	 */
	public static final String	visibleTag		= "RuntimeVisibleTypeAnnotations";

	/**
	 * The name of the {@code RuntimeInvisibleTypeAnnotations} attribute.
	 */
	public static final String	invisibleTag	= "RuntimeInvisibleTypeAnnotations";

	/**
	 * @param n
	 *            the attribute name.
	 */
	TypeAnnotationsAttribute(ConstPool cp, int n, DataInputStream in) throws IOException
	{
		super(cp, n, in);
	}

	/**
	 * Constructs a <code>Runtime(In)VisibleTypeAnnotations_attribute</code>.
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
	public TypeAnnotationsAttribute(ConstPool cp, String attrname, byte[] info)
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
			copier.annotationArray();
			return new TypeAnnotationsAttribute(newCp, this.getName(), copier.close());
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
}
