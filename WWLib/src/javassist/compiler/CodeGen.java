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
import java.util.Arrays;

import javassist.bytecode.Bytecode;
import javassist.bytecode.Opcode;
import javassist.compiler.ast.ASTList;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.ArrayInit;
import javassist.compiler.ast.AssignExpr;
import javassist.compiler.ast.BinExpr;
import javassist.compiler.ast.CallExpr;
import javassist.compiler.ast.CastExpr;
import javassist.compiler.ast.CondExpr;
import javassist.compiler.ast.Declarator;
import javassist.compiler.ast.DoubleConst;
import javassist.compiler.ast.Expr;
import javassist.compiler.ast.FieldDecl;
import javassist.compiler.ast.InstanceOfExpr;
import javassist.compiler.ast.IntConst;
import javassist.compiler.ast.Keyword;
import javassist.compiler.ast.Member;
import javassist.compiler.ast.MethodDecl;
import javassist.compiler.ast.NewExpr;
import javassist.compiler.ast.Pair;
import javassist.compiler.ast.Stmnt;
import javassist.compiler.ast.StringL;
import javassist.compiler.ast.Symbol;
import javassist.compiler.ast.Variable;
import javassist.compiler.ast.Visitor;

/* The code generator is implemeted by three files:
 * CodeGen.java, MemberCodeGen.java, and JvstCodeGen.
 * I just wanted to split a big file into three smaller ones.
 */

public abstract class CodeGen extends Visitor implements Opcode, TokenId
{
	/**
	 * doit() in ReturnHook is called from atReturn().
	 */
	protected static abstract class ReturnHook
	{
		ReturnHook next;

		protected ReturnHook(CodeGen gen)
		{
			this.next = gen.returnHooks;
			gen.returnHooks = this;
		}

		/**
		 * Returns true if the generated code ends with return, throw, or goto.
		 */
		protected abstract boolean doit(Bytecode b, int opcode);

		protected void remove(CodeGen gen)
		{
			gen.returnHooks = this.next;
		}
	}

	static final String javaLangObject = "java.lang.Object";

	static final String	jvmJavaLangObject	= "java/lang/Object";
	static final String	javaLangString		= "java.lang.String";

	static final String			jvmJavaLangString	= "java/lang/String";
	static final int[]			binOp				=
	{ '+', Opcode.DADD, Opcode.FADD, Opcode.LADD, Opcode.IADD, '-', Opcode.DSUB, Opcode.FSUB, Opcode.LSUB, Opcode.ISUB, '*', Opcode.DMUL, Opcode.FMUL, Opcode.LMUL, Opcode.IMUL, '/', Opcode.DDIV,
			Opcode.FDIV, Opcode.LDIV, Opcode.IDIV, '%', Opcode.DREM, Opcode.FREM, Opcode.LREM, Opcode.IREM, '|', Opcode.NOP, Opcode.NOP, Opcode.LOR, Opcode.IOR, '^', Opcode.NOP, Opcode.NOP,
			Opcode.LXOR, Opcode.IXOR, '&', Opcode.NOP, Opcode.NOP, Opcode.LAND, Opcode.IAND, TokenId.LSHIFT, Opcode.NOP, Opcode.NOP, Opcode.LSHL, Opcode.ISHL, TokenId.RSHIFT, Opcode.NOP, Opcode.NOP,
			Opcode.LSHR, Opcode.ISHR, TokenId.ARSHIFT, Opcode.NOP, Opcode.NOP, Opcode.LUSHR, Opcode.IUSHR };
	private static final int	ifOp[]				=
	{ TokenId.EQ, Opcode.IF_ICMPEQ, Opcode.IF_ICMPNE, TokenId.NEQ, Opcode.IF_ICMPNE, Opcode.IF_ICMPEQ, TokenId.LE, Opcode.IF_ICMPLE, Opcode.IF_ICMPGT, TokenId.GE, Opcode.IF_ICMPGE, Opcode.IF_ICMPLT,
			'<', Opcode.IF_ICMPLT, Opcode.IF_ICMPGE, '>', Opcode.IF_ICMPGT, Opcode.IF_ICMPLE };

	private static final int ifOp2[] =
	{ TokenId.EQ, Opcode.IFEQ, Opcode.IFNE, TokenId.NEQ, Opcode.IFNE, Opcode.IFEQ, TokenId.LE, Opcode.IFLE, Opcode.IFGT, TokenId.GE, Opcode.IFGE, Opcode.IFLT, '<', Opcode.IFLT, Opcode.IFGE, '>',
			Opcode.IFGT, Opcode.IFLE };

	private static final int P_DOUBLE = 0;

	private static final int P_FLOAT = 1;

	private static final int P_LONG = 2;

	private static final int P_INT = 3;

	private static final int	P_OTHER	= -1;
	private static final int[]	castOp	=
	{ Opcode.																																												/*
																																															 * D
																																															 * F
																																															 * L
																																															 * I
																																															 */
			/* double */NOP, Opcode.D2F, Opcode.D2L, Opcode.D2I, Opcode./* float */F2D, Opcode.NOP, Opcode.F2L, Opcode.F2I, Opcode./* long */L2D, Opcode.L2F, Opcode.NOP, Opcode.L2I,
			Opcode./* other */I2D, Opcode.I2F, Opcode.I2L, Opcode.NOP };

	protected static void badAssign(Expr expr) throws CompileError
	{
		String msg;
		if (expr == null)
			msg = "incompatible type for assignment";
		else
			msg = "incompatible type for " + expr.getName();

		throw new CompileError(msg);
	}

	protected static void badType(Expr expr) throws CompileError
	{
		throw new CompileError("invalid type for " + expr.getName());
	}

	protected static void badTypes(Expr expr) throws CompileError
	{
		throw new CompileError("invalid types for " + expr.getName());
	}

	protected static void fatal() throws CompileError
	{
		throw new CompileError("fatal");
	}

	protected static int getArrayReadOp(int type, int dim)
	{
		if (dim > 0)
			return Opcode.AALOAD;

		switch (type)
		{
			case DOUBLE:
				return Opcode.DALOAD;
			case FLOAT:
				return Opcode.FALOAD;
			case LONG:
				return Opcode.LALOAD;
			case INT:
				return Opcode.IALOAD;
			case SHORT:
				return Opcode.SALOAD;
			case CHAR:
				return Opcode.CALOAD;
			case BYTE:
			case BOOLEAN:
				return Opcode.BALOAD;
			default:
				return Opcode.AALOAD;
		}
	}

	protected static int getArrayWriteOp(int type, int dim)
	{
		if (dim > 0)
			return Opcode.AASTORE;

		switch (type)
		{
			case DOUBLE:
				return Opcode.DASTORE;
			case FLOAT:
				return Opcode.FASTORE;
			case LONG:
				return Opcode.LASTORE;
			case INT:
				return Opcode.IASTORE;
			case SHORT:
				return Opcode.SASTORE;
			case CHAR:
				return Opcode.CASTORE;
			case BYTE:
			case BOOLEAN:
				return Opcode.BASTORE;
			default:
				return Opcode.AASTORE;
		}
	}

	static int getCompOperator(ASTree expr) throws CompileError
	{
		if (expr instanceof Expr)
		{
			Expr bexpr = (Expr) expr;
			int token = bexpr.getOperator();
			if (token == '!')
				return '!';
			else if (bexpr instanceof BinExpr && token != TokenId.OROR && token != TokenId.ANDAND && token != '&' && token != '|')
				return TokenId.EQ; // ==, !=, ...
			else
				return token;
		}

		return ' '; // others
	}

	private static int getListSize(ArrayList list)
	{
		return list == null ? 0 : list.size();
	}

	public static boolean is2word(int type, int dim)
	{
		return dim == 0 && (type == TokenId.DOUBLE || type == TokenId.LONG);
	}

	private static boolean isAlwaysBranch(ASTree expr, boolean branchIf)
	{
		if (expr instanceof Keyword)
		{
			int t = ((Keyword) expr).get();
			return branchIf ? t == TokenId.TRUE : t == TokenId.FALSE;
		}

		return false;
	}

	// used in TypeChecker.
	static boolean isP_INT(int type)
	{
		return CodeGen.typePrecedence(type) == CodeGen.P_INT;
	}

	private static boolean isPlusPlusExpr(ASTree expr)
	{
		if (expr instanceof Expr)
		{
			int op = ((Expr) expr).getOperator();
			return op == TokenId.PLUSPLUS || op == TokenId.MINUSMINUS;
		}

		return false;
	}

	protected static boolean isRefType(int type)
	{
		return type == TokenId.CLASS || type == TokenId.NULL;
	}

	static int lookupBinOp(int token)
	{
		int[] code = CodeGen.binOp;
		int s = code.length;
		for (int k = 0; k < s; k = k + 5)
			if (code[k] == token)
				return k;

		return -1;
	}

	// used in TypeChecker.
	static boolean rightIsStrong(int type1, int type2)
	{
		int type1_p = CodeGen.typePrecedence(type1);
		int type2_p = CodeGen.typePrecedence(type2);
		return type1_p >= 0 && type2_p >= 0 && type1_p > type2_p;
	}

	/**
	 * @param name
	 *            the JVM-internal representation. name is not exapnded to
	 *            java.lang.*.
	 */
	protected static String toJvmArrayName(String name, int dim)
	{
		if (name == null)
			return null;

		if (dim == 0)
			return name;
		else
		{
			StringBuffer sbuf = new StringBuffer();
			int d = dim;
			while (d-- > 0)
				sbuf.append('[');

			sbuf.append('L');
			sbuf.append(name);
			sbuf.append(';');

			return sbuf.toString();
		}
	}

	protected static String toJvmTypeName(int type, int dim)
	{
		char c = 'I';
		switch (type)
		{
			case BOOLEAN:
				c = 'Z';
				break;
			case BYTE:
				c = 'B';
				break;
			case CHAR:
				c = 'C';
				break;
			case SHORT:
				c = 'S';
				break;
			case INT:
				c = 'I';
				break;
			case LONG:
				c = 'J';
				break;
			case FLOAT:
				c = 'F';
				break;
			case DOUBLE:
				c = 'D';
				break;
			case VOID:
				c = 'V';
				break;
		}

		StringBuffer sbuf = new StringBuffer();
		while (dim-- > 0)
			sbuf.append('[');

		sbuf.append(c);
		return sbuf.toString();
	}

	private static int typePrecedence(int type)
	{
		if (type == TokenId.DOUBLE)
			return CodeGen.P_DOUBLE;
		else if (type == TokenId.FLOAT)
			return CodeGen.P_FLOAT;
		else if (type == TokenId.LONG)
			return CodeGen.P_LONG;
		else if (CodeGen.isRefType(type))
			return CodeGen.P_OTHER;
		else if (type == TokenId.VOID)
			return CodeGen.P_OTHER; // this is wrong, but ...
		else
			return CodeGen.P_INT; // BOOLEAN, BYTE, CHAR, SHORT, INT
	}

	protected Bytecode bytecode;

	private int tempVar;

	TypeChecker typeChecker;

	/**
	 * true if the last visited node is a return statement.
	 */
	protected boolean hasReturned;

	/**
	 * Must be true if compilation is for a static method.
	 */
	public boolean inStaticMethod;

	protected ArrayList breakList, continueList;

	protected ReturnHook returnHooks;

	/*
	 * The following fields are used by atXXX() methods for returning the type
	 * of the compiled expression.
	 */
	protected int exprType;					// VOID, NULL, CLASS,
	// BOOLEAN, INT, ...

	protected int arrayDim;

	protected String className;					// JVM-internal
	// representation

	public CodeGen(Bytecode b)
	{
		this.bytecode = b;
		this.tempVar = -1;
		this.typeChecker = null;
		this.hasReturned = false;
		this.inStaticMethod = false;
		this.breakList = null;
		this.continueList = null;
		this.returnHooks = null;
	}

	protected void arrayAccess(ASTree array, ASTree index) throws CompileError
	{
		array.accept(this);
		int type = this.exprType;
		int dim = this.arrayDim;
		if (dim == 0)
			throw new CompileError("bad array access");

		String cname = this.className;

		index.accept(this);
		if (CodeGen.typePrecedence(this.exprType) != CodeGen.P_INT || this.arrayDim > 0)
			throw new CompileError("bad array index");

		this.exprType = type;
		this.arrayDim = dim - 1;
		this.className = cname;
	}

	/*
	 * arrayDim values of the two oprands must be equal. If an oprand type is
	 * not a numeric type, this method throws an exception.
	 */
	private void atArithBinExpr(Expr expr, int token, int index, int type1) throws CompileError
	{
		if (this.arrayDim != 0)
			CodeGen.badTypes(expr);

		int type2 = this.exprType;
		if (token == TokenId.LSHIFT || token == TokenId.RSHIFT || token == TokenId.ARSHIFT)
			if (type2 == TokenId.INT || type2 == TokenId.SHORT || type2 == TokenId.CHAR || type2 == TokenId.BYTE)
				this.exprType = type1;
			else
				CodeGen.badTypes(expr);
		else
			this.convertOprandTypes(type1, type2, expr);

		int p = CodeGen.typePrecedence(this.exprType);
		if (p >= 0)
		{
			int op = CodeGen.binOp[index + p + 1];
			if (op != Opcode.NOP)
			{
				if (p == CodeGen.P_INT && this.exprType != TokenId.BOOLEAN)
					this.exprType = TokenId.INT; // type1 may be BYTE, ...

				this.bytecode.addOpcode(op);
				return;
			}
		}

		CodeGen.badTypes(expr);
	}

	private void atArrayAssign(Expr expr, int op, Expr array, ASTree right, boolean doDup) throws CompileError
	{
		this.arrayAccess(array.oprand1(), array.oprand2());

		if (op != '=')
		{
			this.bytecode.addOpcode(Opcode.DUP2);
			this.bytecode.addOpcode(CodeGen.getArrayReadOp(this.exprType, this.arrayDim));
		}

		int aType = this.exprType;
		int aDim = this.arrayDim;
		String cname = this.className;

		this.atAssignCore(expr, op, right, aType, aDim, cname);

		if (doDup)
			if (CodeGen.is2word(aType, aDim))
				this.bytecode.addOpcode(Opcode.DUP2_X2);
			else
				this.bytecode.addOpcode(Opcode.DUP_X2);

		this.bytecode.addOpcode(CodeGen.getArrayWriteOp(aType, aDim));
		this.exprType = aType;
		this.arrayDim = aDim;
		this.className = cname;
	}

	@Override
	public abstract void atArrayInit(ArrayInit init) throws CompileError;

	public void atArrayPlusPlus(int token, boolean isPost, Expr expr, boolean doDup) throws CompileError
	{
		this.arrayAccess(expr.oprand1(), expr.oprand2());
		int t = this.exprType;
		int dim = this.arrayDim;
		if (dim > 0)
			CodeGen.badType(expr);

		this.bytecode.addOpcode(Opcode.DUP2);
		this.bytecode.addOpcode(CodeGen.getArrayReadOp(t, this.arrayDim));
		int dup_code = CodeGen.is2word(t, dim) ? Opcode.DUP2_X2 : Opcode.DUP_X2;
		this.atPlusPlusCore(dup_code, doDup, token, isPost, expr);
		this.bytecode.addOpcode(CodeGen.getArrayWriteOp(t, dim));
	}

	public void atArrayRead(ASTree array, ASTree index) throws CompileError
	{
		this.arrayAccess(array, index);
		this.bytecode.addOpcode(CodeGen.getArrayReadOp(this.exprType, this.arrayDim));
	}

	protected abstract void atArrayVariableAssign(ArrayInit init, int varType, int varArray, String varClass) throws CompileError;

	protected void atAssignCore(Expr expr, int op, ASTree right, int type, int dim, String cname) throws CompileError
	{
		if (op == TokenId.PLUS_E && dim == 0 && type == TokenId.CLASS)
			this.atStringPlusEq(expr, type, dim, cname, right);
		else
		{
			right.accept(this);
			if (this.invalidDim(this.exprType, this.arrayDim, this.className, type, dim, cname, false) || op != '=' && dim > 0)
				CodeGen.badAssign(expr);

			if (op != '=')
			{
				int token = TokenId.assignOps[op - TokenId.MOD_E];
				int k = CodeGen.lookupBinOp(token);
				if (k < 0)
					CodeGen.fatal();

				this.atArithBinExpr(expr, token, k, type);
			}
		}

		if (op != '=' || dim == 0 && !CodeGen.isRefType(type))
			this.atNumCastExpr(this.exprType, type);

		// type check should be done here.
	}

	@Override
	public void atAssignExpr(AssignExpr expr) throws CompileError
	{
		this.atAssignExpr(expr, true);
	}

	protected void atAssignExpr(AssignExpr expr, boolean doDup) throws CompileError
	{
		// =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, >>>=
		int op = expr.getOperator();
		ASTree left = expr.oprand1();
		ASTree right = expr.oprand2();
		if (left instanceof Variable)
			this.atVariableAssign(expr, op, (Variable) left, ((Variable) left).getDeclarator(), right, doDup);
		else
		{
			if (left instanceof Expr)
			{
				Expr e = (Expr) left;
				if (e.getOperator() == TokenId.ARRAY)
				{
					this.atArrayAssign(expr, op, (Expr) left, right, doDup);
					return;
				}
			}

			this.atFieldAssign(expr, op, left, right, doDup);
		}
	}

	@Override
	public void atASTList(ASTList n) throws CompileError
	{
		CodeGen.fatal();
	}

	@Override
	public void atBinExpr(BinExpr expr) throws CompileError
	{
		int token = expr.getOperator();

		/*
		 * arithmetic operators: +, -, *, /, %, |, ^, &, <<, >>, >>>
		 */
		int k = CodeGen.lookupBinOp(token);
		if (k >= 0)
		{
			expr.oprand1().accept(this);
			ASTree right = expr.oprand2();
			if (right == null)
				return; // see TypeChecker.atBinExpr().

			int type1 = this.exprType;
			int dim1 = this.arrayDim;
			String cname1 = this.className;
			right.accept(this);
			if (dim1 != this.arrayDim)
				throw new CompileError("incompatible array types");

			if (token == '+' && dim1 == 0 && (type1 == TokenId.CLASS || this.exprType == TokenId.CLASS))
				this.atStringConcatExpr(expr, type1, dim1, cname1);
			else
				this.atArithBinExpr(expr, token, k, type1);
		} else
		{
			/*
			 * equation: &&, ||, ==, !=, <=, >=, <, >
			 */
			if (!this.booleanExpr(true, expr))
			{
				this.bytecode.addIndex(7);
				this.bytecode.addIconst(0); // false
				this.bytecode.addOpcode(Opcode.GOTO);
				this.bytecode.addIndex(4);
			}

			this.bytecode.addIconst(1); // true
		}
	}

	private void atBreakStmnt(Stmnt st, boolean notCont) throws CompileError
	{
		if (st.head() != null)
			throw new CompileError("sorry, not support labeled break or continue");

		this.bytecode.addOpcode(Opcode.GOTO);
		Integer pc = new Integer(this.bytecode.currentPc());
		this.bytecode.addIndex(0);
		if (notCont)
			this.breakList.add(pc);
		else
			this.continueList.add(pc);
	}

	@Override
	public abstract void atCallExpr(CallExpr expr) throws CompileError;

	@Override
	public void atCastExpr(CastExpr expr) throws CompileError
	{
		String cname = this.resolveClassName(expr.getClassName());
		String toClass = this.checkCastExpr(expr, cname);
		int srcType = this.exprType;
		this.exprType = expr.getType();
		this.arrayDim = expr.getArrayDim();
		this.className = cname;
		if (toClass == null)
			this.atNumCastExpr(srcType, this.exprType); // built-in type
		else
			this.bytecode.addCheckcast(toClass);
	}

	public void atClassObject(Expr expr) throws CompileError
	{
		ASTree op1 = expr.oprand1();
		if (!(op1 instanceof Symbol))
			throw new CompileError("fatal error: badly parsed .class expr");

		String cname = ((Symbol) op1).get();
		if (cname.startsWith("["))
		{
			int i = cname.indexOf("[L");
			if (i >= 0)
			{
				String name = cname.substring(i + 2, cname.length() - 1);
				String name2 = this.resolveClassName(name);
				if (!name.equals(name2))
				{
					/*
					 * For example, to obtain String[].class,
					 * "[Ljava.lang.String;" (not "[Ljava/lang/String"!) must be
					 * passed to Class.forName().
					 */
					name2 = MemberResolver.jvmToJavaName(name2);
					StringBuffer sbuf = new StringBuffer();
					while (i-- >= 0)
						sbuf.append('[');

					sbuf.append('L').append(name2).append(';');
					cname = sbuf.toString();
				}
			}
		} else
		{
			cname = this.resolveClassName(MemberResolver.javaToJvmName(cname));
			cname = MemberResolver.jvmToJavaName(cname);
		}

		this.atClassObject2(cname);
		this.exprType = TokenId.CLASS;
		this.arrayDim = 0;
		this.className = "java/lang/Class";
	}

	/*
	 * MemberCodeGen overrides this method.
	 */
	protected void atClassObject2(String cname) throws CompileError
	{
		int start = this.bytecode.currentPc();
		this.bytecode.addLdc(cname);
		this.bytecode.addInvokestatic("java.lang.Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
		int end = this.bytecode.currentPc();
		this.bytecode.addOpcode(Opcode.GOTO);
		int pc = this.bytecode.currentPc();
		this.bytecode.addIndex(0); // correct later

		this.bytecode.addExceptionHandler(start, end, this.bytecode.currentPc(), "java.lang.ClassNotFoundException");

		/*
		 * -- the following code is for inlining a call to DotClass.fail(). int
		 * var = getMaxLocals(); incMaxLocals(1); bytecode.growStack(1);
		 * bytecode.addAstore(var);
		 * bytecode.addNew("java.lang.NoClassDefFoundError");
		 * bytecode.addOpcode(DUP); bytecode.addAload(var);
		 * bytecode.addInvokevirtual("java.lang.ClassNotFoundException",
		 * "getMessage", "()Ljava/lang/String;");
		 * bytecode.addInvokespecial("java.lang.NoClassDefFoundError", "<init>",
		 * "(Ljava/lang/String;)V");
		 */

		this.bytecode.growStack(1);
		this.bytecode.addInvokestatic("javassist.runtime.DotClass", "fail", "(Ljava/lang/ClassNotFoundException;)" + "Ljava/lang/NoClassDefFoundError;");
		this.bytecode.addOpcode(Opcode.ATHROW);
		this.bytecode.write16bit(pc, this.bytecode.currentPc() - pc + 1);
	}

	@Override
	public void atCondExpr(CondExpr expr) throws CompileError
	{
		if (this.booleanExpr(false, expr.condExpr()))
			expr.elseExpr().accept(this);
		else
		{
			int pc = this.bytecode.currentPc();
			this.bytecode.addIndex(0); // correct later
			expr.thenExpr().accept(this);
			int dim1 = this.arrayDim;
			this.bytecode.addOpcode(Opcode.GOTO);
			int pc2 = this.bytecode.currentPc();
			this.bytecode.addIndex(0);
			this.bytecode.write16bit(pc, this.bytecode.currentPc() - pc + 1);
			expr.elseExpr().accept(this);
			if (dim1 != this.arrayDim)
				throw new CompileError("type mismatch in ?:");

			this.bytecode.write16bit(pc2, this.bytecode.currentPc() - pc2 + 1);
		}
	}

	@Override
	public void atDeclarator(Declarator d) throws CompileError
	{
		d.setLocalVar(this.getMaxLocals());
		d.setClassName(this.resolveClassName(d.getClassName()));

		int size;
		if (CodeGen.is2word(d.getType(), d.getArrayDim()))
			size = 2;
		else
			size = 1;

		this.incMaxLocals(size);

		/*
		 * NOTE: Array initializers has not been supported.
		 */
		ASTree init = d.getInitializer();
		if (init != null)
		{
			this.doTypeCheck(init);
			this.atVariableAssign(null, '=', null, d, init, false);
		}
	}

	@Override
	public void atDoubleConst(DoubleConst d) throws CompileError
	{
		this.arrayDim = 0;
		if (d.getType() == TokenId.DoubleConstant)
		{
			this.exprType = TokenId.DOUBLE;
			this.bytecode.addDconst(d.get());
		} else
		{
			this.exprType = TokenId.FLOAT;
			this.bytecode.addFconst((float) d.get());
		}
	}

	@Override
	public void atExpr(Expr expr) throws CompileError
	{
		// array access, member access,
		// (unary) +, (unary) -, ++, --, !, ~

		int token = expr.getOperator();
		ASTree oprand = expr.oprand1();
		if (token == '.')
		{
			String member = ((Symbol) expr.oprand2()).get();
			if (member.equals("class"))
				this.atClassObject(expr); // .class
			else
				this.atFieldRead(expr);
		} else if (token == TokenId.MEMBER)
			/*
			 * MEMBER ('#') is an extension by Javassist. The compiler
			 * internally uses # for compiling .class expressions such as
			 * "int.class".
			 */
			this.atFieldRead(expr);
		else if (token == TokenId.ARRAY)
			this.atArrayRead(oprand, expr.oprand2());
		else if (token == TokenId.PLUSPLUS || token == TokenId.MINUSMINUS)
			this.atPlusPlus(token, oprand, expr, true);
		else if (token == '!')
		{
			if (!this.booleanExpr(false, expr))
			{
				this.bytecode.addIndex(7);
				this.bytecode.addIconst(1);
				this.bytecode.addOpcode(Opcode.GOTO);
				this.bytecode.addIndex(4);
			}

			this.bytecode.addIconst(0);
		} else if (token == TokenId.CALL)   // method call
			CodeGen.fatal();
		else
		{
			expr.oprand1().accept(this);
			int type = CodeGen.typePrecedence(this.exprType);
			if (this.arrayDim > 0)
				CodeGen.badType(expr);

			if (token == '-')
			{
				if (type == CodeGen.P_DOUBLE)
					this.bytecode.addOpcode(Opcode.DNEG);
				else if (type == CodeGen.P_FLOAT)
					this.bytecode.addOpcode(Opcode.FNEG);
				else if (type == CodeGen.P_LONG)
					this.bytecode.addOpcode(Opcode.LNEG);
				else if (type == CodeGen.P_INT)
				{
					this.bytecode.addOpcode(Opcode.INEG);
					this.exprType = TokenId.INT; // type may be BYTE, ...
				} else
					CodeGen.badType(expr);
			} else if (token == '~')
			{
				if (type == CodeGen.P_INT)
				{
					this.bytecode.addIconst(-1);
					this.bytecode.addOpcode(Opcode.IXOR);
					this.exprType = TokenId.INT; // type may be BYTE. ...
				} else if (type == CodeGen.P_LONG)
				{
					this.bytecode.addLconst(-1);
					this.bytecode.addOpcode(Opcode.LXOR);
				} else
					CodeGen.badType(expr);

			} else if (token == '+')
			{
				if (type == CodeGen.P_OTHER)
					CodeGen.badType(expr);

				// do nothing. ignore.
			} else
				CodeGen.fatal();
		}
	}

	protected abstract void atFieldAssign(Expr expr, int op, ASTree left, ASTree right, boolean doDup) throws CompileError;

	@Override
	public void atFieldDecl(FieldDecl field) throws CompileError
	{
		field.getInit().accept(this);
	}

	protected abstract void atFieldPlusPlus(int token, boolean isPost, ASTree oprand, Expr expr, boolean doDup) throws CompileError;

	protected abstract void atFieldRead(ASTree expr) throws CompileError;

	private void atForStmnt(Stmnt st) throws CompileError
	{
		ArrayList prevBreakList = this.breakList;
		ArrayList prevContList = this.continueList;
		this.breakList = new ArrayList();
		this.continueList = new ArrayList();

		Stmnt init = (Stmnt) st.head();
		ASTList p = st.tail();
		ASTree expr = p.head();
		p = p.tail();
		Stmnt update = (Stmnt) p.head();
		Stmnt body = (Stmnt) p.tail();

		if (init != null)
			init.accept(this);

		int pc = this.bytecode.currentPc();
		int pc2 = 0;
		if (expr != null)
		{
			if (this.compileBooleanExpr(false, expr))
			{
				// in case of "for (...; false; ...)"
				this.continueList = prevContList;
				this.breakList = prevBreakList;
				this.hasReturned = false;
				return;
			}

			pc2 = this.bytecode.currentPc();
			this.bytecode.addIndex(0);
		}

		if (body != null)
			body.accept(this);

		int pc3 = this.bytecode.currentPc();
		if (update != null)
			update.accept(this);

		this.bytecode.addOpcode(Opcode.GOTO);
		this.bytecode.addIndex(pc - this.bytecode.currentPc() + 1);

		int pc4 = this.bytecode.currentPc();
		if (expr != null)
			this.bytecode.write16bit(pc2, pc4 - pc2 + 1);

		this.patchGoto(this.breakList, pc4);
		this.patchGoto(this.continueList, pc3);
		this.continueList = prevContList;
		this.breakList = prevBreakList;
		this.hasReturned = false;
	}

	private void atIfStmnt(Stmnt st) throws CompileError
	{
		ASTree expr = st.head();
		Stmnt thenp = (Stmnt) st.tail().head();
		Stmnt elsep = (Stmnt) st.tail().tail().head();
		if (this.compileBooleanExpr(false, expr))
		{
			this.hasReturned = false;
			if (elsep != null)
				elsep.accept(this);

			return;
		}

		int pc = this.bytecode.currentPc();
		int pc2 = 0;
		this.bytecode.addIndex(0); // correct later

		this.hasReturned = false;
		if (thenp != null)
			thenp.accept(this);

		boolean thenHasReturned = this.hasReturned;
		this.hasReturned = false;

		if (elsep != null && !thenHasReturned)
		{
			this.bytecode.addOpcode(Opcode.GOTO);
			pc2 = this.bytecode.currentPc();
			this.bytecode.addIndex(0);
		}

		this.bytecode.write16bit(pc, this.bytecode.currentPc() - pc + 1);
		if (elsep != null)
		{
			elsep.accept(this);
			if (!thenHasReturned)
				this.bytecode.write16bit(pc2, this.bytecode.currentPc() - pc2 + 1);

			this.hasReturned = thenHasReturned && this.hasReturned;
		}
	}

	@Override
	public void atInstanceOfExpr(InstanceOfExpr expr) throws CompileError
	{
		String cname = this.resolveClassName(expr.getClassName());
		String toClass = this.checkCastExpr(expr, cname);
		this.bytecode.addInstanceof(toClass);
		this.exprType = TokenId.BOOLEAN;
		this.arrayDim = 0;
	}

	@Override
	public void atIntConst(IntConst i) throws CompileError
	{
		this.arrayDim = 0;
		long value = i.get();
		int type = i.getType();
		if (type == TokenId.IntConstant || type == TokenId.CharConstant)
		{
			this.exprType = type == TokenId.IntConstant ? TokenId.INT : TokenId.CHAR;
			this.bytecode.addIconst((int) value);
		} else
		{
			this.exprType = TokenId.LONG;
			this.bytecode.addLconst(value);
		}
	}

	@Override
	public void atKeyword(Keyword k) throws CompileError
	{
		this.arrayDim = 0;
		int token = k.get();
		switch (token)
		{
			case TRUE:
				this.bytecode.addIconst(1);
				this.exprType = TokenId.BOOLEAN;
				break;
			case FALSE:
				this.bytecode.addIconst(0);
				this.exprType = TokenId.BOOLEAN;
				break;
			case NULL:
				this.bytecode.addOpcode(Opcode.ACONST_NULL);
				this.exprType = TokenId.NULL;
				break;
			case THIS:
			case SUPER:
				if (this.inStaticMethod)
					throw new CompileError("not-available: " + (token == TokenId.THIS ? "this" : "super"));

				this.bytecode.addAload(0);
				this.exprType = TokenId.CLASS;
				if (token == TokenId.THIS)
					this.className = this.getThisName();
				else
					this.className = this.getSuperName();
				break;
			default:
				CodeGen.fatal();
		}
	}

	@Override
	public abstract void atMember(Member n) throws CompileError;

	/**
	 * @param isCons
	 *            true if super() must be called. false if the method is a class
	 *            initializer.
	 */
	public void atMethodBody(Stmnt s, boolean isCons, boolean isVoid) throws CompileError
	{
		if (s == null)
			return;

		if (isCons && this.needsSuperCall(s))
			this.insertDefaultSuperCall();

		this.hasReturned = false;
		s.accept(this);
		if (!this.hasReturned)
			if (isVoid)
			{
				this.bytecode.addOpcode(Opcode.RETURN);
				this.hasReturned = true;
			} else
				throw new CompileError("no return statement");
	}

	@Override
	public void atMethodDecl(MethodDecl method) throws CompileError
	{
		ASTList mods = method.getModifiers();
		this.setMaxLocals(1);
		while (mods != null)
		{
			Keyword k = (Keyword) mods.head();
			mods = mods.tail();
			if (k.get() == TokenId.STATIC)
			{
				this.setMaxLocals(0);
				this.inStaticMethod = true;
			}
		}

		ASTList params = method.getParams();
		while (params != null)
		{
			this.atDeclarator((Declarator) params.head());
			params = params.tail();
		}

		Stmnt s = method.getBody();
		this.atMethodBody(s, method.isConstructor(), method.getReturn().getType() == TokenId.VOID);
	}

	@Override
	public abstract void atNewExpr(NewExpr n) throws CompileError;

	void atNumCastExpr(int srcType, int destType) throws CompileError
	{
		if (srcType == destType)
			return;

		int op, op2;
		int stype = CodeGen.typePrecedence(srcType);
		int dtype = CodeGen.typePrecedence(destType);
		if (0 <= stype && stype < 3)
			op = CodeGen.castOp[stype * 4 + dtype];
		else
			op = Opcode.NOP;

		if (destType == TokenId.DOUBLE)
			op2 = Opcode.I2D;
		else if (destType == TokenId.FLOAT)
			op2 = Opcode.I2F;
		else if (destType == TokenId.LONG)
			op2 = Opcode.I2L;
		else if (destType == TokenId.SHORT)
			op2 = Opcode.I2S;
		else if (destType == TokenId.CHAR)
			op2 = Opcode.I2C;
		else if (destType == TokenId.BYTE)
			op2 = Opcode.I2B;
		else
			op2 = Opcode.NOP;

		if (op != Opcode.NOP)
			this.bytecode.addOpcode(op);

		if (op == Opcode.NOP || op == Opcode.L2I || op == Opcode.F2I || op == Opcode.D2I)
			if (op2 != Opcode.NOP)
				this.bytecode.addOpcode(op2);
	}

	@Override
	public void atPair(Pair n) throws CompileError
	{
		CodeGen.fatal();
	}

	private void atPlusPlus(int token, ASTree oprand, Expr expr, boolean doDup) throws CompileError
	{
		boolean isPost = oprand == null; // ++i or i++?
		if (isPost)
			oprand = expr.oprand2();

		if (oprand instanceof Variable)
		{
			Declarator d = ((Variable) oprand).getDeclarator();
			int t = this.exprType = d.getType();
			this.arrayDim = d.getArrayDim();
			int var = this.getLocalVar(d);
			if (this.arrayDim > 0)
				CodeGen.badType(expr);

			if (t == TokenId.DOUBLE)
			{
				this.bytecode.addDload(var);
				if (doDup && isPost)
					this.bytecode.addOpcode(Opcode.DUP2);

				this.bytecode.addDconst(1.0);
				this.bytecode.addOpcode(token == TokenId.PLUSPLUS ? Opcode.DADD : Opcode.DSUB);
				if (doDup && !isPost)
					this.bytecode.addOpcode(Opcode.DUP2);

				this.bytecode.addDstore(var);
			} else if (t == TokenId.LONG)
			{
				this.bytecode.addLload(var);
				if (doDup && isPost)
					this.bytecode.addOpcode(Opcode.DUP2);

				this.bytecode.addLconst(1);
				this.bytecode.addOpcode(token == TokenId.PLUSPLUS ? Opcode.LADD : Opcode.LSUB);
				if (doDup && !isPost)
					this.bytecode.addOpcode(Opcode.DUP2);

				this.bytecode.addLstore(var);
			} else if (t == TokenId.FLOAT)
			{
				this.bytecode.addFload(var);
				if (doDup && isPost)
					this.bytecode.addOpcode(Opcode.DUP);

				this.bytecode.addFconst(1.0f);
				this.bytecode.addOpcode(token == TokenId.PLUSPLUS ? Opcode.FADD : Opcode.FSUB);
				if (doDup && !isPost)
					this.bytecode.addOpcode(Opcode.DUP);

				this.bytecode.addFstore(var);
			} else if (t == TokenId.BYTE || t == TokenId.CHAR || t == TokenId.SHORT || t == TokenId.INT)
			{
				if (doDup && isPost)
					this.bytecode.addIload(var);

				int delta = token == TokenId.PLUSPLUS ? 1 : -1;
				if (var > 0xff)
				{
					this.bytecode.addOpcode(Opcode.WIDE);
					this.bytecode.addOpcode(Opcode.IINC);
					this.bytecode.addIndex(var);
					this.bytecode.addIndex(delta);
				} else
				{
					this.bytecode.addOpcode(Opcode.IINC);
					this.bytecode.add(var);
					this.bytecode.add(delta);
				}

				if (doDup && !isPost)
					this.bytecode.addIload(var);
			} else
				CodeGen.badType(expr);
		} else
		{
			if (oprand instanceof Expr)
			{
				Expr e = (Expr) oprand;
				if (e.getOperator() == TokenId.ARRAY)
				{
					this.atArrayPlusPlus(token, isPost, e, doDup);
					return;
				}
			}

			this.atFieldPlusPlus(token, isPost, oprand, expr, doDup);
		}
	}

	protected void atPlusPlusCore(int dup_code, boolean doDup, int token, boolean isPost, Expr expr) throws CompileError
	{
		int t = this.exprType;

		if (doDup && isPost)
			this.bytecode.addOpcode(dup_code);

		if (t == TokenId.INT || t == TokenId.BYTE || t == TokenId.CHAR || t == TokenId.SHORT)
		{
			this.bytecode.addIconst(1);
			this.bytecode.addOpcode(token == TokenId.PLUSPLUS ? Opcode.IADD : Opcode.ISUB);
			this.exprType = TokenId.INT;
		} else if (t == TokenId.LONG)
		{
			this.bytecode.addLconst(1);
			this.bytecode.addOpcode(token == TokenId.PLUSPLUS ? Opcode.LADD : Opcode.LSUB);
		} else if (t == TokenId.FLOAT)
		{
			this.bytecode.addFconst(1.0f);
			this.bytecode.addOpcode(token == TokenId.PLUSPLUS ? Opcode.FADD : Opcode.FSUB);
		} else if (t == TokenId.DOUBLE)
		{
			this.bytecode.addDconst(1.0);
			this.bytecode.addOpcode(token == TokenId.PLUSPLUS ? Opcode.DADD : Opcode.DSUB);
		} else
			CodeGen.badType(expr);

		if (doDup && !isPost)
			this.bytecode.addOpcode(dup_code);
	}

	protected void atReturnStmnt(Stmnt st) throws CompileError
	{
		this.atReturnStmnt2(st.getLeft());
	}

	protected final void atReturnStmnt2(ASTree result) throws CompileError
	{
		int op;
		if (result == null)
			op = Opcode.RETURN;
		else
		{
			this.compileExpr(result);
			if (this.arrayDim > 0)
				op = Opcode.ARETURN;
			else
			{
				int type = this.exprType;
				if (type == TokenId.DOUBLE)
					op = Opcode.DRETURN;
				else if (type == TokenId.FLOAT)
					op = Opcode.FRETURN;
				else if (type == TokenId.LONG)
					op = Opcode.LRETURN;
				else if (CodeGen.isRefType(type))
					op = Opcode.ARETURN;
				else
					op = Opcode.IRETURN;
			}
		}

		for (ReturnHook har = this.returnHooks; har != null; har = har.next)
			if (har.doit(this.bytecode, op))
			{
				this.hasReturned = true;
				return;
			}

		this.bytecode.addOpcode(op);
		this.hasReturned = true;
	}

	@Override
	public void atStmnt(Stmnt st) throws CompileError
	{
		if (st == null)
			return; // empty

		int op = st.getOperator();
		if (op == TokenId.EXPR)
		{
			ASTree expr = st.getLeft();
			this.doTypeCheck(expr);
			if (expr instanceof AssignExpr)
				this.atAssignExpr((AssignExpr) expr, false);
			else if (CodeGen.isPlusPlusExpr(expr))
			{
				Expr e = (Expr) expr;
				this.atPlusPlus(e.getOperator(), e.oprand1(), e, false);
			} else
			{
				expr.accept(this);
				if (CodeGen.is2word(this.exprType, this.arrayDim))
					this.bytecode.addOpcode(Opcode.POP2);
				else if (this.exprType != TokenId.VOID)
					this.bytecode.addOpcode(Opcode.POP);
			}
		} else if (op == TokenId.DECL || op == TokenId.BLOCK)
		{
			ASTList list = st;
			while (list != null)
			{
				ASTree h = list.head();
				list = list.tail();
				if (h != null)
					h.accept(this);
			}
		} else if (op == TokenId.IF)
			this.atIfStmnt(st);
		else if (op == TokenId.WHILE || op == TokenId.DO)
			this.atWhileStmnt(st, op == TokenId.WHILE);
		else if (op == TokenId.FOR)
			this.atForStmnt(st);
		else if (op == TokenId.BREAK || op == TokenId.CONTINUE)
			this.atBreakStmnt(st, op == TokenId.BREAK);
		else if (op == TokenId.RETURN)
			this.atReturnStmnt(st);
		else if (op == TokenId.THROW)
			this.atThrowStmnt(st);
		else if (op == TokenId.TRY)
			this.atTryStmnt(st);
		else if (op == TokenId.SWITCH)
			this.atSwitchStmnt(st);
		else if (op == TokenId.SYNCHRONIZED)
			this.atSyncStmnt(st);
		else
		{
			// LABEL, SWITCH label stament might be null?.
			this.hasReturned = false;
			throw new CompileError("sorry, not supported statement: TokenId " + op);
		}
	}

	private void atStringConcatExpr(Expr expr, int type1, int dim1, String cname1) throws CompileError
	{
		int type2 = this.exprType;
		int dim2 = this.arrayDim;
		boolean type2Is2 = CodeGen.is2word(type2, dim2);
		boolean type2IsString = type2 == TokenId.CLASS && CodeGen.jvmJavaLangString.equals(this.className);

		if (type2Is2)
			this.convToString(type2, dim2);

		if (CodeGen.is2word(type1, dim1))
		{
			this.bytecode.addOpcode(Opcode.DUP_X2);
			this.bytecode.addOpcode(Opcode.POP);
		} else
			this.bytecode.addOpcode(Opcode.SWAP);

		// even if type1 is String, the left operand might be null.
		this.convToString(type1, dim1);
		this.bytecode.addOpcode(Opcode.SWAP);

		if (!type2Is2 && !type2IsString)
			this.convToString(type2, dim2);

		this.bytecode.addInvokevirtual(CodeGen.javaLangString, "concat", "(Ljava/lang/String;)Ljava/lang/String;");
		this.exprType = TokenId.CLASS;
		this.arrayDim = 0;
		this.className = CodeGen.jvmJavaLangString;
	}

	@Override
	public void atStringL(StringL s) throws CompileError
	{
		this.exprType = TokenId.CLASS;
		this.arrayDim = 0;
		this.className = CodeGen.jvmJavaLangString;
		this.bytecode.addLdc(s.get());
	}

	private void atStringPlusEq(Expr expr, int type, int dim, String cname, ASTree right) throws CompileError
	{
		if (!CodeGen.jvmJavaLangString.equals(cname))
			CodeGen.badAssign(expr);

		this.convToString(type, dim); // the value might be null.
		right.accept(this);
		this.convToString(this.exprType, this.arrayDim);
		this.bytecode.addInvokevirtual(CodeGen.javaLangString, "concat", "(Ljava/lang/String;)Ljava/lang/String;");
		this.exprType = TokenId.CLASS;
		this.arrayDim = 0;
		this.className = CodeGen.jvmJavaLangString;
	}

	private void atSwitchStmnt(Stmnt st) throws CompileError
	{
		this.compileExpr(st.head());

		ArrayList prevBreakList = this.breakList;
		this.breakList = new ArrayList();
		int opcodePc = this.bytecode.currentPc();
		this.bytecode.addOpcode(Opcode.LOOKUPSWITCH);
		int npads = 3 - (opcodePc & 3);
		while (npads-- > 0)
			this.bytecode.add(0);

		Stmnt body = (Stmnt) st.tail();
		int npairs = 0;
		for (ASTList list = body; list != null; list = list.tail())
			if (((Stmnt) list.head()).getOperator() == TokenId.CASE)
				++npairs;

		// opcodePc2 is the position at which the default jump offset is.
		int opcodePc2 = this.bytecode.currentPc();
		this.bytecode.addGap(4);
		this.bytecode.add32bit(npairs);
		this.bytecode.addGap(npairs * 8);

		long[] pairs = new long[npairs];
		int ipairs = 0;
		int defaultPc = -1;
		for (ASTList list = body; list != null; list = list.tail())
		{
			Stmnt label = (Stmnt) list.head();
			int op = label.getOperator();
			if (op == TokenId.DEFAULT)
				defaultPc = this.bytecode.currentPc();
			else if (op != TokenId.CASE)
				CodeGen.fatal();
			else
				pairs[ipairs++] = ((long) this.computeLabel(label.head()) << 32) + ((long) (this.bytecode.currentPc() - opcodePc) & 0xffffffff);

			this.hasReturned = false;
			((Stmnt) label.tail()).accept(this);
		}

		Arrays.sort(pairs);
		int pc = opcodePc2 + 8;
		for (int i = 0; i < npairs; ++i)
		{
			this.bytecode.write32bit(pc, (int) (pairs[i] >>> 32));
			this.bytecode.write32bit(pc + 4, (int) pairs[i]);
			pc += 8;
		}

		if (defaultPc < 0 || this.breakList.size() > 0)
			this.hasReturned = false;

		int endPc = this.bytecode.currentPc();
		if (defaultPc < 0)
			defaultPc = endPc;

		this.bytecode.write32bit(opcodePc2, defaultPc - opcodePc);

		this.patchGoto(this.breakList, endPc);
		this.breakList = prevBreakList;
	}

	@Override
	public void atSymbol(Symbol n) throws CompileError
	{
		CodeGen.fatal();
	}

	private void atSyncStmnt(Stmnt st) throws CompileError
	{
		int nbreaks = CodeGen.getListSize(this.breakList);
		int ncontinues = CodeGen.getListSize(this.continueList);

		this.compileExpr(st.head());
		if (this.exprType != TokenId.CLASS && this.arrayDim == 0)
			throw new CompileError("bad type expr for synchronized block");

		Bytecode bc = this.bytecode;
		final int var = bc.getMaxLocals();
		bc.incMaxLocals(1);
		bc.addOpcode(Opcode.DUP);
		bc.addAstore(var);
		bc.addOpcode(Opcode.MONITORENTER);

		ReturnHook rh = new ReturnHook(this)
		{
			@Override
			protected boolean doit(Bytecode b, int opcode)
			{
				b.addAload(var);
				b.addOpcode(Opcode.MONITOREXIT);
				return false;
			}
		};

		int pc = bc.currentPc();
		Stmnt body = (Stmnt) st.tail();
		if (body != null)
			body.accept(this);

		int pc2 = bc.currentPc();
		int pc3 = 0;
		if (!this.hasReturned)
		{
			rh.doit(bc, 0); // the 2nd arg is ignored.
			bc.addOpcode(Opcode.GOTO);
			pc3 = bc.currentPc();
			bc.addIndex(0);
		}

		if (pc < pc2)
		{ // if the body is not empty
			int pc4 = bc.currentPc();
			rh.doit(bc, 0); // the 2nd arg is ignored.
			bc.addOpcode(Opcode.ATHROW);
			bc.addExceptionHandler(pc, pc2, pc4, 0);
		}

		if (!this.hasReturned)
			bc.write16bit(pc3, bc.currentPc() - pc3 + 1);

		rh.remove(this);

		if (CodeGen.getListSize(this.breakList) != nbreaks || CodeGen.getListSize(this.continueList) != ncontinues)
			throw new CompileError("sorry, cannot break/continue in synchronized block");
	}

	private void atThrowStmnt(Stmnt st) throws CompileError
	{
		ASTree e = st.getLeft();
		this.compileExpr(e);
		if (this.exprType != TokenId.CLASS || this.arrayDim > 0)
			throw new CompileError("bad throw statement");

		this.bytecode.addOpcode(Opcode.ATHROW);
		this.hasReturned = true;
	}

	/*
	 * overridden in MemberCodeGen
	 */
	protected void atTryStmnt(Stmnt st) throws CompileError
	{
		this.hasReturned = false;
	}

	@Override
	public void atVariable(Variable v) throws CompileError
	{
		Declarator d = v.getDeclarator();
		this.exprType = d.getType();
		this.arrayDim = d.getArrayDim();
		this.className = d.getClassName();
		int var = this.getLocalVar(d);

		if (this.arrayDim > 0)
			this.bytecode.addAload(var);
		else
			switch (this.exprType)
			{
				case CLASS:
					this.bytecode.addAload(var);
					break;
				case LONG:
					this.bytecode.addLload(var);
					break;
				case FLOAT:
					this.bytecode.addFload(var);
					break;
				case DOUBLE:
					this.bytecode.addDload(var);
					break;
				default: // BOOLEAN, BYTE, CHAR, SHORT, INT
					this.bytecode.addIload(var);
					break;
			}
	}

	/*
	 * op is either =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, or >>>=. expr
	 * and var can be null.
	 */
	private void atVariableAssign(Expr expr, int op, Variable var, Declarator d, ASTree right, boolean doDup) throws CompileError
	{
		int varType = d.getType();
		int varArray = d.getArrayDim();
		String varClass = d.getClassName();
		int varNo = this.getLocalVar(d);

		if (op != '=')
			this.atVariable(var);

		// expr is null if the caller is atDeclarator().
		if (expr == null && right instanceof ArrayInit)
			this.atArrayVariableAssign((ArrayInit) right, varType, varArray, varClass);
		else
			this.atAssignCore(expr, op, right, varType, varArray, varClass);

		if (doDup)
			if (CodeGen.is2word(varType, varArray))
				this.bytecode.addOpcode(Opcode.DUP2);
			else
				this.bytecode.addOpcode(Opcode.DUP);

		if (varArray > 0)
			this.bytecode.addAstore(varNo);
		else if (varType == TokenId.DOUBLE)
			this.bytecode.addDstore(varNo);
		else if (varType == TokenId.FLOAT)
			this.bytecode.addFstore(varNo);
		else if (varType == TokenId.LONG)
			this.bytecode.addLstore(varNo);
		else if (CodeGen.isRefType(varType))
			this.bytecode.addAstore(varNo);
		else
			this.bytecode.addIstore(varNo);

		this.exprType = varType;
		this.arrayDim = varArray;
		this.className = varClass;
	}

	private void atWhileStmnt(Stmnt st, boolean notDo) throws CompileError
	{
		ArrayList prevBreakList = this.breakList;
		ArrayList prevContList = this.continueList;
		this.breakList = new ArrayList();
		this.continueList = new ArrayList();

		ASTree expr = st.head();
		Stmnt body = (Stmnt) st.tail();

		int pc = 0;
		if (notDo)
		{
			this.bytecode.addOpcode(Opcode.GOTO);
			pc = this.bytecode.currentPc();
			this.bytecode.addIndex(0);
		}

		int pc2 = this.bytecode.currentPc();
		if (body != null)
			body.accept(this);

		int pc3 = this.bytecode.currentPc();
		if (notDo)
			this.bytecode.write16bit(pc, pc3 - pc + 1);

		boolean alwaysBranch = this.compileBooleanExpr(true, expr);
		if (alwaysBranch)
		{
			this.bytecode.addOpcode(Opcode.GOTO);
			alwaysBranch = this.breakList.size() == 0;
		}

		this.bytecode.addIndex(pc2 - this.bytecode.currentPc() + 1);
		this.patchGoto(this.breakList, this.bytecode.currentPc());
		this.patchGoto(this.continueList, pc3);
		this.continueList = prevContList;
		this.breakList = prevBreakList;
		this.hasReturned = alwaysBranch;
	}

	/*
	 * Produces the opcode to branch if the condition is true. The oprand
	 * (branch offset) is not produced.
	 * @return true if the compiled code is GOTO (always branch). GOTO is not
	 * produced.
	 */
	private boolean booleanExpr(boolean branchIf, ASTree expr) throws CompileError
	{
		boolean isAndAnd;
		int op = CodeGen.getCompOperator(expr);
		if (op == TokenId.EQ)
		{ // ==, !=, ...
			BinExpr bexpr = (BinExpr) expr;
			int type1 = this.compileOprands(bexpr);
			// here, arrayDim might represent the array dim. of the left oprand
			// if the right oprand is NULL.
			this.compareExpr(branchIf, bexpr.getOperator(), type1, bexpr);
		} else if (op == '!')
			return this.booleanExpr(!branchIf, ((Expr) expr).oprand1());
		else if ((isAndAnd = op == TokenId.ANDAND) || op == TokenId.OROR)
		{
			BinExpr bexpr = (BinExpr) expr;
			if (this.booleanExpr(!isAndAnd, bexpr.oprand1()))
			{
				this.exprType = TokenId.BOOLEAN;
				this.arrayDim = 0;
				return true;
			} else
			{
				int pc = this.bytecode.currentPc();
				this.bytecode.addIndex(0); // correct later
				if (this.booleanExpr(isAndAnd, bexpr.oprand2()))
					this.bytecode.addOpcode(Opcode.GOTO);

				this.bytecode.write16bit(pc, this.bytecode.currentPc() - pc + 3);
				if (branchIf != isAndAnd)
				{
					this.bytecode.addIndex(6); // skip GOTO instruction
					this.bytecode.addOpcode(Opcode.GOTO);
				}
			}
		} else if (CodeGen.isAlwaysBranch(expr, branchIf))
		{
			// Opcode.GOTO is not added here. The caller must add it.
			this.exprType = TokenId.BOOLEAN;
			this.arrayDim = 0;
			return true; // always branch
		} else
		{ // others
			expr.accept(this);
			if (this.exprType != TokenId.BOOLEAN || this.arrayDim != 0)
				throw new CompileError("boolean expr is required");

			this.bytecode.addOpcode(branchIf ? Opcode.IFNE : Opcode.IFEQ);
		}

		this.exprType = TokenId.BOOLEAN;
		this.arrayDim = 0;
		return false;
	}

	private String checkCastExpr(CastExpr expr, String name) throws CompileError
	{
		final String msg = "invalid cast";
		ASTree oprand = expr.getOprand();
		int dim = expr.getArrayDim();
		int type = expr.getType();
		oprand.accept(this);
		int srcType = this.exprType;
		if (this.invalidDim(srcType, this.arrayDim, this.className, type, dim, name, true) || srcType == TokenId.VOID || type == TokenId.VOID)
			throw new CompileError(msg);

		if (type == TokenId.CLASS)
		{
			if (!CodeGen.isRefType(srcType))
				throw new CompileError(msg);

			return CodeGen.toJvmArrayName(name, dim);
		} else if (dim > 0)
			return CodeGen.toJvmTypeName(type, dim);
		else
			return null; // built-in type
	}

	/*
	 * Produces the opcode to branch if the condition is true. The oprands are
	 * not produced. Parameter expr - compare expression ==, !=, <=, >=, <, >
	 */
	private void compareExpr(boolean branchIf, int token, int type1, BinExpr expr) throws CompileError
	{
		if (this.arrayDim == 0)
			this.convertOprandTypes(type1, this.exprType, expr);

		int p = CodeGen.typePrecedence(this.exprType);
		if (p == CodeGen.P_OTHER || this.arrayDim > 0)
			if (token == TokenId.EQ)
				this.bytecode.addOpcode(branchIf ? Opcode.IF_ACMPEQ : Opcode.IF_ACMPNE);
			else if (token == TokenId.NEQ)
				this.bytecode.addOpcode(branchIf ? Opcode.IF_ACMPNE : Opcode.IF_ACMPEQ);
			else
				CodeGen.badTypes(expr);
		else if (p == CodeGen.P_INT)
		{
			int op[] = CodeGen.ifOp;
			for (int i = 0; i < op.length; i += 3)
				if (op[i] == token)
				{
					this.bytecode.addOpcode(op[i + (branchIf ? 1 : 2)]);
					return;
				}

			CodeGen.badTypes(expr);
		} else
		{
			if (p == CodeGen.P_DOUBLE)
				if (token == '<' || token == TokenId.LE)
					this.bytecode.addOpcode(Opcode.DCMPG);
				else
					this.bytecode.addOpcode(Opcode.DCMPL);
			else if (p == CodeGen.P_FLOAT)
				if (token == '<' || token == TokenId.LE)
					this.bytecode.addOpcode(Opcode.FCMPG);
				else
					this.bytecode.addOpcode(Opcode.FCMPL);
			else if (p == CodeGen.P_LONG)
				this.bytecode.addOpcode(Opcode.LCMP); // 1: >, 0: =, -1: <
			else
				CodeGen.fatal();

			int[] op = CodeGen.ifOp2;
			for (int i = 0; i < op.length; i += 3)
				if (op[i] == token)
				{
					this.bytecode.addOpcode(op[i + (branchIf ? 1 : 2)]);
					return;
				}

			CodeGen.badTypes(expr);
		}
	}

	public boolean compileBooleanExpr(boolean branchIf, ASTree expr) throws CompileError
	{
		this.doTypeCheck(expr);
		return this.booleanExpr(branchIf, expr);
	}

	public void compileExpr(ASTree expr) throws CompileError
	{
		this.doTypeCheck(expr);
		expr.accept(this);
	}

	private int compileOprands(BinExpr expr) throws CompileError
	{
		expr.oprand1().accept(this);
		int type1 = this.exprType;
		int dim1 = this.arrayDim;
		expr.oprand2().accept(this);
		if (dim1 != this.arrayDim)
			if (type1 != TokenId.NULL && this.exprType != TokenId.NULL)
				throw new CompileError("incompatible array types");
			else if (this.exprType == TokenId.NULL)
				this.arrayDim = dim1;

		if (type1 == TokenId.NULL)
			return this.exprType;
		else
			return type1;
	}

	private int computeLabel(ASTree expr) throws CompileError
	{
		this.doTypeCheck(expr);
		expr = TypeChecker.stripPlusExpr(expr);
		if (expr instanceof IntConst)
			return (int) ((IntConst) expr).get();
		else
			throw new CompileError("bad case label");
	}

	/*
	 * do implicit type conversion. arrayDim values of the two oprands must be
	 * zero.
	 */
	private void convertOprandTypes(int type1, int type2, Expr expr) throws CompileError
	{
		boolean rightStrong;
		int type1_p = CodeGen.typePrecedence(type1);
		int type2_p = CodeGen.typePrecedence(type2);

		if (type2_p < 0 && type1_p < 0)   // not primitive types
			return;

		if (type2_p < 0 || type1_p < 0)   // either is not a primitive type
			CodeGen.badTypes(expr);

		int op, result_type;
		if (type1_p <= type2_p)
		{
			rightStrong = false;
			this.exprType = type1;
			op = CodeGen.castOp[type2_p * 4 + type1_p];
			result_type = type1_p;
		} else
		{
			rightStrong = true;
			op = CodeGen.castOp[type1_p * 4 + type2_p];
			result_type = type2_p;
		}

		if (rightStrong)
		{
			if (result_type == CodeGen.P_DOUBLE || result_type == CodeGen.P_LONG)
			{
				if (type1_p == CodeGen.P_DOUBLE || type1_p == CodeGen.P_LONG)
					this.bytecode.addOpcode(Opcode.DUP2_X2);
				else
					this.bytecode.addOpcode(Opcode.DUP2_X1);

				this.bytecode.addOpcode(Opcode.POP2);
				this.bytecode.addOpcode(op);
				this.bytecode.addOpcode(Opcode.DUP2_X2);
				this.bytecode.addOpcode(Opcode.POP2);
			} else if (result_type == CodeGen.P_FLOAT)
			{
				if (type1_p == CodeGen.P_LONG)
				{
					this.bytecode.addOpcode(Opcode.DUP_X2);
					this.bytecode.addOpcode(Opcode.POP);
				} else
					this.bytecode.addOpcode(Opcode.SWAP);

				this.bytecode.addOpcode(op);
				this.bytecode.addOpcode(Opcode.SWAP);
			} else
				CodeGen.fatal();
		} else if (op != Opcode.NOP)
			this.bytecode.addOpcode(op);
	}

	private void convToString(int type, int dim) throws CompileError
	{
		final String method = "valueOf";

		if (CodeGen.isRefType(type) || dim > 0)
			this.bytecode.addInvokestatic(CodeGen.javaLangString, method, "(Ljava/lang/Object;)Ljava/lang/String;");
		else if (type == TokenId.DOUBLE)
			this.bytecode.addInvokestatic(CodeGen.javaLangString, method, "(D)Ljava/lang/String;");
		else if (type == TokenId.FLOAT)
			this.bytecode.addInvokestatic(CodeGen.javaLangString, method, "(F)Ljava/lang/String;");
		else if (type == TokenId.LONG)
			this.bytecode.addInvokestatic(CodeGen.javaLangString, method, "(J)Ljava/lang/String;");
		else if (type == TokenId.BOOLEAN)
			this.bytecode.addInvokestatic(CodeGen.javaLangString, method, "(Z)Ljava/lang/String;");
		else if (type == TokenId.CHAR)
			this.bytecode.addInvokestatic(CodeGen.javaLangString, method, "(C)Ljava/lang/String;");
		else if (type == TokenId.VOID)
			throw new CompileError("void type expression");
		else
			/* INT, BYTE, SHORT */
			this.bytecode.addInvokestatic(CodeGen.javaLangString, method, "(I)Ljava/lang/String;");
	}

	public void doTypeCheck(ASTree expr) throws CompileError
	{
		if (this.typeChecker != null)
			expr.accept(this.typeChecker);
	}

	protected int getLocalVar(Declarator d)
	{
		int v = d.getLocalVar();
		if (v < 0)
		{
			v = this.getMaxLocals(); // delayed variable allocation.
			d.setLocalVar(v);
			this.incMaxLocals(1);
		}

		return v;
	}

	public int getMaxLocals()
	{
		return this.bytecode.getMaxLocals();
	}

	/**
	 * Returns the JVM-internal representation of this super class name.
	 */
	protected abstract String getSuperName() throws CompileError;

	/**
	 * Returns a local variable that single or double words can be stored in.
	 */
	protected int getTempVar()
	{
		if (this.tempVar < 0)
		{
			this.tempVar = this.getMaxLocals();
			this.incMaxLocals(2);
		}

		return this.tempVar;
	}

	/**
	 * Returns the JVM-internal representation of this class name.
	 */
	protected abstract String getThisName();

	protected void incMaxLocals(int size)
	{
		this.bytecode.incMaxLocals(size);
	}

	protected abstract void insertDefaultSuperCall() throws CompileError;

	private boolean invalidDim(int srcType, int srcDim, String srcClass, int destType, int destDim, String destClass, boolean isCast)
	{
		if (srcDim != destDim)
			if (srcType == TokenId.NULL)
				return false;
			else if (destDim == 0 && destType == TokenId.CLASS && CodeGen.jvmJavaLangObject.equals(destClass))
				return false;
			else if (isCast && srcDim == 0 && srcType == TokenId.CLASS && CodeGen.jvmJavaLangObject.equals(srcClass))
				return false;
			else
				return true;

		return false;
	}

	private boolean needsSuperCall(Stmnt body) throws CompileError
	{
		if (body.getOperator() == TokenId.BLOCK)
			body = (Stmnt) body.head();

		if (body != null && body.getOperator() == TokenId.EXPR)
		{
			ASTree expr = body.head();
			if (expr != null && expr instanceof Expr && ((Expr) expr).getOperator() == TokenId.CALL)
			{
				ASTree target = ((Expr) expr).head();
				if (target instanceof Keyword)
				{
					int token = ((Keyword) target).get();
					return token != TokenId.THIS && token != TokenId.SUPER;
				}
			}
		}

		return true;
	}

	protected void patchGoto(ArrayList list, int targetPc)
	{
		int n = list.size();
		for (int i = 0; i < n; ++i)
		{
			int pc = ((Integer) list.get(i)).intValue();
			this.bytecode.write16bit(pc, targetPc - pc + 1);
		}
	}

	/*
	 * Converts a class name into a JVM-internal representation. It may also
	 * expand a simple class name to java.lang.*. For example, this converts
	 * Object into java/lang/Object.
	 */
	protected abstract String resolveClassName(ASTList name) throws CompileError;

	/*
	 * Expands a simple class name to java.lang.*. For example, this converts
	 * Object into java/lang/Object.
	 */
	protected abstract String resolveClassName(String jvmClassName) throws CompileError;

	public void setMaxLocals(int n)
	{
		this.bytecode.setMaxLocals(n);
	}

	public void setTypeChecker(TypeChecker checker)
	{
		this.typeChecker = checker;
	}
}
