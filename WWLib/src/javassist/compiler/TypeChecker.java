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
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
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
import javassist.compiler.ast.InstanceOfExpr;
import javassist.compiler.ast.IntConst;
import javassist.compiler.ast.Keyword;
import javassist.compiler.ast.Member;
import javassist.compiler.ast.NewExpr;
import javassist.compiler.ast.StringL;
import javassist.compiler.ast.Symbol;
import javassist.compiler.ast.Variable;
import javassist.compiler.ast.Visitor;

public class TypeChecker extends Visitor implements Opcode, TokenId
{
	static final String	javaLangObject		= "java.lang.Object";
	static final String	jvmJavaLangObject	= "java/lang/Object";
	static final String	jvmJavaLangString	= "java/lang/String";
	static final String	jvmJavaLangClass	= "java/lang/Class";

	/*
	 * Converts an array of tuples of exprType, arrayDim, and className into a
	 * String object.
	 */
	protected static String argTypesToString(int[] types, int[] dims, String[] cnames)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append('(');
		int n = types.length;
		if (n > 0)
		{
			int i = 0;
			while (true)
			{
				TypeChecker.typeToString(sbuf, types[i], dims[i], cnames[i]);
				if (++i < n)
					sbuf.append(',');
				else
					break;
			}
		}

		sbuf.append(')');
		return sbuf.toString();
	}

	private static void badMethod() throws CompileError
	{
		throw new CompileError("bad method");
	}

	protected static void fatal() throws CompileError
	{
		throw new CompileError("fatal");
	}

	public static ASTree getConstantFieldValue(CtField f)
	{
		if (f == null)
			return null;

		Object value = f.getConstantValue();
		if (value == null)
			return null;

		if (value instanceof String)
			return new StringL((String) value);
		else if (value instanceof Double || value instanceof Float)
		{
			int token = value instanceof Double ? TokenId.DoubleConstant : TokenId.FloatConstant;
			return new DoubleConst(((Number) value).doubleValue(), token);
		} else if (value instanceof Number)
		{
			int token = value instanceof Long ? TokenId.LongConstant : TokenId.IntConstant;
			return new IntConst(((Number) value).longValue(), token);
		} else if (value instanceof Boolean)
			return new Keyword(((Boolean) value).booleanValue() ? TokenId.TRUE : TokenId.FALSE);
		else
			return null;
	}

	/**
	 * If MEM is a static final field, this method returns a constant expression
	 * representing the value of that field.
	 */
	private static ASTree getConstantFieldValue(Member mem)
	{
		return TypeChecker.getConstantFieldValue(mem.getField());
	}

	/**
	 * Returns non-null if target is something like Foo.super for accessing the
	 * default method in an interface. Otherwise, null.
	 *
	 * @return the class name followed by {@code .super} or null.
	 */
	static String isDotSuper(ASTree target)
	{
		if (target instanceof Expr)
		{
			Expr e = (Expr) target;
			if (e.getOperator() == '.')
			{
				ASTree right = e.oprand2();
				if (right instanceof Keyword && ((Keyword) right).get() == TokenId.SUPER)
					return ((Symbol) e.oprand1()).get();
			}
		}

		return null;
	}

	private static boolean isPlusExpr(ASTree expr)
	{
		if (expr instanceof BinExpr)
		{
			BinExpr bexpr = (BinExpr) expr;
			int token = bexpr.getOperator();
			return token == '+';
		}

		return false;
	}

	private static Expr makeAppendCall(ASTree target, ASTree arg)
	{
		return CallExpr.makeCall(Expr.make('.', target, new Member("append")), new ASTList(arg));
	}

	/*
	 * CodeGen.atSwitchStmnt() also calls stripPlusExpr().
	 */
	static ASTree stripPlusExpr(ASTree expr)
	{
		if (expr instanceof BinExpr)
		{
			BinExpr e = (BinExpr) expr;
			if (e.getOperator() == '+' && e.oprand2() == null)
				return e.getLeft();
		} else if (expr instanceof Expr)
		{ // note: BinExpr extends Expr.
			Expr e = (Expr) expr;
			int op = e.getOperator();
			if (op == TokenId.MEMBER)
			{
				ASTree cexpr = TypeChecker.getConstantFieldValue((Member) e.oprand2());
				if (cexpr != null)
					return cexpr;
			} else if (op == '+' && e.getRight() == null)
				return e.getLeft();
		} else if (expr instanceof Member)
		{
			ASTree cexpr = TypeChecker.getConstantFieldValue((Member) expr);
			if (cexpr != null)
				return cexpr;
		}

		return expr;
	}

	/*
	 * Converts a tuple of exprType, arrayDim, and className into a String
	 * object.
	 */
	protected static StringBuffer typeToString(StringBuffer sbuf, int type, int dim, String cname)
	{
		String s;
		if (type == TokenId.CLASS)
			s = MemberResolver.jvmToJavaName(cname);
		else if (type == TokenId.NULL)
			s = "Object";
		else
			try
			{
				s = MemberResolver.getTypeName(type);
			} catch (CompileError e)
			{
				s = "?";
			}

		sbuf.append(s);
		while (dim-- > 0)
			sbuf.append("[]");

		return sbuf;
	}

	/*
	 * The following fields are used by atXXX() methods for returning the type
	 * of the compiled expression.
	 */
	protected int				exprType;	// VOID, NULL, CLASS, BOOLEAN, INT,
											// ...

	protected int				arrayDim;

	protected String			className;	// JVM-internal representation

	protected MemberResolver	resolver;

	protected CtClass			thisClass;

	protected MethodInfo		thisMethod;

	public TypeChecker(CtClass cc, ClassPool cp)
	{
		this.resolver = new MemberResolver(cp);
		this.thisClass = cc;
		this.thisMethod = null;
	}

	private void atArrayAssign(Expr expr, int op, Expr array, ASTree right) throws CompileError
	{
		this.atArrayRead(array.oprand1(), array.oprand2());
		int aType = this.exprType;
		int aDim = this.arrayDim;
		String cname = this.className;
		right.accept(this);
		this.exprType = aType;
		this.arrayDim = aDim;
		this.className = cname;
	}

	@Override
	public void atArrayInit(ArrayInit init) throws CompileError
	{
		ASTList list = init;
		while (list != null)
		{
			ASTree h = list.head();
			list = list.tail();
			if (h != null)
				h.accept(this);
		}
	}

	public void atArrayLength(Expr expr) throws CompileError
	{
		expr.oprand1().accept(this);
		if (this.arrayDim == 0)
			throw new NoFieldException("length", expr);

		this.exprType = TokenId.INT;
		this.arrayDim = 0;
	}

	public void atArrayRead(ASTree array, ASTree index) throws CompileError
	{
		array.accept(this);
		int type = this.exprType;
		int dim = this.arrayDim;
		String cname = this.className;
		index.accept(this);
		this.exprType = type;
		this.arrayDim = dim - 1;
		this.className = cname;
	}

	@Override
	public void atAssignExpr(AssignExpr expr) throws CompileError
	{
		// =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, >>>=
		int op = expr.getOperator();
		ASTree left = expr.oprand1();
		ASTree right = expr.oprand2();
		if (left instanceof Variable)
			this.atVariableAssign(expr, op, (Variable) left, ((Variable) left).getDeclarator(), right);
		else
		{
			if (left instanceof Expr)
			{
				Expr e = (Expr) left;
				if (e.getOperator() == TokenId.ARRAY)
				{
					this.atArrayAssign(expr, op, (Expr) left, right);
					return;
				}
			}

			this.atFieldAssign(expr, op, left, right);
		}
	}

	/*
	 * If atBinExpr() substitutes a new expression for the original
	 * binary-operator expression, it changes the operator name to '+' (if the
	 * original is not '+') and sets the new expression to the left-hand-side
	 * expression and null to the right-hand-side expression.
	 */
	@Override
	public void atBinExpr(BinExpr expr) throws CompileError
	{
		int token = expr.getOperator();
		int k = CodeGen.lookupBinOp(token);
		if (k >= 0)
		{
			/*
			 * arithmetic operators: +, -, *, /, %, |, ^, &, <<, >>, >>>
			 */
			if (token == '+')
			{
				Expr e = this.atPlusExpr(expr);
				if (e != null)
				{
					/*
					 * String concatenation has been translated into an
					 * expression using StringBuffer.
					 */
					e = CallExpr.makeCall(Expr.make('.', e, new Member("toString")), null);
					expr.setOprand1(e);
					expr.setOprand2(null); // <---- look at this!
					this.className = TypeChecker.jvmJavaLangString;
				}
			} else
			{
				ASTree left = expr.oprand1();
				ASTree right = expr.oprand2();
				left.accept(this);
				int type1 = this.exprType;
				right.accept(this);
				if (!this.isConstant(expr, token, left, right))
					this.computeBinExprType(expr, token, type1);
			}
		} else
			/*
			 * equation: &&, ||, ==, !=, <=, >=, <, >
			 */
			this.booleanExpr(expr);
	}

	@Override
	public void atCallExpr(CallExpr expr) throws CompileError
	{
		String mname = null;
		CtClass targetClass = null;
		ASTree method = expr.oprand1();
		ASTList args = (ASTList) expr.oprand2();

		if (method instanceof Member)
		{
			mname = ((Member) method).get();
			targetClass = this.thisClass;
		} else if (method instanceof Keyword)
		{ // constructor
			mname = MethodInfo.nameInit; // <init>
			if (((Keyword) method).get() == TokenId.SUPER)
				targetClass = MemberResolver.getSuperclass(this.thisClass);
			else
				targetClass = this.thisClass;
		} else if (method instanceof Expr)
		{
			Expr e = (Expr) method;
			mname = ((Symbol) e.oprand2()).get();
			int op = e.getOperator();
			if (op == TokenId.MEMBER) // static method
				targetClass = this.resolver.lookupClass(((Symbol) e.oprand1()).get(), false);
			else if (op == '.')
			{
				ASTree target = e.oprand1();
				String classFollowedByDotSuper = TypeChecker.isDotSuper(target);
				if (classFollowedByDotSuper != null)
					targetClass = MemberResolver.getSuperInterface(this.thisClass, classFollowedByDotSuper);
				else
				{
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
						e.setOperator(TokenId.MEMBER);
						e.setOprand1(new Symbol(MemberResolver.jvmToJavaName(this.className)));
					}

					if (this.arrayDim > 0)
						targetClass = this.resolver.lookupClass(TypeChecker.javaLangObject, true);
					else if (this.exprType == TokenId.CLASS /* && arrayDim == 0 */)
						targetClass = this.resolver.lookupClassByJvmName(this.className);
					else
						TypeChecker.badMethod();
				}
			} else
				TypeChecker.badMethod();
		} else
			TypeChecker.fatal();

		MemberResolver.Method minfo = this.atMethodCallCore(targetClass, mname, args);
		expr.setMethod(minfo);
	}

	@Override
	public void atCastExpr(CastExpr expr) throws CompileError
	{
		String cname = this.resolveClassName(expr.getClassName());
		expr.getOprand().accept(this);
		this.exprType = expr.getType();
		this.arrayDim = expr.getArrayDim();
		this.className = cname;
	}

	public void atClassObject(Expr expr) throws CompileError
	{
		this.exprType = TokenId.CLASS;
		this.arrayDim = 0;
		this.className = TypeChecker.jvmJavaLangClass;
	}

	@Override
	public void atCondExpr(CondExpr expr) throws CompileError
	{
		this.booleanExpr(expr.condExpr());
		expr.thenExpr().accept(this);
		int type1 = this.exprType;
		int dim1 = this.arrayDim;
		String cname1 = this.className;
		expr.elseExpr().accept(this);

		if (dim1 == 0 && dim1 == this.arrayDim)
			if (CodeGen.rightIsStrong(type1, this.exprType))
				expr.setThen(new CastExpr(this.exprType, 0, expr.thenExpr()));
			else if (CodeGen.rightIsStrong(this.exprType, type1))
			{
				expr.setElse(new CastExpr(type1, 0, expr.elseExpr()));
				this.exprType = type1;
			}
	}

	@Override
	public void atDoubleConst(DoubleConst d) throws CompileError
	{
		this.arrayDim = 0;
		if (d.getType() == TokenId.DoubleConstant)
			this.exprType = TokenId.DOUBLE;
		else
			this.exprType = TokenId.FLOAT;
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
			if (member.equals("length"))
				try
				{
					this.atArrayLength(expr);
				} catch (NoFieldException nfe)
				{
					// length might be a class or package name.
					this.atFieldRead(expr);
				}
			else if (member.equals("class"))
				this.atClassObject(expr); // .class
			else
				this.atFieldRead(expr);
		} else if (token == TokenId.MEMBER)
		{ // field read
			String member = ((Symbol) expr.oprand2()).get();
			if (member.equals("class"))
				this.atClassObject(expr); // .class
			else
				this.atFieldRead(expr);
		} else if (token == TokenId.ARRAY)
			this.atArrayRead(oprand, expr.oprand2());
		else if (token == TokenId.PLUSPLUS || token == TokenId.MINUSMINUS)
			this.atPlusPlus(token, oprand, expr);
		else if (token == '!')
			this.booleanExpr(expr);
		else if (token == TokenId.CALL) // method call
			TypeChecker.fatal();
		else
		{
			oprand.accept(this);
			if (!this.isConstant(expr, token, oprand))
				if (token == '-' || token == '~')
					if (CodeGen.isP_INT(this.exprType))
						this.exprType = TokenId.INT; // type may be BYTE, ...
		}
	}

	protected void atFieldAssign(Expr expr, int op, ASTree left, ASTree right) throws CompileError
	{
		CtField f = this.fieldAccess(left);
		this.atFieldRead(f);
		int fType = this.exprType;
		int fDim = this.arrayDim;
		String cname = this.className;
		right.accept(this);
		this.exprType = fType;
		this.arrayDim = fDim;
		this.className = cname;
	}

	protected void atFieldPlusPlus(ASTree oprand) throws CompileError
	{
		CtField f = this.fieldAccess(oprand);
		this.atFieldRead(f);
		int t = this.exprType;
		if (t == TokenId.INT || t == TokenId.BYTE || t == TokenId.CHAR || t == TokenId.SHORT)
			this.exprType = TokenId.INT;
	}

	private void atFieldRead(ASTree expr) throws CompileError
	{
		this.atFieldRead(this.fieldAccess(expr));
	}

	private void atFieldRead(CtField f) throws CompileError
	{
		FieldInfo finfo = f.getFieldInfo2();
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
	}

	@Override
	public void atInstanceOfExpr(InstanceOfExpr expr) throws CompileError
	{
		expr.getOprand().accept(this);
		this.exprType = TokenId.BOOLEAN;
		this.arrayDim = 0;
	}

	@Override
	public void atIntConst(IntConst i) throws CompileError
	{
		this.arrayDim = 0;
		int type = i.getType();
		if (type == TokenId.IntConstant || type == TokenId.CharConstant)
			this.exprType = type == TokenId.IntConstant ? TokenId.INT : TokenId.CHAR;
		else
			this.exprType = TokenId.LONG;
	}

	@Override
	public void atKeyword(Keyword k) throws CompileError
	{
		this.arrayDim = 0;
		int token = k.get();
		switch (token)
		{
			case TRUE:
			case FALSE:
				this.exprType = TokenId.BOOLEAN;
				break;
			case NULL:
				this.exprType = TokenId.NULL;
				break;
			case THIS:
			case SUPER:
				this.exprType = TokenId.CLASS;
				if (token == TokenId.THIS)
					this.className = this.getThisName();
				else
					this.className = this.getSuperName();
				break;
			default:
				TypeChecker.fatal();
		}
	}

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

	/**
	 * @return a pair of the class declaring the invoked method and the
	 *         MethodInfo of that method. Never null.
	 */
	public MemberResolver.Method atMethodCallCore(CtClass targetClass, String mname, ASTList args) throws CompileError
	{
		int nargs = this.getMethodArgsLength(args);
		int[] types = new int[nargs];
		int[] dims = new int[nargs];
		String[] cnames = new String[nargs];
		this.atMethodArgs(args, types, dims, cnames);

		MemberResolver.Method found = this.resolver.lookupMethod(targetClass, this.thisClass, this.thisMethod, mname, types, dims, cnames);
		if (found == null)
		{
			String clazz = targetClass.getName();
			String signature = TypeChecker.argTypesToString(types, dims, cnames);
			String msg;
			if (mname.equals(MethodInfo.nameInit))
				msg = "cannot find constructor " + clazz + signature;
			else
				msg = mname + signature + " not found in " + clazz;

			throw new CompileError(msg);
		}

		String desc = found.info.getDescriptor();
		this.setReturnType(desc);
		return found;
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
		}

		this.exprType = type;
		this.arrayDim = dim;
		if (type == TokenId.CLASS)
			this.className = this.resolveClassName(classname);
		else
			this.className = null;
	}

	public void atNewArrayExpr(NewExpr expr) throws CompileError
	{
		int type = expr.getArrayType();
		ASTList size = expr.getArraySize();
		ASTList classname = expr.getClassName();
		ASTree init = expr.getInitializer();
		if (init != null)
			init.accept(this);

		if (size.length() > 1)
			this.atMultiNewArray(type, classname, size);
		else
		{
			ASTree sizeExpr = size.head();
			if (sizeExpr != null)
				sizeExpr.accept(this);

			this.exprType = type;
			this.arrayDim = 1;
			if (type == TokenId.CLASS)
				this.className = this.resolveClassName(classname);
			else
				this.className = null;
		}
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
			this.atMethodCallCore(clazz, MethodInfo.nameInit, args);
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = MemberResolver.javaToJvmName(cname);
		}
	}

	/*
	 * EXPR must be a + expression. atPlusExpr() returns non-null if the given
	 * expression is string concatenation. The returned value is
	 * "new StringBuffer().append..".
	 */
	private Expr atPlusExpr(BinExpr expr) throws CompileError
	{
		ASTree left = expr.oprand1();
		ASTree right = expr.oprand2();
		if (right == null)
		{
			// this expression has been already type-checked.
			// see atBinExpr() above.
			left.accept(this);
			return null;
		}

		if (TypeChecker.isPlusExpr(left))
		{
			Expr newExpr = this.atPlusExpr((BinExpr) left);
			if (newExpr != null)
			{
				right.accept(this);
				this.exprType = TokenId.CLASS;
				this.arrayDim = 0;
				this.className = "java/lang/StringBuffer";
				return TypeChecker.makeAppendCall(newExpr, right);
			}
		} else
			left.accept(this);

		int type1 = this.exprType;
		int dim1 = this.arrayDim;
		String cname = this.className;
		right.accept(this);

		if (this.isConstant(expr, '+', left, right))
			return null;

		if (type1 == TokenId.CLASS && dim1 == 0 && TypeChecker.jvmJavaLangString.equals(cname) || this.exprType == TokenId.CLASS && this.arrayDim == 0
				&& TypeChecker.jvmJavaLangString.equals(this.className))
		{
			ASTList sbufClass = ASTList.make(new Symbol("java"), new Symbol("lang"), new Symbol("StringBuffer"));
			ASTree e = new NewExpr(sbufClass, null);
			this.exprType = TokenId.CLASS;
			this.arrayDim = 0;
			this.className = "java/lang/StringBuffer";
			return TypeChecker.makeAppendCall(TypeChecker.makeAppendCall(e, left), right);
		} else
		{
			this.computeBinExprType(expr, '+', type1);
			return null;
		}
	}

	private void atPlusPlus(int token, ASTree oprand, Expr expr) throws CompileError
	{
		boolean isPost = oprand == null; // ++i or i++?
		if (isPost)
			oprand = expr.oprand2();

		if (oprand instanceof Variable)
		{
			Declarator d = ((Variable) oprand).getDeclarator();
			this.exprType = d.getType();
			this.arrayDim = d.getArrayDim();
		} else
		{
			if (oprand instanceof Expr)
			{
				Expr e = (Expr) oprand;
				if (e.getOperator() == TokenId.ARRAY)
				{
					this.atArrayRead(e.oprand1(), e.oprand2());
					// arrayDim should be 0.
					int t = this.exprType;
					if (t == TokenId.INT || t == TokenId.BYTE || t == TokenId.CHAR || t == TokenId.SHORT)
						this.exprType = TokenId.INT;

					return;
				}
			}

			this.atFieldPlusPlus(oprand);
		}
	}

	@Override
	public void atStringL(StringL s) throws CompileError
	{
		this.exprType = TokenId.CLASS;
		this.arrayDim = 0;
		this.className = TypeChecker.jvmJavaLangString;
	}

	@Override
	public void atVariable(Variable v) throws CompileError
	{
		Declarator d = v.getDeclarator();
		this.exprType = d.getType();
		this.arrayDim = d.getArrayDim();
		this.className = d.getClassName();
	}

	/*
	 * op is either =, %=, &=, *=, /=, +=, -=, ^=, |=, <<=, >>=, or >>>=.
	 * 
	 * expr and var can be null.
	 */
	private void atVariableAssign(Expr expr, int op, Variable var, Declarator d, ASTree right) throws CompileError
	{
		int varType = d.getType();
		int varArray = d.getArrayDim();
		String varClass = d.getClassName();

		if (op != '=')
			this.atVariable(var);

		right.accept(this);
		this.exprType = varType;
		this.arrayDim = varArray;
		this.className = varClass;
	}

	private void booleanExpr(ASTree expr) throws CompileError
	{
		int op = CodeGen.getCompOperator(expr);
		if (op == TokenId.EQ)
		{ // ==, !=, ...
			BinExpr bexpr = (BinExpr) expr;
			bexpr.oprand1().accept(this);
			int type1 = this.exprType;
			int dim1 = this.arrayDim;
			bexpr.oprand2().accept(this);
			if (dim1 == 0 && this.arrayDim == 0)
				this.insertCast(bexpr, type1, this.exprType);
		} else if (op == '!')
			((Expr) expr).oprand1().accept(this);
		else if (op == TokenId.ANDAND || op == TokenId.OROR)
		{
			BinExpr bexpr = (BinExpr) expr;
			bexpr.oprand1().accept(this);
			bexpr.oprand2().accept(this);
		} else
			// others
			expr.accept(this);

		this.exprType = TokenId.BOOLEAN;
		this.arrayDim = 0;
	}

	private void computeBinExprType(BinExpr expr, int token, int type1) throws CompileError
	{
		// arrayDim should be 0.
		int type2 = this.exprType;
		if (token == TokenId.LSHIFT || token == TokenId.RSHIFT || token == TokenId.ARSHIFT)
			this.exprType = type1;
		else
			this.insertCast(expr, type1, type2);

		if (CodeGen.isP_INT(this.exprType))
			this.exprType = TokenId.INT; // type1 may be BYTE, ...
	}

	/*
	 * if EXPR is to access a static field, fieldAccess() translates EXPR into
	 * an expression using '#' (MEMBER). For example, it translates
	 * java.lang.Integer.TYPE into java.lang.Integer#TYPE. This translation
	 * speeds up type resolution by MemberCodeGen.
	 */
	protected CtField fieldAccess(ASTree expr) throws CompileError
	{
		if (expr instanceof Member)
		{
			Member mem = (Member) expr;
			String name = mem.get();
			try
			{
				CtField f = this.thisClass.getField(name);
				if (Modifier.isStatic(f.getModifiers()))
					mem.setField(f);

				return f;
			} catch (NotFoundException e)
			{
				// EXPR might be part of a static member access?
				throw new NoFieldException(name, expr);
			}
		} else if (expr instanceof Expr)
		{
			Expr e = (Expr) expr;
			int op = e.getOperator();
			if (op == TokenId.MEMBER)
			{
				Member mem = (Member) e.oprand2();
				CtField f = this.resolver.lookupField(((Symbol) e.oprand1()).get(), mem);
				mem.setField(f);
				return f;
			} else if (op == '.')
			{
				try
				{
					e.oprand1().accept(this);
				} catch (NoFieldException nfe)
				{
					if (nfe.getExpr() != e.oprand1())
						throw nfe;

					/*
					 * EXPR should be a static field. If EXPR might be part of a
					 * qualified class name, lookupFieldByJvmName2() throws
					 * NoFieldException.
					 */
					return this.fieldAccess2(e, nfe.getField());
				}

				CompileError err = null;
				try
				{
					if (this.exprType == TokenId.CLASS && this.arrayDim == 0)
						return this.resolver.lookupFieldByJvmName(this.className, (Symbol) e.oprand2());
				} catch (CompileError ce)
				{
					err = ce;
				}

				/*
				 * If a filed name is the same name as a package's, a static
				 * member of a class in that package is not visible. For
				 * example,
				 * 
				 * class Foo { int javassist; }
				 * 
				 * It is impossible to add the following method:
				 * 
				 * String m() { return javassist.CtClass.intType.toString(); }
				 * 
				 * because javassist is a field name. However, this is often
				 * inconvenient, this compiler allows it. The following code is
				 * for that.
				 */
				ASTree oprnd1 = e.oprand1();
				if (oprnd1 instanceof Symbol)
					return this.fieldAccess2(e, ((Symbol) oprnd1).get());

				if (err != null)
					throw err;
			}
		}

		throw new CompileError("bad filed access");
	}

	private CtField fieldAccess2(Expr e, String jvmClassName) throws CompileError
	{
		Member fname = (Member) e.oprand2();
		CtField f = this.resolver.lookupFieldByJvmName2(jvmClassName, fname, e);
		e.setOperator(TokenId.MEMBER);
		e.setOprand1(new Symbol(MemberResolver.jvmToJavaName(jvmClassName)));
		fname.setField(f);
		return f;
	}

	public int getMethodArgsLength(ASTList args)
	{
		return ASTList.length(args);
	}

	/**
	 * Returns the JVM-internal representation of this super class name.
	 */
	protected String getSuperName() throws CompileError
	{
		return MemberResolver.javaToJvmName(MemberResolver.getSuperclass(this.thisClass).getName());
	}

	/**
	 * Returns the JVM-internal representation of this class name.
	 */
	protected String getThisName()
	{
		return MemberResolver.javaToJvmName(this.thisClass.getName());
	}

	private void insertCast(BinExpr expr, int type1, int type2) throws CompileError
	{
		if (CodeGen.rightIsStrong(type1, type2))
			expr.setLeft(new CastExpr(type2, 0, expr.oprand1()));
		else
			this.exprType = type1;
	}

	private boolean isConstant(BinExpr expr, int op, ASTree left, ASTree right) throws CompileError
	{
		left = TypeChecker.stripPlusExpr(left);
		right = TypeChecker.stripPlusExpr(right);
		ASTree newExpr = null;
		if (left instanceof StringL && right instanceof StringL && op == '+')
			newExpr = new StringL(((StringL) left).get() + ((StringL) right).get());
		else if (left instanceof IntConst)
			newExpr = ((IntConst) left).compute(op, right);
		else if (left instanceof DoubleConst)
			newExpr = ((DoubleConst) left).compute(op, right);

		if (newExpr == null)
			return false; // not a constant expression
		else
		{
			expr.setOperator('+');
			expr.setOprand1(newExpr);
			expr.setOprand2(null);
			newExpr.accept(this); // for setting exprType, arrayDim, ...
			return true;
		}
	}

	private boolean isConstant(Expr expr, int op, ASTree oprand)
	{
		oprand = TypeChecker.stripPlusExpr(oprand);
		if (oprand instanceof IntConst)
		{
			IntConst c = (IntConst) oprand;
			long v = c.get();
			if (op == '-')
				v = -v;
			else if (op == '~')
				v = ~v;
			else
				return false;

			c.set(v);
		} else if (oprand instanceof DoubleConst)
		{
			DoubleConst c = (DoubleConst) oprand;
			if (op == '-')
				c.set(-c.get());
			else
				return false;
		} else
			return false;

		expr.setOperator('+');
		return true;
	}

	/*
	 * Converts a class name into a JVM-internal representation.
	 * 
	 * It may also expand a simple class name to java.lang.*. For example, this
	 * converts Object into java/lang/Object.
	 */
	protected String resolveClassName(ASTList name) throws CompileError
	{
		return this.resolver.resolveClassName(name);
	}

	/*
	 * Expands a simple class name to java.lang.*. For example, this converts
	 * Object into java/lang/Object.
	 */
	protected String resolveClassName(String jvmName) throws CompileError
	{
		return this.resolver.resolveJvmClassName(jvmName);
	}

	void setReturnType(String desc) throws CompileError
	{
		int i = desc.indexOf(')');
		if (i < 0)
			TypeChecker.badMethod();

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
				TypeChecker.badMethod();

			this.exprType = TokenId.CLASS;
			this.className = desc.substring(i + 1, j);
		} else
		{
			this.exprType = MemberResolver.descToType(c);
			this.className = null;
		}
	}

	/**
	 * Records the currently compiled method.
	 */
	public void setThisMethod(MethodInfo m)
	{
		this.thisMethod = m;
	}
}
