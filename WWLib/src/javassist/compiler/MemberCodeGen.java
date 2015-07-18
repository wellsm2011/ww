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

package javassist.compiler;

import java.util.ArrayList;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.compiler.ast.ASTList;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.ArrayInit;
import javassist.compiler.ast.CallExpr;
import javassist.compiler.ast.Declarator;
import javassist.compiler.ast.Expr;
import javassist.compiler.ast.Keyword;
import javassist.compiler.ast.Member;
import javassist.compiler.ast.MethodDecl;
import javassist.compiler.ast.NewExpr;
import javassist.compiler.ast.Pair;
import javassist.compiler.ast.Stmnt;
import javassist.compiler.ast.Symbol;

/* Code generator methods depending on javassist.* classes.
 */
public class MemberCodeGen extends CodeGen
{
	static class JsrHook extends ReturnHook
	{
		ArrayList	jsrList;
		CodeGen		cgen;
		int			var;

		JsrHook(CodeGen gen)
		{
			super(gen);
			this.jsrList = new ArrayList();
			this.cgen = gen;
			this.var = -1;
		}

		@Override
		protected boolean doit(Bytecode b, int opcode)
		{
			switch (opcode)
			{
				case Opcode.RETURN:
					this.jsrJmp(b);
					break;
				case ARETURN:
					b.addAstore(this.getVar(1));
					this.jsrJmp(b);
					b.addAload(this.var);
					break;
				case IRETURN:
					b.addIstore(this.getVar(1));
					this.jsrJmp(b);
					b.addIload(this.var);
					break;
				case LRETURN:
					b.addLstore(this.getVar(2));
					this.jsrJmp(b);
					b.addLload(this.var);
					break;
				case DRETURN:
					b.addDstore(this.getVar(2));
					this.jsrJmp(b);
					b.addDload(this.var);
					break;
				case FRETURN:
					b.addFstore(this.getVar(1));
					this.jsrJmp(b);
					b.addFload(this.var);
					break;
				default:
					throw new RuntimeException("fatal");
			}

			return false;
		}

		private int getVar(int size)
		{
			if (this.var < 0)
			{
				this.var = this.cgen.getMaxLocals();
				this.cgen.incMaxLocals(size);
			}

			return this.var;
		}

		private void jsrJmp(Bytecode b)
		{
			b.addOpcode(Opcode.GOTO);
			this.jsrList.add(new int[]
			{ b.currentPc(), this.var });
			b.addIndex(0);
		}
	}

	static class JsrHook2 extends ReturnHook
	{
		int	var;
		int	target;

		JsrHook2(CodeGen gen, int[] retTarget)
		{
			super(gen);
			this.target = retTarget[0];
			this.var = retTarget[1];
		}

		@Override
		protected boolean doit(Bytecode b, int opcode)
		{
			switch (opcode)
			{
				case Opcode.RETURN:
					break;
				case ARETURN:
					b.addAstore(this.var);
					break;
				case IRETURN:
					b.addIstore(this.var);
					break;
				case LRETURN:
					b.addLstore(this.var);
					break;
				case DRETURN:
					b.addDstore(this.var);
					break;
				case FRETURN:
					b.addFstore(this.var);
					break;
				default:
					throw new RuntimeException("fatal");
			}

			b.addOpcode(Opcode.GOTO);
			b.addIndex(this.target - b.currentPc() + 3);
			return true;
		}
	}

	private static void badLvalue() throws CompileError
	{
		throw new CompileError("bad l-value");
	}

	private static void badMethod() throws CompileError
	{
		throw new CompileError("bad method");
	}

	private static void badNewExpr() throws CompileError
	{
		throw new CompileError("bad new expression");
	}

	protected MemberResolver resolver;

	protected CtClass thisClass;

	protected MethodInfo thisMethod;

	protected boolean resultStatic;

	public MemberCodeGen(Bytecode b, CtClass cc, ClassPool cp)
	{
		super(b);
		this.resolver = new MemberResolver(cp);
		this.thisClass = cc;
		this.thisMethod = null;
	}

	private int addFieldrefInfo(CtField f, FieldInfo finfo)
	{
		ConstPool cp = this.bytecode.getConstPool();
		String cname = f.getDeclaringClass().getName();
		int ci = cp.addClassInfo(cname);
		String name = finfo.getName();
		String type = finfo.getDescriptor();
		return cp.addFieldrefInfo(ci, name, type);
	}

	/**
	 * Adds a finally clause for earch return statement.
	 */
	private void addFinally(ArrayList returnList, Stmnt finallyBlock) throws CompileError
	{
		Bytecode bc = this.bytecode;
		int n = returnList.size();
		for (int i = 0; i < n; ++i)
		{
			final int[] ret = (int[]) returnList.get(i);
			int pc = ret[0];
			bc.write16bit(pc, bc.currentPc() - pc + 1);
			ReturnHook hook = new JsrHook2(this, ret);
			finallyBlock.accept(this);
			hook.remove(this);
			if (!this.hasReturned)
			{
				bc.addOpcode(Opcode.GOTO);
				bc.addIndex(pc + 3 - bc.currentPc());
			}
		}
	}

	@Override
	public void atArrayInit(ArrayInit init) throws CompileError
	{
		throw new CompileError("array initializer is not supported");
	}

	private void atArrayLength(ASTree expr) throws CompileError
	{
		if (this.arrayDim == 0)
			throw new CompileError(".length applied to a non array");

		this.bytecode.addOpcode(Opcode.ARRAYLENGTH);
		this.exprType = TokenId.INT;
		this.arrayDim = 0;
	}

	@Override
	protected void atArrayVariableAssign(ArrayInit init, int varType, int varArray, String varClass) throws CompileError
	{
		this.atNewArrayExpr2(varType, null, varClass, init);
	}

	@Override
	public void atCallExpr(CallExpr expr) throws CompileError
	{
		String mname = null;
		CtClass targetClass = null;
		ASTree method = expr.oprand1();
		ASTList args = (ASTList) expr.oprand2();
		boolean isStatic = false;
		boolean isSpecial = false;
		int aload0pos = -1;

		MemberResolver.Method cached = expr.getMethod();
		if (method instanceof Member)
		{
			mname = ((Member) method).get();
			targetClass = this.thisClass;
			if (this.inStaticMethod || cached != null && cached.isStatic())
				isStatic = true; // should be static
			else
			{
				aload0pos = this.bytecode.currentPc();
				this.bytecode.addAload(0); // this
			}
		} else if (method instanceof Keyword)
		{ // constructor
			isSpecial = true;
			mname = MethodInfo.nameInit; // <init>
			targetClass = this.thisClass;
			if (this.inStaticMethod)
				throw new CompileError("a constructor cannot be static");
			else
				this.bytecode.addAload(0); // this

			if (((Keyword) method).get() == TokenId.SUPER)
				targetClass = MemberResolver.getSuperclass(targetClass);
		} else if (method instanceof Expr)
		{
			Expr e = (Expr) method;
			mname = ((Symbol) e.oprand2()).get();
			int op = e.getOperator();
			if (op == TokenId.MEMBER)
			{ // static method
				targetClass = this.resolver.lookupClass(((Symbol) e.oprand1()).get(), false);
				isStatic = true;
			} else if (op == '.')
			{
				ASTree target = e.oprand1();
				String classFollowedByDotSuper = TypeChecker.isDotSuper(target);
				if (classFollowedByDotSuper != null)
				{
					isSpecial = true;
					targetClass = MemberResolver.getSuperInterface(this.thisClass, classFollowedByDotSuper);
					if (this.inStaticMethod || cached != null && cached.isStatic())
						isStatic = true; // should be static
					else
					{
						aload0pos = this.bytecode.currentPc();
						this.bytecode.addAload(0); // this
					}
				} else
				{
					if (target instanceof Keyword)
						if (((Keyword) target).get() == TokenId.SUPER)
							isSpecial = true;

					try
					{
						target.accept(this);
					} catch (NoFieldException nfe)
					{
						if (nfe.getExpr() != target)
							throw nfe;

						// it should be a static method.
						this.exprType = TokenId.CLASS;
						this.arrayDim = 0;
						this.className = nfe.getField(); // JVM-internal
						isStatic = true;
					}

					if (this.arrayDim > 0)
						targetClass = this.resolver.lookupClass(CodeGen.javaLangObject, true);
					else if (this.exprType == TokenId.CLASS /*
															 * && arrayDim == 0
															 */)
						targetClass = this.resolver.lookupClassByJvmName(this.className);
					else
						MemberCodeGen.badMethod();
				}
			} else
				MemberCodeGen.badMethod();
		} else
			CodeGen.fatal();

		this.atMethodCallCore(targetClass, mname, args, isStatic, isSpecial, aload0pos, cached);
	}

	@Override
	protected void atClassObject2(String cname) throws CompileError
	{
		if (this.getMajorVersion() < ClassFile.JAVA_5)
			super.atClassObject2(cname);
		else
			this.bytecode.addLdc(this.bytecode.getConstPool().addClassInfo(cname));
	}

	@Override
	protected void atFieldAssign(Expr expr, int op, ASTree left, ASTree right, boolean doDup) throws CompileError
	{
		CtField f = this.fieldAccess(left, false);
		boolean is_static = this.resultStatic;
		if (op != '=' && !is_static)
			this.bytecode.addOpcode(Opcode.DUP);

		int fi;
		if (op == '=')
		{
			FieldInfo finfo = f.getFieldInfo2();
			this.setFieldType(finfo);
			AccessorMaker maker = this.isAccessibleField(f, finfo);
			if (maker == null)
				fi = this.addFieldrefInfo(f, finfo);
			else
				fi = 0;
		} else
			fi = this.atFieldRead(f, is_static);

		int fType = this.exprType;
		int fDim = this.arrayDim;
		String cname = this.className;

		this.atAssignCore(expr, op, right, fType, fDim, cname);

		boolean is2w = CodeGen.is2word(fType, fDim);
		if (doDup)
		{
			int dup_code;
			if (is_static)
				dup_code = is2w ? Opcode.DUP2 : Opcode.DUP;
			else
				dup_code = is2w ? Opcode.DUP2_X1 : Opcode.DUP_X1;

			this.bytecode.addOpcode(dup_code);
		}

		this.atFieldAssignCore(f, is_static, fi, is2w);

		this.exprType = fType;
		this.arrayDim = fDim;
		this.className = cname;
	}

	/*
	 * If fi == 0, the field must be a private field in an enclosing class.
	 */
	private void atFieldAssignCore(CtField f, boolean is_static, int fi, boolean is2byte) throws CompileError
	{
		if (fi != 0)
		{
			if (is_static)
			{
				this.bytecode.add(Opcode.PUTSTATIC);
				this.bytecode.growStack(is2byte ? -2 : -1);
			} else
			{
				this.bytecode.add(Opcode.PUTFIELD);
				this.bytecode.growStack(is2byte ? -3 : -2);
			}

			this.bytecode.addIndex(fi);
		} else
		{
			CtClass declClass = f.getDeclaringClass();
			AccessorMaker maker = declClass.getAccessorMaker();
			// make should be non null.
			FieldInfo finfo = f.getFieldInfo2();
			MethodInfo minfo = maker.getFieldSetter(finfo, is_static);
			this.bytecode.addInvokestatic(declClass, minfo.getName(), minfo.getDescriptor());
		}
	}

	@Override
	protected void atFieldPlusPlus(int token, boolean isPost, ASTree oprand, Expr expr, boolean doDup) throws CompileError
	{
		CtField f = this.fieldAccess(oprand, false);
		boolean is_static = this.resultStatic;
		if (!is_static)
			this.bytecode.addOpcode(Opcode.DUP);

		int fi = this.atFieldRead(f, is_static);
		int t = this.exprType;
		boolean is2w = CodeGen.is2word(t, this.arrayDim);

		int dup_code;
		if (is_static)
			dup_code = is2w ? Opcode.DUP2 : Opcode.DUP;
		else
			dup_code = is2w ? Opcode.DUP2_X1 : Opcode.DUP_X1;

		this.atPlusPlusCore(dup_code, doDup, token, isPost, expr);
		this.atFieldAssignCore(f, is_static, fi, is2w);
	}

	@Override
	protected void atFieldRead(ASTree expr) throws CompileError
	{
		CtField f = this.fieldAccess(expr, true);
		if (f == null)
		{
			this.atArrayLength(expr);
			return;
		}

		boolean is_static = this.resultStatic;
		ASTree cexpr = TypeChecker.getConstantFieldValue(f);
		if (cexpr == null)
			this.atFieldRead(f, is_static);
		else
		{
			cexpr.accept(this);
			this.setFieldType(f.getFieldInfo2());
		}
	}

	/**
	 * Generates bytecode for reading a field value. It returns a fieldref_info
	 * index or zero if the field is a private one declared in an enclosing
	 * class.
	 */
	private int atFieldRead(CtField f, boolean isStatic) throws CompileError
	{
		FieldInfo finfo = f.getFieldInfo2();
		boolean is2byte = this.setFieldType(finfo);
		AccessorMaker maker = this.isAccessibleField(f, finfo);
		if (maker != null)
		{
			MethodInfo minfo = maker.getFieldGetter(finfo, isStatic);
			this.bytecode.addInvokestatic(f.getDeclaringClass(), minfo.getName(), minfo.getDescriptor());
			return 0;
		} else
		{
			int fi = this.addFieldrefInfo(f, finfo);
			if (isStatic)
			{
				this.bytecode.add(Opcode.GETSTATIC);
				this.bytecode.growStack(is2byte ? 2 : 1);
			} else
			{
				this.bytecode.add(Opcode.GETFIELD);
				this.bytecode.growStack(is2byte ? 1 : 0);
			}

			this.bytecode.addIndex(fi);
			return fi;
		}
	}

	/*
	 * overwritten in JvstCodeGen.
	 */
	@Override
	public void atMember(Member mem) throws CompileError
	{
		this.atFieldRead(mem);
	}

	public void atMethodArgs(ASTList args, int[] types, int[] dims, String[] cnames) throws CompileError
	{
		int i = 0;
		while (args != null)
		{
			ASTree a = args.head();
			a.accept(this);
			types[i] = this.exprType;
			dims[i] = this.arrayDim;
			cnames[i] = this.className;
			++i;
			args = args.tail();
		}
	}

	/*
	 * atMethodCallCore() is also called by doit() in NewExpr.ProceedForNew
	 * @param targetClass the class at which method lookup starts.
	 * @param found not null if the method look has been already done.
	 */
	public void atMethodCallCore(CtClass targetClass, String mname, ASTList args, boolean isStatic, boolean isSpecial, int aload0pos, MemberResolver.Method found) throws CompileError
	{
		int nargs = this.getMethodArgsLength(args);
		int[] types = new int[nargs];
		int[] dims = new int[nargs];
		String[] cnames = new String[nargs];

		if (!isStatic && found != null && found.isStatic())
		{
			this.bytecode.addOpcode(Opcode.POP);
			isStatic = true;
		}

		int stack = this.bytecode.getStackDepth();

		// generate code for evaluating arguments.
		this.atMethodArgs(args, types, dims, cnames);

		if (found == null)
			found = this.resolver.lookupMethod(targetClass, this.thisClass, this.thisMethod, mname, types, dims, cnames);

		if (found == null)
		{
			String msg;
			if (mname.equals(MethodInfo.nameInit))
				msg = "constructor not found";
			else
				msg = "Method " + mname + " not found in " + targetClass.getName();

			throw new CompileError(msg);
		}

		this.atMethodCallCore2(targetClass, mname, isStatic, isSpecial, aload0pos, found);
	}

	private void atMethodCallCore2(CtClass targetClass, String mname, boolean isStatic, boolean isSpecial, int aload0pos, MemberResolver.Method found) throws CompileError
	{
		CtClass declClass = found.declaring;
		MethodInfo minfo = found.info;
		String desc = minfo.getDescriptor();
		int acc = minfo.getAccessFlags();

		if (mname.equals(MethodInfo.nameInit))
		{
			isSpecial = true;
			if (declClass != targetClass)
				throw new CompileError("no such constructor: " + targetClass.getName());

			if (declClass != this.thisClass && AccessFlag.isPrivate(acc))
			{
				desc = this.getAccessibleConstructor(desc, declClass, minfo);
				this.bytecode.addOpcode(Opcode.ACONST_NULL); // the last
				// parameter
			}
		} else if (AccessFlag.isPrivate(acc))
			if (declClass == this.thisClass)
				isSpecial = true;
			else
			{
				isSpecial = false;
				isStatic = true;
				String origDesc = desc;
				if ((acc & AccessFlag.STATIC) == 0)
					desc = Descriptor.insertParameter(declClass.getName(), origDesc);

				acc = AccessFlag.setPackage(acc) | AccessFlag.STATIC;
				mname = this.getAccessiblePrivate(mname, origDesc, desc, minfo, declClass);
			}

		boolean popTarget = false;
		if ((acc & AccessFlag.STATIC) != 0)
		{
			if (!isStatic)
			{
				/*
				 * this method is static but the target object is on stack. It
				 * must be popped out. If aload0pos >= 0, then the target object
				 * was pushed by aload_0. It is overwritten by NOP.
				 */
				isStatic = true;
				if (aload0pos >= 0)
					this.bytecode.write(aload0pos, Opcode.NOP);
				else
					popTarget = true;
			}

			this.bytecode.addInvokestatic(declClass, mname, desc);
		} else if (isSpecial)   // if (isSpecial && notStatic(acc))
			this.bytecode.addInvokespecial(declClass, mname, desc);
		else
		{
			if (!Modifier.isPublic(declClass.getModifiers()) || declClass.isInterface() != targetClass.isInterface())
				declClass = targetClass;

			if (declClass.isInterface())
			{
				int nargs = Descriptor.paramSize(desc) + 1;
				this.bytecode.addInvokeinterface(declClass, mname, desc, nargs);
			} else if (isStatic)
				throw new CompileError(mname + " is not static");
			else
				this.bytecode.addInvokevirtual(declClass, mname, desc);
		}

		this.setReturnType(desc, isStatic, popTarget);
	}

	protected void atMultiNewArray(int type, ASTList classname, ASTList size) throws CompileError
	{
		int count, dim;
		dim = size.length();
		for (count = 0; size != null; size = size.tail())
		{
			ASTree s = size.head();
			if (s == null)
				break; // int[][][] a = new int[3][4][];

			++count;
			s.accept(this);
			if (this.exprType != TokenId.INT)
				throw new CompileError("bad type for array size");
		}

		String desc;
		this.exprType = type;
		this.arrayDim = dim;
		if (type == TokenId.CLASS)
		{
			this.className = this.resolveClassName(classname);
			desc = CodeGen.toJvmArrayName(this.className, dim);
		} else
			desc = CodeGen.toJvmTypeName(type, dim);

		this.bytecode.addMultiNewarray(desc, count);
	}

	public void atNewArrayExpr(NewExpr expr) throws CompileError
	{
		int type = expr.getArrayType();
		ASTList size = expr.getArraySize();
		ASTList classname = expr.getClassName();
		ArrayInit init = expr.getInitializer();
		if (size.length() > 1)
		{
			if (init != null)
				throw new CompileError("sorry, multi-dimensional array initializer " + "for new is not supported");

			this.atMultiNewArray(type, classname, size);
			return;
		}

		ASTree sizeExpr = size.head();
		this.atNewArrayExpr2(type, sizeExpr, Declarator.astToClassName(classname, '/'), init);
	}

	private void atNewArrayExpr2(int type, ASTree sizeExpr, String jvmClassname, ArrayInit init) throws CompileError
	{
		if (init == null)
			if (sizeExpr == null)
				throw new CompileError("no array size");
			else
				sizeExpr.accept(this);
		else if (sizeExpr == null)
		{
			int s = init.length();
			this.bytecode.addIconst(s);
		} else
			throw new CompileError("unnecessary array size specified for new");

		String elementClass;
		if (type == TokenId.CLASS)
		{
			elementClass = this.resolveClassName(jvmClassname);
			this.bytecode.addAnewarray(MemberResolver.jvmToJavaName(elementClass));
		} else
		{
			elementClass = null;
			int atype = 0;
			switch (type)
			{
				case BOOLEAN:
					atype = Opcode.T_BOOLEAN;
					break;
				case CHAR:
					atype = Opcode.T_CHAR;
					break;
				case FLOAT:
					atype = Opcode.T_FLOAT;
					break;
				case DOUBLE:
					atype = Opcode.T_DOUBLE;
					break;
				case BYTE:
					atype = Opcode.T_BYTE;
					break;
				case SHORT:
					atype = Opcode.T_SHORT;
					break;
				case INT:
					atype = Opcode.T_INT;
					break;
				case LONG:
					atype = Opcode.T_LONG;
					break;
				default:
					MemberCodeGen.badNewExpr();
					break;
			}

			this.bytecode.addOpcode(Opcode.NEWARRAY);
			this.bytecode.add(atype);
		}

		if (init != null)
		{
			int s = init.length();
			ASTList list = init;
			for (int i = 0; i < s; i++)
			{
				this.bytecode.addOpcode(Opcode.DUP);
				this.bytecode.addIconst(i);
				list.head().accept(this);
				if (!CodeGen.isRefType(type))
					this.atNumCastExpr(this.exprType, type);

				this.bytecode.addOpcode(CodeGen.getArrayWriteOp(type, 0));
				list = list.tail();
			}
		}

		this.exprType = type;
		this.arrayDim = 1;
		this.className = elementClass;
	}

	@Override
	public void atNewExpr(NewExpr expr) throws CompileError
	{
		if (expr.isArray())
			this.atNewArrayExpr(expr);
		else
		{
			CtClass clazz = this.resolver.lookupClassByName(expr.getClassName());
			String cname = clazz.getName();
			ASTList args = expr.getArguments();
			this.bytecode.addNew(cname);
			this.bytecode.addOpcode(Opcode.DUP);

			this.atMethodCallCore(clazz, MethodInfo.nameInit, args, false, true, -1, null);

			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = MemberResolver.javaToJvmName(cname);
		}
	}

	@Override
	protected void atTryStmnt(Stmnt st) throws CompileError
	{
		Bytecode bc = this.bytecode;
		Stmnt body = (Stmnt) st.getLeft();
		if (body == null)
			return;

		ASTList catchList = (ASTList) st.getRight().getLeft();
		Stmnt finallyBlock = (Stmnt) st.getRight().getRight().getLeft();
		ArrayList gotoList = new ArrayList();

		JsrHook jsrHook = null;
		if (finallyBlock != null)
			jsrHook = new JsrHook(this);

		int start = bc.currentPc();
		body.accept(this);
		int end = bc.currentPc();
		if (start == end)
			throw new CompileError("empty try block");

		boolean tryNotReturn = !this.hasReturned;
		if (tryNotReturn)
		{
			bc.addOpcode(Opcode.GOTO);
			gotoList.add(new Integer(bc.currentPc()));
			bc.addIndex(0); // correct later
		}

		int var = this.getMaxLocals();
		this.incMaxLocals(1);
		while (catchList != null)
		{
			// catch clause
			Pair p = (Pair) catchList.head();
			catchList = catchList.tail();
			Declarator decl = (Declarator) p.getLeft();
			Stmnt block = (Stmnt) p.getRight();

			decl.setLocalVar(var);

			CtClass type = this.resolver.lookupClassByJvmName(decl.getClassName());
			decl.setClassName(MemberResolver.javaToJvmName(type.getName()));
			bc.addExceptionHandler(start, end, bc.currentPc(), type);
			bc.growStack(1);
			bc.addAstore(var);
			this.hasReturned = false;
			if (block != null)
				block.accept(this);

			if (!this.hasReturned)
			{
				bc.addOpcode(Opcode.GOTO);
				gotoList.add(new Integer(bc.currentPc()));
				bc.addIndex(0); // correct later
				tryNotReturn = true;
			}
		}

		if (finallyBlock != null)
		{
			jsrHook.remove(this);
			// catch (any) clause
			int pcAnyCatch = bc.currentPc();
			bc.addExceptionHandler(start, pcAnyCatch, pcAnyCatch, 0);
			bc.growStack(1);
			bc.addAstore(var);
			this.hasReturned = false;
			finallyBlock.accept(this);
			if (!this.hasReturned)
			{
				bc.addAload(var);
				bc.addOpcode(Opcode.ATHROW);
			}

			this.addFinally(jsrHook.jsrList, finallyBlock);
		}

		int pcEnd = bc.currentPc();
		this.patchGoto(gotoList, pcEnd);
		this.hasReturned = !tryNotReturn;
		if (finallyBlock != null)
			if (tryNotReturn)
				finallyBlock.accept(this);
	}

	/*
	 * This method also returns a value in resultStatic.
	 * @param acceptLength true if array length is acceptable
	 */
	protected CtField fieldAccess(ASTree expr, boolean acceptLength) throws CompileError
	{
		if (expr instanceof Member)
		{
			String name = ((Member) expr).get();
			CtField f = null;
			try
			{
				f = this.thisClass.getField(name);
			} catch (NotFoundException e)
			{
				// EXPR might be part of a static member access?
				throw new NoFieldException(name, expr);
			}

			boolean is_static = Modifier.isStatic(f.getModifiers());
			if (!is_static)
				if (this.inStaticMethod)
					throw new CompileError("not available in a static method: " + name);
				else
					this.bytecode.addAload(0); // this

			this.resultStatic = is_static;
			return f;
		} else if (expr instanceof Expr)
		{
			Expr e = (Expr) expr;
			int op = e.getOperator();
			if (op == TokenId.MEMBER)
			{
				/*
				 * static member by # (extension by Javassist) For example, if
				 * int.class is parsed, the resulting tree is (#
				 * "java.lang.Integer" "TYPE").
				 */
				CtField f = this.resolver.lookupField(((Symbol) e.oprand1()).get(), (Symbol) e.oprand2());
				this.resultStatic = true;
				return f;
			} else if (op == '.')
			{
				CtField f = null;
				try
				{
					e.oprand1().accept(this);
					/*
					 * Don't call lookupFieldByJvmName2(). The left operand of .
					 * is not a class name but a normal expression.
					 */
					if (this.exprType == TokenId.CLASS && this.arrayDim == 0)
						f = this.resolver.lookupFieldByJvmName(this.className, (Symbol) e.oprand2());
					else if (acceptLength && this.arrayDim > 0 && ((Symbol) e.oprand2()).get().equals("length"))
						return null; // expr is an array length.
					else
						MemberCodeGen.badLvalue();

					boolean is_static = Modifier.isStatic(f.getModifiers());
					if (is_static)
						this.bytecode.addOpcode(Opcode.POP);

					this.resultStatic = is_static;
					return f;
				} catch (NoFieldException nfe)
				{
					if (nfe.getExpr() != e.oprand1())
						throw nfe;

					/*
					 * EXPR should be a static field. If EXPR might be part of a
					 * qualified class name, lookupFieldByJvmName2() throws
					 * NoFieldException.
					 */
					Symbol fname = (Symbol) e.oprand2();
					String cname = nfe.getField();
					f = this.resolver.lookupFieldByJvmName2(cname, fname, expr);
					this.resultStatic = true;
					return f;
				}
			} else
				MemberCodeGen.badLvalue();
		} else
			MemberCodeGen.badLvalue();

		this.resultStatic = false;
		return null; // never reach
	}

	/*
	 * Finds (or adds if necessary) a hidden constructor if the given
	 * constructor is in an enclosing class.
	 * @param desc the descriptor of the constructor.
	 * @param declClass the class declaring the constructor.
	 * @param minfo the method info of the constructor.
	 * @return the descriptor of the hidden constructor.
	 */
	protected String getAccessibleConstructor(String desc, CtClass declClass, MethodInfo minfo) throws CompileError
	{
		if (this.isEnclosing(declClass, this.thisClass))
		{
			AccessorMaker maker = declClass.getAccessorMaker();
			if (maker != null)
				return maker.getConstructor(declClass, desc, minfo);
		}

		throw new CompileError("the called constructor is private in " + declClass.getName());
	}

	/*
	 * Finds (or adds if necessary) a hidden accessor if the method is in an
	 * enclosing class.
	 * @param desc the descriptor of the method.
	 * @param declClass the class declaring the method.
	 */
	protected String getAccessiblePrivate(String methodName, String desc, String newDesc, MethodInfo minfo, CtClass declClass) throws CompileError
	{
		if (this.isEnclosing(declClass, this.thisClass))
		{
			AccessorMaker maker = declClass.getAccessorMaker();
			if (maker != null)
				return maker.getMethodAccessor(methodName, desc, newDesc, minfo);
		}

		throw new CompileError("Method " + methodName + " is private");
	}

	/**
	 * Returns the major version of the class file targeted by this compilation.
	 */
	public int getMajorVersion()
	{
		ClassFile cf = this.thisClass.getClassFile2();
		if (cf == null)
			return ClassFile.MAJOR_VERSION; // JDK 1.3
		else
			return cf.getMajorVersion();
	}

	public int getMethodArgsLength(ASTList args)
	{
		return ASTList.length(args);
	}

	/**
	 * Returns the JVM-internal representation of this super class name.
	 */
	@Override
	protected String getSuperName() throws CompileError
	{
		return MemberResolver.javaToJvmName(MemberResolver.getSuperclass(this.thisClass).getName());
	}

	public CtClass getThisClass()
	{
		return this.thisClass;
	}

	/**
	 * Returns the JVM-internal representation of this class name.
	 */
	@Override
	protected String getThisName()
	{
		return MemberResolver.javaToJvmName(this.thisClass.getName());
	}

	@Override
	protected void insertDefaultSuperCall() throws CompileError
	{
		this.bytecode.addAload(0);
		this.bytecode.addInvokespecial(MemberResolver.getSuperclass(this.thisClass), "<init>", "()V");
	}

	/**
	 * Returns null if the field is accessible. Otherwise, it throws an
	 * exception or it returns AccessorMaker if the field is a private one
	 * declared in an enclosing class.
	 */
	private AccessorMaker isAccessibleField(CtField f, FieldInfo finfo) throws CompileError
	{
		if (AccessFlag.isPrivate(finfo.getAccessFlags()) && f.getDeclaringClass() != this.thisClass)
		{
			CtClass declClass = f.getDeclaringClass();
			if (this.isEnclosing(declClass, this.thisClass))
			{
				AccessorMaker maker = declClass.getAccessorMaker();
				if (maker != null)
					return maker;
				else
					throw new CompileError("fatal error.  bug?");
			} else
				throw new CompileError("Field " + f.getName() + " in " + declClass.getName() + " is private.");
		}

		return null; // accessible field
	}

	private boolean isEnclosing(CtClass outer, CtClass inner)
	{
		try
		{
			while (inner != null)
			{
				inner = inner.getDeclaringClass();
				if (inner == outer)
					return true;
			}
		} catch (NotFoundException e)
		{
		}
		return false;
	}

	public CtClass[] makeParamList(MethodDecl md) throws CompileError
	{
		CtClass[] params;
		ASTList plist = md.getParams();
		if (plist == null)
			params = new CtClass[0];
		else
		{
			int i = 0;
			params = new CtClass[plist.length()];
			while (plist != null)
			{
				params[i++] = this.resolver.lookupClass((Declarator) plist.head());
				plist = plist.tail();
			}
		}

		return params;
	}

	public CtClass[] makeThrowsList(MethodDecl md) throws CompileError
	{
		CtClass[] clist;
		ASTList list = md.getThrows();
		if (list == null)
			return null;
		else
		{
			int i = 0;
			clist = new CtClass[list.length()];
			while (list != null)
			{
				clist[i++] = this.resolver.lookupClassByName((ASTList) list.head());
				list = list.tail();
			}

			return clist;
		}
	}

	/*
	 * Converts a class name into a JVM-internal representation. It may also
	 * expand a simple class name to java.lang.*. For example, this converts
	 * Object into java/lang/Object.
	 */
	@Override
	protected String resolveClassName(ASTList name) throws CompileError
	{
		return this.resolver.resolveClassName(name);
	}

	/*
	 * Expands a simple class name to java.lang.*. For example, this converts
	 * Object into java/lang/Object.
	 */
	@Override
	protected String resolveClassName(String jvmName) throws CompileError
	{
		return this.resolver.resolveJvmClassName(jvmName);
	}

	/**
	 * Sets exprType, arrayDim, and className.
	 *
	 * @return true if the field type is long or double.
	 */
	private boolean setFieldType(FieldInfo finfo) throws CompileError
	{
		String type = finfo.getDescriptor();

		int i = 0;
		int dim = 0;
		char c = type.charAt(i);
		while (c == '[')
		{
			++dim;
			c = type.charAt(++i);
		}

		this.arrayDim = dim;
		this.exprType = MemberResolver.descToType(c);

		if (c == 'L')
			this.className = type.substring(i + 1, type.indexOf(';', i + 1));
		else
			this.className = null;

		boolean is2byte = dim == 0 && (c == 'J' || c == 'D');
		return is2byte;
	}

	void setReturnType(String desc, boolean isStatic, boolean popTarget) throws CompileError
	{
		int i = desc.indexOf(')');
		if (i < 0)
			MemberCodeGen.badMethod();

		char c = desc.charAt(++i);
		int dim = 0;
		while (c == '[')
		{
			++dim;
			c = desc.charAt(++i);
		}

		this.arrayDim = dim;
		if (c == 'L')
		{
			int j = desc.indexOf(';', i + 1);
			if (j < 0)
				MemberCodeGen.badMethod();

			this.exprType = TokenId.CLASS;
			this.className = desc.substring(i + 1, j);
		} else
		{
			this.exprType = MemberResolver.descToType(c);
			this.className = null;
		}

		int etype = this.exprType;
		if (isStatic)
			if (popTarget)
				if (CodeGen.is2word(etype, dim))
				{
					this.bytecode.addOpcode(Opcode.DUP2_X1);
					this.bytecode.addOpcode(Opcode.POP2);
					this.bytecode.addOpcode(Opcode.POP);
				} else if (etype == TokenId.VOID)
					this.bytecode.addOpcode(Opcode.POP);
				else
				{
					this.bytecode.addOpcode(Opcode.SWAP);
					this.bytecode.addOpcode(Opcode.POP);
				}
	}

	/**
	 * Records the currently compiled method.
	 */
	public void setThisMethod(CtMethod m)
	{
		this.thisMethod = m.getMethodInfo2();
		if (this.typeChecker != null)
			this.typeChecker.setThisMethod(this.thisMethod);
	}
}
