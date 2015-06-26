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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Bytecode;
import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;
import javassist.compiler.ast.ASTList;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.CallExpr;
import javassist.compiler.ast.CastExpr;
import javassist.compiler.ast.Declarator;
import javassist.compiler.ast.Expr;
import javassist.compiler.ast.Member;
import javassist.compiler.ast.Stmnt;
import javassist.compiler.ast.Symbol;

/* Code generator accepting extended Java syntax for Javassist.
 */

public class JvstCodeGen extends MemberCodeGen
{
	public static final String	sigName			= "$sig";
	public static final String	dollarTypeName	= "$type";
	public static final String	clazzName		= "$class";
	public static final String	wrapperCastName	= "$w";
	public static final String	cflowName		= "$cflow";

	/*
	 * compileParameterList() returns the stack size used by the produced code.
	 * This method correctly computes the max_stack value.
	 * @param regno the index of the local variable in which the first argument
	 * is received. (0: static method, 1: regular method.)
	 */
	public static int compileParameterList(Bytecode code, CtClass[] params, int regno)
	{
		if (params == null)
		{
			code.addIconst(0); // iconst_0
			code.addAnewarray(CodeGen.javaLangObject); // anewarray Object
			return 1;
		} else
		{
			CtClass[] args = new CtClass[1];
			int n = params.length;
			code.addIconst(n); // iconst_<n>
			code.addAnewarray(CodeGen.javaLangObject); // anewarray Object
			for (int i = 0; i < n; ++i)
			{
				code.addOpcode(Opcode.DUP); // dup
				code.addIconst(i); // iconst_<i>
				if (params[i].isPrimitive())
				{
					CtPrimitiveType pt = (CtPrimitiveType) params[i];
					String wrapper = pt.getWrapperName();
					code.addNew(wrapper); // new <wrapper>
					code.addOpcode(Opcode.DUP); // dup
					int s = code.addLoad(regno, pt); // ?load <regno>
					regno += s;
					args[0] = pt;
					code.addInvokespecial(wrapper, "<init>", Descriptor.ofMethod(CtClass.voidType, args));
					// invokespecial
				} else
				{
					code.addAload(regno); // aload <regno>
					++regno;
				}

				code.addOpcode(Opcode.AASTORE); // aastore
			}

			return 8;
		}
	}

	/*
	 * Syntax: <cflow> : $cflow '(' <cflow name> ')' <cflow name> : <identifier>
	 * ('.' <identifier>)*
	 */
	private static void makeCflowName(StringBuffer sbuf, ASTree name) throws CompileError
	{
		if (name instanceof Symbol)
		{
			sbuf.append(((Symbol) name).get());
			return;
		} else if (name instanceof Expr)
		{
			Expr expr = (Expr) name;
			if (expr.getOperator() == '.')
			{
				JvstCodeGen.makeCflowName(sbuf, expr.oprand1());
				sbuf.append('.');
				JvstCodeGen.makeCflowName(sbuf, expr.oprand2());
				return;
			}
		}

		throw new CompileError("bad " + JvstCodeGen.cflowName);
	}

	String			paramArrayName	= null;
	String			paramListName	= null;
	CtClass[]		paramTypeList	= null;
	private int		paramVarBase	= 0;		// variable index for $0 or $1.
	private boolean	useParam0		= false;	// true if $0 is used.
	private String	param0Type		= null;	// JVM name
	private CtClass	dollarType		= null;
	CtClass			returnType		= null;
	String			returnCastName	= null;
	private String	returnVarName	= null;	// null if $_ is not used.

	String			proceedName		= null;

	ProceedHandler	procHandler		= null;	// null if not used.

	public JvstCodeGen(Bytecode b, CtClass cc, ClassPool cp)
	{
		super(b, cc, cp);
		this.setTypeChecker(new JvstTypeChecker(cc, cp, this));
	}

	/*
	 * If the type of the expression compiled last is void, add ACONST_NULL and
	 * change exprType, arrayDim, className.
	 */
	public void addNullIfVoid()
	{
		if (this.exprType == TokenId.VOID)
		{
			this.bytecode.addOpcode(Opcode.ACONST_NULL);
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = CodeGen.jvmJavaLangObject;
		}
	}

	protected void atAssignParamList(CtClass[] params, Bytecode code) throws CompileError
	{
		if (params == null)
			return;

		int varNo = this.indexOfParam1();
		int n = params.length;
		for (int i = 0; i < n; ++i)
		{
			code.addOpcode(Opcode.DUP);
			code.addIconst(i);
			code.addOpcode(Opcode.AALOAD);
			this.compileUnwrapValue(params[i], code);
			code.addStore(varNo, params[i]);
			varNo += CodeGen.is2word(this.exprType, this.arrayDim) ? 2 : 1;
		}
	}

	/*
	 * Delegates to a ProcHandler object if the method call is $proceed(). It
	 * may process $cflow().
	 */
	@Override
	public void atCallExpr(CallExpr expr) throws CompileError
	{
		ASTree method = expr.oprand1();
		if (method instanceof Member)
		{
			String name = ((Member) method).get();
			if (this.procHandler != null && name.equals(this.proceedName))
			{
				this.procHandler.doit(this, this.bytecode, (ASTList) expr.oprand2());
				return;
			} else if (name.equals(JvstCodeGen.cflowName))
			{
				this.atCflow((ASTList) expr.oprand2());
				return;
			}
		}

		super.atCallExpr(expr);
	}

	@Override
	public void atCastExpr(CastExpr expr) throws CompileError
	{
		ASTList classname = expr.getClassName();
		if (classname != null && expr.getArrayDim() == 0)
		{
			ASTree p = classname.head();
			if (p instanceof Symbol && classname.tail() == null)
			{
				String typename = ((Symbol) p).get();
				if (typename.equals(this.returnCastName))
				{
					this.atCastToRtype(expr);
					return;
				} else if (typename.equals(JvstCodeGen.wrapperCastName))
				{
					this.atCastToWrapper(expr);
					return;
				}
			}
		}

		super.atCastExpr(expr);
	}

	/**
	 * Inserts a cast operator to the return type. If the return type is void,
	 * this does nothing.
	 */
	protected void atCastToRtype(CastExpr expr) throws CompileError
	{
		expr.getOprand().accept(this);
		if (this.exprType == TokenId.VOID || CodeGen.isRefType(this.exprType) || this.arrayDim > 0)
			this.compileUnwrapValue(this.returnType, this.bytecode);
		else if (this.returnType instanceof CtPrimitiveType)
		{
			CtPrimitiveType pt = (CtPrimitiveType) this.returnType;
			int destType = MemberResolver.descToType(pt.getDescriptor());
			this.atNumCastExpr(this.exprType, destType);
			this.exprType = destType;
			this.arrayDim = 0;
			this.className = null;
		} else
			throw new CompileError("invalid cast");
	}

	protected void atCastToWrapper(CastExpr expr) throws CompileError
	{
		expr.getOprand().accept(this);
		if (CodeGen.isRefType(this.exprType) || this.arrayDim > 0)
			return; // Object type. do nothing.

		CtClass clazz = this.resolver.lookupClass(this.exprType, this.arrayDim, this.className);
		if (clazz instanceof CtPrimitiveType)
		{
			CtPrimitiveType pt = (CtPrimitiveType) clazz;
			String wrapper = pt.getWrapperName();
			this.bytecode.addNew(wrapper); // new <wrapper>
			this.bytecode.addOpcode(Opcode.DUP); // dup
			if (pt.getDataSize() > 1)
				this.bytecode.addOpcode(Opcode.DUP2_X2); // dup2_x2
			else
				this.bytecode.addOpcode(Opcode.DUP2_X1); // dup2_x1

			this.bytecode.addOpcode(Opcode.POP2); // pop2
			this.bytecode.addInvokespecial(wrapper, "<init>", "(" + pt.getDescriptor() + ")V");
			// invokespecial
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = CodeGen.jvmJavaLangObject;
		}
	}

	/*
	 * To support $cflow().
	 */
	protected void atCflow(ASTList cname) throws CompileError
	{
		StringBuffer sbuf = new StringBuffer();
		if (cname == null || cname.tail() != null)
			throw new CompileError("bad " + JvstCodeGen.cflowName);

		JvstCodeGen.makeCflowName(sbuf, cname.head());
		String name = sbuf.toString();
		Object[] names = this.resolver.getClassPool().lookupCflow(name);
		if (names == null)
			throw new CompileError("no such " + JvstCodeGen.cflowName + ": " + name);

		this.bytecode.addGetstatic((String) names[0], (String) names[1], "Ljavassist/runtime/Cflow;");
		this.bytecode.addInvokevirtual("javassist.runtime.Cflow", "value", "()I");
		this.exprType = TokenId.INT;
		this.arrayDim = 0;
		this.className = null;
	}

	@Override
	protected void atFieldAssign(Expr expr, int op, ASTree left, ASTree right, boolean doDup) throws CompileError
	{
		if (left instanceof Member && ((Member) left).get().equals(this.paramArrayName))
		{
			if (op != '=')
				throw new CompileError("bad operator for " + this.paramArrayName);

			right.accept(this);
			if (this.arrayDim != 1 || this.exprType != TokenId.CLASS)
				throw new CompileError("invalid type for " + this.paramArrayName);

			this.atAssignParamList(this.paramTypeList, this.bytecode);
			if (!doDup)
				this.bytecode.addOpcode(Opcode.POP);
		} else
			super.atFieldAssign(expr, op, left, right, doDup);
	}

	/*
	 * To support $args, $sig, and $type. $args is an array of parameter list.
	 */
	@Override
	public void atMember(Member mem) throws CompileError
	{
		String name = mem.get();
		if (name.equals(this.paramArrayName))
		{
			JvstCodeGen.compileParameterList(this.bytecode, this.paramTypeList, this.indexOfParam1());
			this.exprType = TokenId.CLASS;
			this.arrayDim = 1;
			this.className = CodeGen.jvmJavaLangObject;
		} else if (name.equals(JvstCodeGen.sigName))
		{
			this.bytecode.addLdc(Descriptor.ofMethod(this.returnType, this.paramTypeList));
			this.bytecode.addInvokestatic("javassist/runtime/Desc", "getParams", "(Ljava/lang/String;)[Ljava/lang/Class;");
			this.exprType = TokenId.CLASS;
			this.arrayDim = 1;
			this.className = "java/lang/Class";
		} else if (name.equals(JvstCodeGen.dollarTypeName))
		{
			if (this.dollarType == null)
				throw new CompileError(JvstCodeGen.dollarTypeName + " is not available");

			this.bytecode.addLdc(Descriptor.of(this.dollarType));
			this.callGetType("getType");
		} else if (name.equals(JvstCodeGen.clazzName))
		{
			if (this.param0Type == null)
				throw new CompileError(JvstCodeGen.clazzName + " is not available");

			this.bytecode.addLdc(this.param0Type);
			this.callGetType("getClazz");
		} else
			super.atMember(mem);
	}

	@Override
	public void atMethodArgs(ASTList args, int[] types, int[] dims, String[] cnames) throws CompileError
	{
		CtClass[] params = this.paramTypeList;
		String pname = this.paramListName;
		int i = 0;
		while (args != null)
		{
			ASTree a = args.head();
			if (a instanceof Member && ((Member) a).get().equals(pname))
			{
				if (params != null)
				{
					int n = params.length;
					int regno = this.indexOfParam1();
					for (int k = 0; k < n; ++k)
					{
						CtClass p = params[k];
						regno += this.bytecode.addLoad(regno, p);
						this.setType(p);
						types[i] = this.exprType;
						dims[i] = this.arrayDim;
						cnames[i] = this.className;
						++i;
					}
				}
			} else
			{
				a.accept(this);
				types[i] = this.exprType;
				dims[i] = this.arrayDim;
				cnames[i] = this.className;
				++i;
			}

			args = args.tail();
		}
	}

	/*
	 * Makes it valid to write "return <expr>;" for a void method.
	 */
	@Override
	protected void atReturnStmnt(Stmnt st) throws CompileError
	{
		ASTree result = st.getLeft();
		if (result != null && this.returnType == CtClass.voidType)
		{
			this.compileExpr(result);
			if (CodeGen.is2word(this.exprType, this.arrayDim))
				this.bytecode.addOpcode(Opcode.POP2);
			else if (this.exprType != TokenId.VOID)
				this.bytecode.addOpcode(Opcode.POP);

			result = null;
		}

		this.atReturnStmnt2(result);
	}

	private void callGetType(String method)
	{
		this.bytecode.addInvokestatic("javassist/runtime/Desc", method, "(Ljava/lang/String;)Ljava/lang/Class;");
		this.exprType = TokenId.CLASS;
		this.arrayDim = 0;
		this.className = "java/lang/Class";
	}

	/*
	 * public int getMethodArgsLength(ASTList args) { if
	 * (!isParamListName(args)) return super.getMethodArgsLength(args); return
	 * paramTypeList.length; }
	 */

	/*
	 * called by Javac#recordSpecialProceed().
	 */
	void compileInvokeSpecial(ASTree target, String classname, String methodname, String descriptor, ASTList args) throws CompileError
	{
		target.accept(this);
		int nargs = this.getMethodArgsLength(args);
		this.atMethodArgs(args, new int[nargs], new int[nargs], new String[nargs]);
		this.bytecode.addInvokespecial(classname, methodname, descriptor);
		this.setReturnType(descriptor, false, false);
		this.addNullIfVoid();
	}

	protected void compileUnwrapValue(CtClass type, Bytecode code) throws CompileError
	{
		if (type == CtClass.voidType)
		{
			this.addNullIfVoid();
			return;
		}

		if (this.exprType == TokenId.VOID)
			throw new CompileError("invalid type for " + this.returnCastName);

		if (type instanceof CtPrimitiveType)
		{
			CtPrimitiveType pt = (CtPrimitiveType) type;
			// pt is not voidType.
			String wrapper = pt.getWrapperName();
			code.addCheckcast(wrapper);
			code.addInvokevirtual(wrapper, pt.getGetMethodName(), pt.getGetMethodDescriptor());
			this.setType(type);
		} else
		{
			code.addCheckcast(type);
			this.setType(type);
		}
	}

	/*
	 * public void atMethodArgs(ASTList args, int[] types, int[] dims, String[]
	 * cnames) throws CompileError { if (!isParamListName(args)) {
	 * super.atMethodArgs(args, types, dims, cnames); return; } CtClass[] params
	 * = paramTypeList; if (params == null) return; int n = params.length; int
	 * regno = indexOfParam1(); for (int i = 0; i < n; ++i) { CtClass p =
	 * params[i]; regno += bytecode.addLoad(regno, p); setType(p); types[i] =
	 * exprType; dims[i] = arrayDim; cnames[i] = className; } }
	 */

	/*
	 * Performs implicit coercion from exprType to type.
	 */
	public void doNumCast(CtClass type) throws CompileError
	{
		if (this.arrayDim == 0 && !CodeGen.isRefType(this.exprType))
			if (type instanceof CtPrimitiveType)
			{
				CtPrimitiveType pt = (CtPrimitiveType) type;
				this.atNumCastExpr(this.exprType, MemberResolver.descToType(pt.getDescriptor()));
			} else
				throw new CompileError("type mismatch");
	}

	@Override
	public int getMethodArgsLength(ASTList args)
	{
		String pname = this.paramListName;
		int n = 0;
		while (args != null)
		{
			ASTree a = args.head();
			if (a instanceof Member && ((Member) a).get().equals(pname))
			{
				if (this.paramTypeList != null)
					n += this.paramTypeList.length;
			} else
				++n;

			args = args.tail();
		}

		return n;
	}

	/*
	 * Index of $1.
	 */
	private int indexOfParam1()
	{
		return this.paramVarBase + (this.useParam0 ? 1 : 0);
	}

	/*
	 * To support $$. ($$) is equivalent to ($1, ..., $n). It can be used only
	 * as a parameter list of method call.
	 */
	public boolean isParamListName(ASTList args)
	{
		if (this.paramTypeList != null && args != null && args.tail() == null)
		{
			ASTree left = args.head();
			return left instanceof Member && ((Member) left).get().equals(this.paramListName);
		} else
			return false;
	}

	/**
	 * Makes method parameters $0, $1, ..., $args, $$, and $class available. $0
	 * is available only if use0 is true. It might not be equivalent to THIS.
	 *
	 * @param params
	 *            the parameter types (the types of $1, $2, ..)
	 * @param prefix
	 *            it must be "$" (the first letter of $0, $1, ...)
	 * @param paramVarName
	 *            it must be "$args"
	 * @param paramsName
	 *            it must be "$$"
	 * @param use0
	 *            true if $0 is used.
	 * @param paramBase
	 *            the register number of $0 (use0 is true) or $1 (otherwise).
	 * @param target
	 *            the class of $0. If use0 is false, target can be null. The
	 *            value of "target" is also used as the name of the type
	 *            represented by $class.
	 * @param isStatic
	 *            true if the method in which the compiled bytecode is embedded
	 *            is static.
	 */
	public int recordParams(CtClass[] params, boolean isStatic, String prefix, String paramVarName, String paramsName, boolean use0, int paramBase, String target, SymbolTable tbl) throws CompileError
	{
		int varNo;

		this.paramTypeList = params;
		this.paramArrayName = paramVarName;
		this.paramListName = paramsName;
		this.paramVarBase = paramBase;
		this.useParam0 = use0;

		if (target != null)
			this.param0Type = MemberResolver.jvmToJavaName(target);

		this.inStaticMethod = isStatic;
		varNo = paramBase;
		if (use0)
		{
			String varName = prefix + "0";
			Declarator decl = new Declarator(TokenId.CLASS, MemberResolver.javaToJvmName(target), 0, varNo++, new Symbol(varName));
			tbl.append(varName, decl);
		}

		for (int i = 0; i < params.length; ++i)
			varNo += this.recordVar(params[i], prefix + (i + 1), varNo, tbl);

		if (this.getMaxLocals() < varNo)
			this.setMaxLocals(varNo);

		return varNo;
	}

	/**
	 * Makes method parameters $0, $1, ..., $args, $$, and $class available. $0
	 * is equivalent to THIS if the method is not static. Otherwise, if the
	 * method is static, then $0 is not available.
	 */
	public int recordParams(CtClass[] params, boolean isStatic, String prefix, String paramVarName, String paramsName, SymbolTable tbl) throws CompileError
	{
		return this.recordParams(params, isStatic, prefix, paramVarName, paramsName, !isStatic, 0, this.getThisName(), tbl);
	}

	/**
	 * Makes a cast to the return type ($r) available. It also enables $_.
	 * <p>
	 * If the return type is void, ($r) does nothing. The type of $_ is
	 * java.lang.Object.
	 *
	 * @param resultName
	 *            null if $_ is not used.
	 * @return -1 or the variable index assigned to $_.
	 */
	public int recordReturnType(CtClass type, String castName, String resultName, SymbolTable tbl) throws CompileError
	{
		this.returnType = type;
		this.returnCastName = castName;
		this.returnVarName = resultName;
		if (resultName == null)
			return -1;
		else
		{
			int varNo = this.getMaxLocals();
			int locals = varNo + this.recordVar(type, resultName, varNo, tbl);
			this.setMaxLocals(locals);
			return varNo;
		}
	}

	/**
	 * Makes $type available.
	 */
	public void recordType(CtClass t)
	{
		this.dollarType = t;
	}

	private int recordVar(CtClass cc, String varName, int varNo, SymbolTable tbl) throws CompileError
	{
		if (cc == CtClass.voidType)
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = CodeGen.jvmJavaLangObject;
		} else
			this.setType(cc);

		Declarator decl = new Declarator(this.exprType, this.className, this.arrayDim, varNo, new Symbol(varName));
		tbl.append(varName, decl);
		return CodeGen.is2word(this.exprType, this.arrayDim) ? 2 : 1;
	}

	/**
	 * Makes the given variable name available.
	 *
	 * @param type
	 *            variable type
	 * @param varName
	 *            variable name
	 */
	public int recordVariable(CtClass type, String varName, SymbolTable tbl) throws CompileError
	{
		if (varName == null)
			return -1;
		else
		{
			int varNo = this.getMaxLocals();
			int locals = varNo + this.recordVar(type, varName, varNo, tbl);
			this.setMaxLocals(locals);
			return varNo;
		}
	}

	/**
	 * Makes the given variable name available.
	 *
	 * @param typeDesc
	 *            the type descriptor of the variable
	 * @param varName
	 *            variable name
	 * @param varNo
	 *            an index into the local variable array
	 */
	public void recordVariable(String typeDesc, String varName, int varNo, SymbolTable tbl) throws CompileError
	{
		char c;
		int dim = 0;
		while ((c = typeDesc.charAt(dim)) == '[')
			++dim;

		int type = MemberResolver.descToType(c);
		String cname = null;
		if (type == TokenId.CLASS)
			if (dim == 0)
				cname = typeDesc.substring(1, typeDesc.length() - 1);
			else
				cname = typeDesc.substring(dim + 1, typeDesc.length() - 1);

		Declarator decl = new Declarator(type, cname, dim, varNo, new Symbol(varName));
		tbl.append(varName, decl);
	}

	/*
	 * Records a ProceedHandler obejct.
	 * @param name the name of the special method call. it is usually $proceed.
	 */
	public void setProceedHandler(ProceedHandler h, String name)
	{
		this.proceedName = name;
		this.procHandler = h;
	}

	/*
	 * Sets exprType, arrayDim, and className; If type is void, then this method
	 * does nothing.
	 */
	public void setType(CtClass type) throws CompileError
	{
		this.setType(type, 0);
	}

	private void setType(CtClass type, int dim) throws CompileError
	{
		if (type.isPrimitive())
		{
			CtPrimitiveType pt = (CtPrimitiveType) type;
			this.exprType = MemberResolver.descToType(pt.getDescriptor());
			this.arrayDim = dim;
			this.className = null;
		} else if (type.isArray())
			try
			{
				this.setType(type.getComponentType(), dim + 1);
			} catch (NotFoundException e)
			{
				throw new CompileError("undefined type: " + type.getName());
			}
		else
		{
			this.exprType = TokenId.CLASS;
			this.arrayDim = dim;
			this.className = MemberResolver.javaToJvmName(type.getName());
		}
	}
}
