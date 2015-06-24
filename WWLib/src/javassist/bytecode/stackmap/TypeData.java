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

package javassist.bytecode.stackmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.StackMapTable;

public abstract class TypeData
{
	/*
	 * Memo: array type is a subtype of Cloneable and Serializable
	 */

	// a type variable
	public static abstract class AbsTypeVar extends TypeData
	{
		public AbsTypeVar()
		{
		}

		@Override
		public boolean eq(TypeData d)
		{
			return this.getName().equals(d.getName());
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			return cp.addClassInfo(this.getName());
		}

		@Override
		public int getTypeTag()
		{
			return StackMapTable.OBJECT;
		}

		public abstract void merge(TypeData t);
	}

	/*
	 * A type variable representing an array-element type. It is a decorator of
	 * another type variable.
	 */
	public static class ArrayElement extends AbsTypeVar
	{
		public static TypeData make(TypeData array) throws BadBytecode
		{
			if (array instanceof ArrayType)
				return ((ArrayType) array).elementType();
			else if (array instanceof AbsTypeVar)
				return new ArrayElement((AbsTypeVar) array);
			else if (array instanceof ClassName)
				if (!array.isNullType())
					return new ClassName(ArrayElement.typeName(array.getName()));

			throw new BadBytecode("bad AASTORE: " + array);
		}

		private static String typeName(String arrayType)
		{
			if (arrayType.length() > 1 && arrayType.charAt(0) == '[')
			{
				char c = arrayType.charAt(1);
				if (c == 'L')
					return arrayType.substring(2, arrayType.length() - 1).replace('/', '.');
				else if (c == '[')
					return arrayType.substring(1);
			}

			return "java.lang.Object"; // the array type may be NullType
		}

		private AbsTypeVar	array;

		private ArrayElement(AbsTypeVar a)
		{ // a is never null
			this.array = a;
		}

		public AbsTypeVar arrayType()
		{
			return this.array;
		}

		@Override
		public int dfs(ArrayList order, int index, ClassPool cp) throws NotFoundException
		{
			return this.array.dfs(order, index, cp);
		}

		/*
		 * arrayType must be a class name. Basic type names are not allowed.
		 */

		@Override
		public String getName()
		{
			return ArrayElement.typeName(this.array.getName());
		}

		@Override
		public boolean is2WordType()
		{
			return false;
		}

		@Override
		public BasicType isBasicType()
		{
			return null;
		}

		@Override
		public void merge(TypeData t)
		{
			try
			{
				if (!t.isNullType())
					this.array.merge(ArrayType.make(t));
			} catch (BadBytecode e)
			{
				// never happens
				throw new RuntimeException("fatal: " + e);
			}
		}

		@Override
		public void setType(String s, ClassPool cp) throws BadBytecode
		{
			this.array.setType(ArrayType.typeName(s), cp);
		}

		@Override
		protected TypeVar toTypeVar()
		{
			return this.array.toTypeVar();
		}
	}

	/*
	 * A type variable representing an array type. It is a decorator of another
	 * type variable.
	 */
	public static class ArrayType extends AbsTypeVar
	{
		static TypeData make(TypeData element) throws BadBytecode
		{
			if (element instanceof ArrayElement)
				return ((ArrayElement) element).arrayType();
			else if (element instanceof AbsTypeVar)
				return new ArrayType((AbsTypeVar) element);
			else if (element instanceof ClassName)
				if (!element.isNullType())
					return new ClassName(ArrayType.typeName(element.getName()));

			throw new BadBytecode("bad AASTORE: " + element);
		}

		/*
		 * elementType must be a class name. Basic type names are not allowed.
		 */
		public static String typeName(String elementType)
		{
			if (elementType.charAt(0) == '[')
				return "[" + elementType;
			else
				return "[L" + elementType.replace('.', '/') + ";";
		}

		private AbsTypeVar	element;

		private ArrayType(AbsTypeVar elementType)
		{
			this.element = elementType;
		}

		@Override
		public int dfs(ArrayList order, int index, ClassPool cp) throws NotFoundException
		{
			return this.element.dfs(order, index, cp);
		}

		public AbsTypeVar elementType()
		{
			return this.element;
		}

		@Override
		public String getName()
		{
			return ArrayType.typeName(this.element.getName());
		}

		@Override
		public boolean is2WordType()
		{
			return false;
		}

		@Override
		public BasicType isBasicType()
		{
			return null;
		}

		@Override
		public void merge(TypeData t)
		{
			try
			{
				if (!t.isNullType())
					this.element.merge(ArrayElement.make(t));
			} catch (BadBytecode e)
			{
				// never happens
				throw new RuntimeException("fatal: " + e);
			}
		}

		@Override
		public void setType(String s, ClassPool cp) throws BadBytecode
		{
			this.element.setType(ArrayElement.typeName(s), cp);
		}

		@Override
		protected TypeVar toTypeVar()
		{
			return this.element.toTypeVar();
		}
	}

	/**
	 * Primitive types.
	 */
	protected static class BasicType extends TypeData
	{
		private String	name;
		private int		typeTag;

		public BasicType(String type, int tag)
		{
			this.name = type;
			this.typeTag = tag;
		}

		@Override
		public boolean eq(TypeData d)
		{
			return this == d;
		}

		@Override
		public String getName()
		{
			return this.name;
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			return 0;
		}

		@Override
		public int getTypeTag()
		{
			return this.typeTag;
		}

		@Override
		public boolean is2WordType()
		{
			return this.typeTag == StackMapTable.LONG || this.typeTag == StackMapTable.DOUBLE;
		}

		@Override
		public BasicType isBasicType()
		{
			return this;
		}

		@Override
		public TypeData join()
		{
			if (this == TypeTag.TOP)
				return this;
			else
				return super.join();
		}

		@Override
		public void setType(String s, ClassPool cp) throws BadBytecode
		{
			throw new BadBytecode("conflict: " + this.name + " and " + s);
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	/**
	 * Type data for OBJECT.
	 */
	public static class ClassName extends TypeData
	{
		private String	name;	// dot separated.

		public ClassName(String n)
		{
			this.name = n;
		}

		@Override
		public boolean eq(TypeData d)
		{
			return this.name.equals(d.getName());
		}

		@Override
		public String getName()
		{
			return this.name;
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			return cp.addClassInfo(this.getName());
		}

		@Override
		public int getTypeTag()
		{
			return StackMapTable.OBJECT;
		}

		@Override
		public boolean is2WordType()
		{
			return false;
		}

		@Override
		public BasicType isBasicType()
		{
			return null;
		}

		@Override
		public void setType(String typeName, ClassPool cp) throws BadBytecode
		{
		}
	}

	/**
	 * Type data for NULL or OBJECT. The types represented by the instances of
	 * this class are initially NULL but will be OBJECT.
	 */
	public static class NullType extends ClassName
	{
		public NullType()
		{
			super("null-type"); // type name
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			return 0;
		}

		@Override
		public int getTypeTag()
		{
			return StackMapTable.NULL;
		}

		@Override
		public boolean isNullType()
		{
			return true;
		}
	}

	/*
	 * a type variable representing a class type or a basic type.
	 */
	public static class TypeVar extends AbsTypeVar
	{
		private static boolean isObjectArray(CtClass cc) throws NotFoundException
		{
			return cc.isArray() && cc.getComponentType().getSuperclass() == null;
		}

		protected ArrayList	lowers;				// lower bounds of this
													// type. ArrayList<TypeData>
		protected ArrayList	usedBy;				// reverse relations of
													// lowers
		protected ArrayList	uppers;				// upper bounds of this
													// type.
		protected String	fixedType;

		private boolean		is2WordType;			// cache

		private int			visited		= 0;

		private int			smallest	= 0;

		private boolean		inList		= false;

		public TypeVar(TypeData t)
		{
			this.uppers = null;
			this.lowers = new ArrayList(2);
			this.usedBy = new ArrayList(2);
			this.merge(t);
			this.fixedType = null;
			this.is2WordType = t.is2WordType();
		}

		// depth-first serach
		@Override
		public int dfs(ArrayList preOrder, int index, ClassPool cp) throws NotFoundException
		{
			if (this.visited > 0)
				return index; // MapMaker.make() may call an already visited
								// node.

			this.visited = this.smallest = ++index;
			preOrder.add(this);
			this.inList = true;
			int n = this.lowers.size();
			for (int i = 0; i < n; i++)
			{
				TypeVar child = ((TypeData) this.lowers.get(i)).toTypeVar();
				if (child != null)
					if (child.visited == 0)
					{
						index = child.dfs(preOrder, index, cp);
						if (child.smallest < this.smallest)
							this.smallest = child.smallest;
					} else if (child.inList)
						if (child.visited < this.smallest)
							this.smallest = child.visited;
			}

			if (this.visited == this.smallest)
			{
				ArrayList scc = new ArrayList(); // strongly connected component
				TypeVar cv;
				do
				{
					cv = (TypeVar) preOrder.remove(preOrder.size() - 1);
					cv.inList = false;
					scc.add(cv);
				} while (cv != this);
				this.fixTypes(scc, cp);
			}

			return index;
		}

		private CtClass fixByUppers(ArrayList users, ClassPool cp, HashSet visited, CtClass type) throws NotFoundException
		{
			if (users == null)
				return type;

			int size = users.size();
			for (int i = 0; i < size; i++)
			{
				TypeVar t = (TypeVar) users.get(i);
				if (!visited.add(t))
					return type;

				if (t.uppers != null)
				{
					int s = t.uppers.size();
					for (int k = 0; k < s; k++)
					{
						CtClass cc = cp.get((String) t.uppers.get(k));
						if (cc.subtypeOf(type))
							type = cc;
					}
				}

				type = this.fixByUppers(t.usedBy, cp, visited, type);
			}

			return type;
		}

		private void fixTypes(ArrayList scc, ClassPool cp) throws NotFoundException
		{
			HashSet lowersSet = new HashSet();
			boolean isBasicType = false;
			TypeData kind = null;
			int size = scc.size();
			for (int i = 0; i < size; i++)
			{
				ArrayList tds = ((TypeVar) scc.get(i)).lowers;
				int size2 = tds.size();
				for (int j = 0; j < size2; j++)
				{
					TypeData d = (TypeData) tds.get(j);
					BasicType bt = d.isBasicType();
					if (kind == null)
					{
						if (bt == null)
						{
							isBasicType = false;
							kind = d;
							/*
							 * If scc has only an UninitData, fixedType is kept
							 * null. So lowerSet must be empty. If scc has not
							 * only an UninitData but also another TypeData, an
							 * error must be thrown but this error detection has
							 * not been implemented.
							 */
							if (d.isUninit())
								break;
						} else
						{
							isBasicType = true;
							kind = bt;
						}
					} else if (bt == null && isBasicType || bt != null && kind != bt)
					{
						isBasicType = true;
						kind = TypeTag.TOP;
						break;
					}

					if (bt == null && !d.isNullType())
						lowersSet.add(d.getName());
				}
			}

			if (isBasicType)
				for (int i = 0; i < size; i++)
				{
					TypeVar cv = (TypeVar) scc.get(i);
					cv.lowers.clear();
					cv.lowers.add(kind);
					this.is2WordType = kind.is2WordType();
				}
			else
			{
				String typeName = this.fixTypes2(scc, lowersSet, cp);
				for (int i = 0; i < size; i++)
				{
					TypeVar cv = (TypeVar) scc.get(i);
					cv.fixedType = typeName;
				}
			}
		}

		private String fixTypes2(ArrayList scc, HashSet lowersSet, ClassPool cp) throws NotFoundException
		{
			Iterator it = lowersSet.iterator();
			if (lowersSet.size() == 0)
				return null; // only NullType
			else if (lowersSet.size() == 1)
				return (String) it.next();
			else
			{
				CtClass cc = cp.get((String) it.next());
				while (it.hasNext())
					cc = TypeData.commonSuperClassEx(cc, cp.get((String) it.next()));

				if (cc.getSuperclass() == null || TypeVar.isObjectArray(cc))
					cc = this.fixByUppers(scc, cp, new HashSet(), cc);

				if (cc.isArray())
					return Descriptor.toJvmName(cc);
				else
					return cc.getName();
			}
		}

		@Override
		public String getName()
		{
			if (this.fixedType == null)
				return ((TypeData) this.lowers.get(0)).getName();
			else
				return this.fixedType;
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			if (this.fixedType == null)
				return ((TypeData) this.lowers.get(0)).getTypeData(cp);
			else
				return super.getTypeData(cp);
		}

		@Override
		public int getTypeTag()
		{
			/*
			 * If fixedType is null after calling dfs(), then this type is NULL,
			 * Uninit, or a basic type. So call getTypeTag() on the first
			 * element of lowers.
			 */
			if (this.fixedType == null)
				return ((TypeData) this.lowers.get(0)).getTypeTag();
			else
				return super.getTypeTag();
		}

		@Override
		public boolean is2WordType()
		{
			if (this.fixedType == null)
				return this.is2WordType;
			// return ((TypeData)lowers.get(0)).is2WordType();
			else
				return false;
		}

		@Override
		public BasicType isBasicType()
		{
			if (this.fixedType == null)
				return ((TypeData) this.lowers.get(0)).isBasicType();
			else
				return null;
		}

		@Override
		public boolean isNullType()
		{
			if (this.fixedType == null)
				return ((TypeData) this.lowers.get(0)).isNullType();
			else
				return false;
		}

		@Override
		public boolean isUninit()
		{
			if (this.fixedType == null)
				return ((TypeData) this.lowers.get(0)).isUninit();
			else
				return false;
		}

		@Override
		public void merge(TypeData t)
		{
			this.lowers.add(t);
			if (t instanceof TypeVar)
				((TypeVar) t).usedBy.add(this);
		}

		@Override
		public void setType(String typeName, ClassPool cp) throws BadBytecode
		{
			if (this.uppers == null)
				this.uppers = new ArrayList();

			this.uppers.add(typeName);
		}

		@Override
		protected TypeVar toTypeVar()
		{
			return this;
		}
	}

	/**
	 * Type data for UNINIT.
	 */
	public static class UninitData extends ClassName
	{
		int		offset;
		boolean	initialized;

		UninitData(int offset, String className)
		{
			super(className);
			this.offset = offset;
			this.initialized = false;
		}

		@Override
		public void constructorCalled(int offset)
		{
			if (offset == this.offset)
				this.initialized = true;
		}

		public UninitData copy()
		{
			return new UninitData(this.offset, this.getName());
		}

		@Override
		public boolean eq(TypeData d)
		{
			if (d instanceof UninitData)
			{
				UninitData ud = (UninitData) d;
				return this.offset == ud.offset && this.getName().equals(ud.getName());
			} else
				return false;
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			return this.offset;
		}

		@Override
		public int getTypeTag()
		{
			return StackMapTable.UNINIT;
		}

		@Override
		public boolean isUninit()
		{
			return true;
		}

		@Override
		public TypeData join()
		{
			if (this.initialized)
				return new TypeVar(new ClassName(this.getName()));
			else
				return new UninitTypeVar(this.copy());
		}

		public int offset()
		{
			return this.offset;
		}

		@Override
		public String toString()
		{
			return "uninit:" + this.getName() + "@" + this.offset;
		}
	}

	public static class UninitThis extends UninitData
	{
		UninitThis(String className)
		{
			super(-1, className);
		}

		@Override
		public UninitData copy()
		{
			return new UninitThis(this.getName());
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			return 0;
		}

		@Override
		public int getTypeTag()
		{
			return StackMapTable.THIS;
		}

		@Override
		public String toString()
		{
			return "uninit:this";
		}
	}

	public static class UninitTypeVar extends AbsTypeVar
	{
		protected TypeData	type;	// UninitData or TOP

		public UninitTypeVar(UninitData t)
		{
			this.type = t;
		}

		@Override
		public void constructorCalled(int offset)
		{
			this.type.constructorCalled(offset);
		}

		@Override
		public boolean eq(TypeData d)
		{
			return this.type.eq(d);
		}

		@Override
		public String getName()
		{
			return this.type.getName();
		}

		@Override
		public int getTypeData(ConstPool cp)
		{
			return this.type.getTypeData(cp);
		}

		@Override
		public int getTypeTag()
		{
			return this.type.getTypeTag();
		}

		@Override
		public boolean is2WordType()
		{
			return this.type.is2WordType();
		}

		@Override
		public BasicType isBasicType()
		{
			return this.type.isBasicType();
		}

		@Override
		public boolean isUninit()
		{
			return this.type.isUninit();
		}

		@Override
		public TypeData join()
		{
			return this.type.join();
		}

		@Override
		public void merge(TypeData t)
		{
			if (!t.eq(this.type))
				this.type = TypeTag.TOP;
		}

		public int offset()
		{
			if (this.type instanceof UninitData)
				return ((UninitData) this.type).offset;
			else
				// if type == TypeTag.TOP
				throw new RuntimeException("not available");
		}

		@Override
		public void setType(String s, ClassPool cp) throws BadBytecode
		{
			this.type.setType(s, cp);
		}

		@Override
		protected TypeVar toTypeVar()
		{
			return null;
		}
	}

	public static void aastore(TypeData array, TypeData value, ClassPool cp) throws BadBytecode
	{
		if (array instanceof AbsTypeVar)
			if (!value.isNullType())
				((AbsTypeVar) array).merge(ArrayType.make(value));

		if (value instanceof AbsTypeVar)
			if (array instanceof AbsTypeVar)
				ArrayElement.make(array); // should call value.setType() later.
			else if (array instanceof ClassName)
			{
				if (!array.isNullType())
				{
					String type = ArrayElement.typeName(array.getName());
					value.setType(type, cp);
				}
			} else
				throw new BadBytecode("bad AASTORE: " + array);
	}

	/**
	 * Finds the most specific common super class of the given classes. This
	 * method is a copy from javassist.bytecode.analysis.Type.
	 */
	public static CtClass commonSuperClass(CtClass one, CtClass two) throws NotFoundException
	{
		CtClass deep = one;
		CtClass shallow = two;
		CtClass backupShallow = shallow;
		CtClass backupDeep = deep;

		// Phase 1 - Find the deepest hierarchy, set deep and shallow correctly
		for (;;)
		{
			// In case we get lucky, and find a match early
			if (TypeData.eq(deep, shallow) && deep.getSuperclass() != null)
				return deep;

			CtClass deepSuper = deep.getSuperclass();
			CtClass shallowSuper = shallow.getSuperclass();

			if (shallowSuper == null)
			{
				// right, now reset shallow
				shallow = backupShallow;
				break;
			}

			if (deepSuper == null)
			{
				// wrong, swap them, since deep is now useless, its our tmp
				// before we swap it
				deep = backupDeep;
				backupDeep = backupShallow;
				backupShallow = deep;

				deep = shallow;
				shallow = backupShallow;
				break;
			}

			deep = deepSuper;
			shallow = shallowSuper;
		}

		// Phase 2 - Move deepBackup up by (deep end - deep)
		for (;;)
		{
			deep = deep.getSuperclass();
			if (deep == null)
				break;

			backupDeep = backupDeep.getSuperclass();
		}

		deep = backupDeep;

		// Phase 3 - The hierarchy positions are now aligned
		// The common super class is easy to find now
		while (!TypeData.eq(deep, shallow))
		{
			deep = deep.getSuperclass();
			shallow = shallow.getSuperclass();
		}

		return deep;
	}

	/**
	 * Finds the most specific common super class of the given classes by
	 * considering array types.
	 */
	public static CtClass commonSuperClassEx(CtClass one, CtClass two) throws NotFoundException
	{
		if (one == two)
			return one;
		else if (one.isArray() && two.isArray())
		{
			CtClass ele1 = one.getComponentType();
			CtClass ele2 = two.getComponentType();
			CtClass element = TypeData.commonSuperClassEx(ele1, ele2);
			if (element == ele1)
				return one;
			else if (element == ele2)
				return two;
			else
				return one.getClassPool().get(element == null ? "java.lang.Object" : element.getName() + "[]");
		} else if (one.isPrimitive() || two.isPrimitive())
			return null; // TOP
		else if (one.isArray() || two.isArray()) // but !(one.isArray() &&
													// two.isArray())
			return one.getClassPool().get("java.lang.Object");
		else
			return TypeData.commonSuperClass(one, two);
	}

	static boolean eq(CtClass one, CtClass two)
	{
		return one == two || one != null && two != null && one.getName().equals(two.getName());
	}

	public static TypeData[] make(int size)
	{
		TypeData[] array = new TypeData[size];
		for (int i = 0; i < size; i++)
			array[i] = TypeTag.TOP;

		return array;
	}

	/**
	 * Sets the type name of this object type. If the given type name is a
	 * subclass of the current type name, then the given name becomes the name
	 * of this object type.
	 *
	 * @param className
	 *            dot-separated name unless the type is an array type.
	 */
	private static void setType(TypeData td, String className, ClassPool cp) throws BadBytecode
	{
		td.setType(className, cp);
	}

	protected TypeData()
	{
	}

	// see UninitTypeVar and UninitData
	public void constructorCalled(int offset)
	{
	}

	// depth-first search
	public int dfs(ArrayList order, int index, ClassPool cp) throws NotFoundException
	{
		return index;
	}

	public abstract boolean eq(TypeData d);

	public abstract String getName();

	public abstract int getTypeData(ConstPool cp);

	public abstract int getTypeTag();

	public abstract boolean is2WordType();

	/**
	 * If the type is a basic type, this method normalizes the type and returns
	 * a BasicType object. Otherwise, it returns null.
	 */
	public abstract BasicType isBasicType();

	/**
	 * Returns false if getName() returns a valid type name.
	 */
	public boolean isNullType()
	{
		return false;
	}

	public boolean isUninit()
	{
		return false;
	}

	public TypeData join()
	{
		return new TypeVar(this);
	}

	public abstract void setType(String s, ClassPool cp) throws BadBytecode;

	/**
	 * Returns this if it is a TypeVar or a TypeVar that this type depends on.
	 * Otherwise, this method returns null. It is used by dfs().
	 */
	protected TypeVar toTypeVar()
	{
		return null;
	}
}
