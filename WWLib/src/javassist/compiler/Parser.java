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

public final class Parser implements TokenId
{
	// !"#$%&'( )*+,-./0 12345678 9:;<=>?
	private static final int[]	binaryOpPrecedence	=
													{ 0, 0, 0, 0, 1, 6, 0, 0, 0, 1, 2, 0, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 4, 0 };

	private static boolean isAssignOp(int t)
	{
		return t == '=' || t == TokenId.MOD_E || t == TokenId.AND_E || t == TokenId.MUL_E || t == TokenId.PLUS_E || t == TokenId.MINUS_E || t == TokenId.DIV_E || t == TokenId.EXOR_E
				|| t == TokenId.OR_E || t == TokenId.LSHIFT_E || t == TokenId.RSHIFT_E || t == TokenId.ARSHIFT_E;
	}

	private static boolean isBuiltinType(int t)
	{
		return t == TokenId.BOOLEAN || t == TokenId.BYTE || t == TokenId.CHAR || t == TokenId.SHORT || t == TokenId.INT || t == TokenId.LONG || t == TokenId.FLOAT || t == TokenId.DOUBLE;
	}

	private Lex	lex;

	public Parser(Lex lex)
	{
		this.lex = lex;
	}

	private ASTree binaryExpr2(SymbolTable tbl, ASTree expr, int prec) throws CompileError
	{
		int t = this.lex.get();
		if (t == TokenId.INSTANCEOF)
			return this.parseInstanceOf(tbl, expr);

		ASTree expr2 = this.parseUnaryExpr(tbl);
		for (;;)
		{
			int t2 = this.lex.lookAhead();
			int p2 = this.getOpPrecedence(t2);
			if (p2 != 0 && prec > p2)
				expr2 = this.binaryExpr2(tbl, expr2, p2);
			else
				return BinExpr.makeBin(t, expr, expr2);
		}
	}

	private int getOpPrecedence(int c)
	{
		if ('!' <= c && c <= '?')
			return Parser.binaryOpPrecedence[c - '!'];
		else if (c == '^')
			return 7;
		else if (c == '|')
			return 8;
		else if (c == TokenId.ANDAND)
			return 9;
		else if (c == TokenId.OROR)
			return 10;
		else if (c == TokenId.EQ || c == TokenId.NEQ)
			return 5;
		else if (c == TokenId.LE || c == TokenId.GE || c == TokenId.INSTANCEOF)
			return 4;
		else if (c == TokenId.LSHIFT || c == TokenId.RSHIFT || c == TokenId.ARSHIFT)
			return 3;
		else
			return 0; // not a binary operator
	}

	public boolean hasMore()
	{
		return this.lex.lookAhead() >= 0;
	}

	private boolean nextIsBuiltinCast()
	{
		int t;
		int i = 2;
		while ((t = this.lex.lookAhead(i++)) == '[')
			if (this.lex.lookAhead(i++) != ']')
				return false;

		return this.lex.lookAhead(i - 1) == ')';
	}

	private boolean nextIsClassCast()
	{
		int i = this.nextIsClassType(1);
		if (i < 0)
			return false;

		int t = this.lex.lookAhead(i);
		if (t != ')')
			return false;

		t = this.lex.lookAhead(i + 1);
		return t == '(' || t == TokenId.NULL || t == TokenId.StringL || t == TokenId.Identifier || t == TokenId.THIS || t == TokenId.SUPER || t == TokenId.NEW || t == TokenId.TRUE
				|| t == TokenId.FALSE || t == TokenId.LongConstant || t == TokenId.IntConstant || t == TokenId.CharConstant || t == TokenId.DoubleConstant || t == TokenId.FloatConstant;
	}

	private int nextIsClassType(int i)
	{
		int t;
		while (this.lex.lookAhead(++i) == '.')
			if (this.lex.lookAhead(++i) != TokenId.Identifier)
				return -1;

		while ((t = this.lex.lookAhead(i++)) == '[')
			if (this.lex.lookAhead(i++) != ']')
				return -1;

		return i - 1;
	}

	/*
	 * argument.list : "(" [ expression [ "," expression ]* ] ")"
	 */
	private ASTList parseArgumentList(SymbolTable tbl) throws CompileError
	{
		if (this.lex.get() != '(')
			throw new CompileError("( is missing", this.lex);

		ASTList list = null;
		if (this.lex.lookAhead() != ')')
			for (;;)
			{
				list = ASTList.append(list, this.parseExpression(tbl));
				if (this.lex.lookAhead() == ',')
					this.lex.get();
				else
					break;
			}

		if (this.lex.get() != ')')
			throw new CompileError(") is missing", this.lex);

		return list;
	}

	/*
	 * array.dimension : [ "[" "]" ]*
	 */
	private int parseArrayDimension() throws CompileError
	{
		int arrayDim = 0;
		while (this.lex.lookAhead() == '[')
		{
			++arrayDim;
			this.lex.get();
			if (this.lex.get() != ']')
				throw new CompileError("] is missing", this.lex);
		}

		return arrayDim;
	}

	/*
	 * array.index : "[" [ expression ] "]"
	 */
	private ASTree parseArrayIndex(SymbolTable tbl) throws CompileError
	{
		this.lex.get(); // '['
		if (this.lex.lookAhead() == ']')
		{
			this.lex.get();
			return null;
		} else
		{
			ASTree index = this.parseExpression(tbl);
			if (this.lex.get() != ']')
				throw new CompileError("] is missing", this.lex);

			return index;
		}
	}

	/*
	 * array.initializer : '{' (( array.initializer | expression ) ',')* '}'
	 */
	private ArrayInit parseArrayInitializer(SymbolTable tbl) throws CompileError
	{
		this.lex.get(); // '{'
		ASTree expr = this.parseExpression(tbl);
		ArrayInit init = new ArrayInit(expr);
		while (this.lex.lookAhead() == ',')
		{
			this.lex.get();
			expr = this.parseExpression(tbl);
			ASTList.append(init, expr);
		}

		if (this.lex.get() != '}')
			throw new SyntaxError(this.lex);

		return init;
	}

	/*
	 * array.size : [ array.index ]*
	 */
	private ASTList parseArraySize(SymbolTable tbl) throws CompileError
	{
		ASTList list = null;
		while (this.lex.lookAhead() == '[')
			list = ASTList.append(list, this.parseArrayIndex(tbl));

		return list;
	}

	/*
	 * logical.or.expr 10 (operator precedence) : logical.and.expr |
	 * logical.or.expr OROR logical.and.expr left-to-right logical.and.expr 9 :
	 * inclusive.or.expr | logical.and.expr ANDAND inclusive.or.expr
	 * inclusive.or.expr 8 : exclusive.or.expr | inclusive.or.expr "|"
	 * exclusive.or.expr exclusive.or.expr 7 : and.expr | exclusive.or.expr "^"
	 * and.expr and.expr 6 : equality.expr | and.expr "&" equality.expr
	 * equality.expr 5 : relational.expr | equality.expr (EQ | NEQ)
	 * relational.expr relational.expr 4 : shift.expr | relational.expr (LE | GE
	 * | "<" | ">") shift.expr | relational.expr INSTANCEOF class.type ("["
	 * "]")* shift.expr 3 : additive.expr | shift.expr (LSHIFT | RSHIFT |
	 * ARSHIFT) additive.expr additive.expr 2 : multiply.expr | additive.expr
	 * ("+" | "-") multiply.expr multiply.expr 1 : unary.expr | multiply.expr
	 * ("*" | "/" | "%") unary.expr
	 */
	private ASTree parseBinaryExpr(SymbolTable tbl) throws CompileError
	{
		ASTree expr = this.parseUnaryExpr(tbl);
		for (;;)
		{
			int t = this.lex.lookAhead();
			int p = this.getOpPrecedence(t);
			if (p == 0)
				return expr;
			else
				expr = this.binaryExpr2(tbl, expr, p);
		}
	}

	/*
	 * block.statement : "{" statement* "}"
	 */
	private Stmnt parseBlock(SymbolTable tbl) throws CompileError
	{
		if (this.lex.get() != '{')
			throw new SyntaxError(this.lex);

		Stmnt body = null;
		SymbolTable tbl2 = new SymbolTable(tbl);
		while (this.lex.lookAhead() != '}')
		{
			Stmnt s = this.parseStatement(tbl2);
			if (s != null)
				body = (Stmnt) ASTList.concat(body, new Stmnt(TokenId.BLOCK, s));
		}

		this.lex.get(); // '}'
		if (body == null)
			return new Stmnt(TokenId.BLOCK); // empty block
		else
			return body;
	}

	/*
	 * break.statement : BREAK [ Identifier ] ";"
	 */
	private Stmnt parseBreak(SymbolTable tbl) throws CompileError
	{
		return this.parseContinue(tbl);
	}

	/*
	 * cast.expr : "(" builtin.type ("[" "]")* ")" unary.expr | "(" class.type
	 * ("[" "]")* ")" unary.expr2 unary.expr2 is a unary.expr beginning with
	 * "(", NULL, StringL, Identifier, THIS, SUPER, or NEW. Either "(int.class)"
	 * or "(String[].class)" is a not cast expression.
	 */
	private ASTree parseCast(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.lookAhead(1);
		if (Parser.isBuiltinType(t) && this.nextIsBuiltinCast())
		{
			this.lex.get(); // '('
			this.lex.get(); // primitive type
			int dim = this.parseArrayDimension();
			if (this.lex.get() != ')')
				throw new CompileError(") is missing", this.lex);

			return new CastExpr(t, dim, this.parseUnaryExpr(tbl));
		} else if (t == TokenId.Identifier && this.nextIsClassCast())
		{
			this.lex.get(); // '('
			ASTList name = this.parseClassType(tbl);
			int dim = this.parseArrayDimension();
			if (this.lex.get() != ')')
				throw new CompileError(") is missing", this.lex);

			return new CastExpr(name, dim, this.parseUnaryExpr(tbl));
		} else
			return this.parsePostfix(tbl);
	}

	/*
	 * class.type : Identifier ( "." Identifier )*
	 */
	private ASTList parseClassType(SymbolTable tbl) throws CompileError
	{
		ASTList list = null;
		for (;;)
		{
			if (this.lex.get() != TokenId.Identifier)
				throw new SyntaxError(this.lex);

			list = ASTList.append(list, new Symbol(this.lex.getString()));
			if (this.lex.lookAhead() == '.')
				this.lex.get();
			else
				break;
		}

		return list;
	}

	/*
	 * conditional.expr (right-to-left) : logical.or.expr [ '?' expression ':'
	 * conditional.expr ]
	 */
	private ASTree parseConditionalExpr(SymbolTable tbl) throws CompileError
	{
		ASTree cond = this.parseBinaryExpr(tbl);
		if (this.lex.lookAhead() == '?')
		{
			this.lex.get();
			ASTree thenExpr = this.parseExpression(tbl);
			if (this.lex.get() != ':')
				throw new CompileError(": is missing", this.lex);

			ASTree elseExpr = this.parseExpression(tbl);
			return new CondExpr(cond, thenExpr, elseExpr);
		} else
			return cond;
	}

	/*
	 * continue.statement : CONTINUE [ Identifier ] ";"
	 */
	private Stmnt parseContinue(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // CONTINUE
		Stmnt s = new Stmnt(t);
		int t2 = this.lex.get();
		if (t2 == TokenId.Identifier)
		{
			s.setLeft(new Symbol(this.lex.getString()));
			t2 = this.lex.get();
		}

		if (t2 != ';')
			throw new CompileError("; is missing", this.lex);

		return s;
	}

	/*
	 * declaration.or.expression : [ FINAL ] built-in-type array.dimension
	 * declarators | [ FINAL ] class.type array.dimension declarators |
	 * expression ';' | expr.list ';' if exprList is true Note: FINAL is
	 * currently ignored. This must be fixed in future.
	 */
	private Stmnt parseDeclarationOrExpression(SymbolTable tbl, boolean exprList) throws CompileError
	{
		int t = this.lex.lookAhead();
		while (t == TokenId.FINAL)
		{
			this.lex.get();
			t = this.lex.lookAhead();
		}

		if (Parser.isBuiltinType(t))
		{
			t = this.lex.get();
			int dim = this.parseArrayDimension();
			return this.parseDeclarators(tbl, new Declarator(t, dim));
		} else if (t == TokenId.Identifier)
		{
			int i = this.nextIsClassType(0);
			if (i >= 0)
				if (this.lex.lookAhead(i) == TokenId.Identifier)
				{
					ASTList name = this.parseClassType(tbl);
					int dim = this.parseArrayDimension();
					return this.parseDeclarators(tbl, new Declarator(name, dim));
				}
		}

		Stmnt expr;
		if (exprList)
			expr = this.parseExprList(tbl);
		else
			expr = new Stmnt(TokenId.EXPR, this.parseExpression(tbl));

		if (this.lex.get() != ';')
			throw new CompileError("; is missing", this.lex);

		return expr;
	}

	/*
	 * declarator : Identifier array.dimension [ '=' initializer ]
	 */
	private Declarator parseDeclarator(SymbolTable tbl, Declarator d) throws CompileError
	{
		if (this.lex.get() != TokenId.Identifier || d.getType() == TokenId.VOID)
			throw new SyntaxError(this.lex);

		String name = this.lex.getString();
		Symbol symbol = new Symbol(name);
		int dim = this.parseArrayDimension();
		ASTree init = null;
		if (this.lex.lookAhead() == '=')
		{
			this.lex.get();
			init = this.parseInitializer(tbl);
		}

		Declarator decl = d.make(symbol, dim, init);
		tbl.append(name, decl);
		return decl;
	}

	/*
	 * declarators : declarator [ ',' declarator ]* ';'
	 */
	private Stmnt parseDeclarators(SymbolTable tbl, Declarator d) throws CompileError
	{
		Stmnt decl = null;
		for (;;)
		{
			decl = (Stmnt) ASTList.concat(decl, new Stmnt(TokenId.DECL, this.parseDeclarator(tbl, d)));
			int t = this.lex.get();
			if (t == ';')
				return decl;
			else if (t != ',')
				throw new CompileError("; is missing", this.lex);
		}
	}

	/*
	 * do.statement : DO statement WHILE "(" expression ")" ";"
	 */
	private Stmnt parseDo(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // DO
		Stmnt body = this.parseStatement(tbl);
		if (this.lex.get() != TokenId.WHILE || this.lex.get() != '(')
			throw new SyntaxError(this.lex);

		ASTree expr = this.parseExpression(tbl);
		if (this.lex.get() != ')' || this.lex.get() != ';')
			throw new SyntaxError(this.lex);

		return new Stmnt(t, expr, body);
	}

	/*
	 * Parse a .class expression on a class type. For example, String.class =>
	 * ('.' "String" "class") String[].class => ('.' "[LString;" "class")
	 */
	private ASTree parseDotClass(ASTree className, int dim) throws CompileError
	{
		String cname = this.toClassName(className);
		if (dim > 0)
		{
			StringBuffer sbuf = new StringBuffer();
			while (dim-- > 0)
				sbuf.append('[');

			sbuf.append('L').append(cname.replace('.', '/')).append(';');
			cname = sbuf.toString();
		}

		return Expr.make('.', new Symbol(cname), new Member("class"));
	}

	/*
	 * Parses a .class expression on a built-in type. For example, int.class =>
	 * ('#' "java.lang.Integer" "TYPE") int[].class => ('.' "[I", "class")
	 */
	private ASTree parseDotClass(int builtinType, int dim) throws CompileError
	{
		if (dim > 0)
		{
			String cname = CodeGen.toJvmTypeName(builtinType, dim);
			return Expr.make('.', new Symbol(cname), new Member("class"));
		} else
		{
			String cname;
			switch (builtinType)
			{
				case BOOLEAN:
					cname = "java.lang.Boolean";
					break;
				case BYTE:
					cname = "java.lang.Byte";
					break;
				case CHAR:
					cname = "java.lang.Character";
					break;
				case SHORT:
					cname = "java.lang.Short";
					break;
				case INT:
					cname = "java.lang.Integer";
					break;
				case LONG:
					cname = "java.lang.Long";
					break;
				case FLOAT:
					cname = "java.lang.Float";
					break;
				case DOUBLE:
					cname = "java.lang.Double";
					break;
				case VOID:
					cname = "java.lang.Void";
					break;
				default:
					throw new CompileError("invalid builtin type: " + builtinType);
			}

			return Expr.make(TokenId.MEMBER, new Symbol(cname), new Member("TYPE"));
		}
	}

	/*
	 * expression : conditional.expr | conditional.expr assign.op expression
	 * (right-to-left)
	 */
	public ASTree parseExpression(SymbolTable tbl) throws CompileError
	{
		ASTree left = this.parseConditionalExpr(tbl);
		if (!Parser.isAssignOp(this.lex.lookAhead()))
			return left;

		int t = this.lex.get();
		ASTree right = this.parseExpression(tbl);
		return AssignExpr.makeAssign(t, left, right);
	}

	/*
	 * expr.list : ( expression ',')* expression
	 */
	private Stmnt parseExprList(SymbolTable tbl) throws CompileError
	{
		Stmnt expr = null;
		for (;;)
		{
			Stmnt e = new Stmnt(TokenId.EXPR, this.parseExpression(tbl));
			expr = (Stmnt) ASTList.concat(expr, new Stmnt(TokenId.BLOCK, e));
			if (this.lex.lookAhead() == ',')
				this.lex.get();
			else
				return expr;
		}
	}

	/*
	 * field.declaration : member.modifiers formal.type Identifier [ "="
	 * expression ] ";"
	 */
	private FieldDecl parseField(SymbolTable tbl, ASTList mods, Declarator d) throws CompileError
	{
		ASTree expr = null;
		if (this.lex.lookAhead() == '=')
		{
			this.lex.get();
			expr = this.parseExpression(tbl);
		}

		int c = this.lex.get();
		if (c == ';')
			return new FieldDecl(mods, new ASTList(d, new ASTList(expr)));
		else if (c == ',')
			throw new CompileError("only one field can be declared in one declaration", this.lex);
		else
			throw new SyntaxError(this.lex);
	}

	/*
	 * for.statement : FOR "(" decl.or.expr expression ";" expression ")"
	 * statement
	 */
	private Stmnt parseFor(SymbolTable tbl) throws CompileError
	{
		Stmnt expr1, expr3;
		ASTree expr2;
		int t = this.lex.get(); // FOR

		SymbolTable tbl2 = new SymbolTable(tbl);

		if (this.lex.get() != '(')
			throw new SyntaxError(this.lex);

		if (this.lex.lookAhead() == ';')
		{
			this.lex.get();
			expr1 = null;
		} else
			expr1 = this.parseDeclarationOrExpression(tbl2, true);

		if (this.lex.lookAhead() == ';')
			expr2 = null;
		else
			expr2 = this.parseExpression(tbl2);

		if (this.lex.get() != ';')
			throw new CompileError("; is missing", this.lex);

		if (this.lex.lookAhead() == ')')
			expr3 = null;
		else
			expr3 = this.parseExprList(tbl2);

		if (this.lex.get() != ')')
			throw new CompileError(") is missing", this.lex);

		Stmnt body = this.parseStatement(tbl2);
		return new Stmnt(t, expr1, new ASTList(expr2, new ASTList(expr3, body)));
	}

	/*
	 * formal.parameter : formal.type Identifier array.dimension
	 */
	private Declarator parseFormalParam(SymbolTable tbl) throws CompileError
	{
		Declarator d = this.parseFormalType(tbl);
		if (this.lex.get() != TokenId.Identifier)
			throw new SyntaxError(this.lex);

		String name = this.lex.getString();
		d.setVariable(new Symbol(name));
		d.addArrayDim(this.parseArrayDimension());
		tbl.append(name, d);
		return d;
	}

	/*
	 * formal.type : ( build-in-type | class.type ) array.dimension
	 */
	private Declarator parseFormalType(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.lookAhead();
		if (Parser.isBuiltinType(t) || t == TokenId.VOID)
		{
			this.lex.get(); // primitive type
			int dim = this.parseArrayDimension();
			return new Declarator(t, dim);
		} else
		{
			ASTList name = this.parseClassType(tbl);
			int dim = this.parseArrayDimension();
			return new Declarator(name, dim);
		}
	}

	/*
	 * if.statement : IF "(" expression ")" statement [ ELSE statement ]
	 */
	private Stmnt parseIf(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // IF
		ASTree expr = this.parseParExpression(tbl);
		Stmnt thenp = this.parseStatement(tbl);
		Stmnt elsep;
		if (this.lex.lookAhead() == TokenId.ELSE)
		{
			this.lex.get();
			elsep = this.parseStatement(tbl);
		} else
			elsep = null;

		return new Stmnt(t, expr, new ASTList(thenp, new ASTList(elsep)));
	}

	/*
	 * initializer : expression | array.initializer
	 */
	private ASTree parseInitializer(SymbolTable tbl) throws CompileError
	{
		if (this.lex.lookAhead() == '{')
			return this.parseArrayInitializer(tbl);
		else
			return this.parseExpression(tbl);
	}

	private ASTree parseInstanceOf(SymbolTable tbl, ASTree expr) throws CompileError
	{
		int t = this.lex.lookAhead();
		if (Parser.isBuiltinType(t))
		{
			this.lex.get(); // primitive type
			int dim = this.parseArrayDimension();
			return new InstanceOfExpr(t, dim, expr);
		} else
		{
			ASTList name = this.parseClassType(tbl);
			int dim = this.parseArrayDimension();
			return new InstanceOfExpr(name, dim, expr);
		}
	}

	/*
	 * member.declaration : method.declaration | field.declaration
	 */
	public ASTList parseMember(SymbolTable tbl) throws CompileError
	{
		ASTList mem = this.parseMember1(tbl);
		if (mem instanceof MethodDecl)
			return this.parseMethod2(tbl, (MethodDecl) mem);
		else
			return mem;
	}

	/*
	 * A method body is not parsed.
	 */
	public ASTList parseMember1(SymbolTable tbl) throws CompileError
	{
		ASTList mods = this.parseMemberMods();
		Declarator d;
		boolean isConstructor = false;
		if (this.lex.lookAhead() == TokenId.Identifier && this.lex.lookAhead(1) == '(')
		{
			d = new Declarator(TokenId.VOID, 0);
			isConstructor = true;
		} else
			d = this.parseFormalType(tbl);

		if (this.lex.get() != TokenId.Identifier)
			throw new SyntaxError(this.lex);

		String name;
		if (isConstructor)
			name = MethodDecl.initName;
		else
			name = this.lex.getString();

		d.setVariable(new Symbol(name));
		if (isConstructor || this.lex.lookAhead() == '(')
			return this.parseMethod1(tbl, isConstructor, mods, d);
		else
			return this.parseField(tbl, mods, d);
	}

	/*
	 * member.modifiers : ( FINAL | SYNCHRONIZED | ABSTRACT | PUBLIC | PROTECTED
	 * | PRIVATE | STATIC | VOLATILE | TRANSIENT | STRICT )*
	 */
	private ASTList parseMemberMods()
	{
		int t;
		ASTList list = null;
		while (true)
		{
			t = this.lex.lookAhead();
			if (t == TokenId.ABSTRACT || t == TokenId.FINAL || t == TokenId.PUBLIC || t == TokenId.PROTECTED || t == TokenId.PRIVATE || t == TokenId.SYNCHRONIZED || t == TokenId.STATIC
					|| t == TokenId.VOLATILE || t == TokenId.TRANSIENT || t == TokenId.STRICT)
				list = new ASTList(new Keyword(this.lex.get()), list);
			else
				break;
		}

		return list;
	}

	/*
	 * method.declaration : member.modifiers [ formal.type ] Identifier "(" [
	 * formal.parameter ( "," formal.parameter )* ] ")" array.dimension [ THROWS
	 * class.type ( "," class.type ) ] ( block.statement | ";" ) Note that a
	 * method body is not parsed.
	 */
	private MethodDecl parseMethod1(SymbolTable tbl, boolean isConstructor, ASTList mods, Declarator d) throws CompileError
	{
		if (this.lex.get() != '(')
			throw new SyntaxError(this.lex);

		ASTList parms = null;
		if (this.lex.lookAhead() != ')')
			while (true)
			{
				parms = ASTList.append(parms, this.parseFormalParam(tbl));
				int t = this.lex.lookAhead();
				if (t == ',')
					this.lex.get();
				else if (t == ')')
					break;
			}

		this.lex.get(); // ')'
		d.addArrayDim(this.parseArrayDimension());
		if (isConstructor && d.getArrayDim() > 0)
			throw new SyntaxError(this.lex);

		ASTList throwsList = null;
		if (this.lex.lookAhead() == TokenId.THROWS)
		{
			this.lex.get();
			while (true)
			{
				throwsList = ASTList.append(throwsList, this.parseClassType(tbl));
				if (this.lex.lookAhead() == ',')
					this.lex.get();
				else
					break;
			}
		}

		return new MethodDecl(mods, new ASTList(d, ASTList.make(parms, throwsList, null)));
	}

	/*
	 * Parses a method body.
	 */
	public MethodDecl parseMethod2(SymbolTable tbl, MethodDecl md) throws CompileError
	{
		Stmnt body = null;
		if (this.lex.lookAhead() == ';')
			this.lex.get();
		else
		{
			body = this.parseBlock(tbl);
			if (body == null)
				body = new Stmnt(TokenId.BLOCK);
		}

		md.sublist(4).setHead(body);
		return md;
	}

	/*
	 * method.call : method.expr "(" argument.list ")" method.expr : THIS |
	 * SUPER | Identifier | postfix.expr "." Identifier | postfix.expr "#"
	 * Identifier
	 */
	private ASTree parseMethodCall(SymbolTable tbl, ASTree expr) throws CompileError
	{
		if (expr instanceof Keyword)
		{
			int token = ((Keyword) expr).get();
			if (token != TokenId.THIS && token != TokenId.SUPER)
				throw new SyntaxError(this.lex);
		} else if (expr instanceof Symbol) // Identifier
			;
		else if (expr instanceof Expr)
		{
			int op = ((Expr) expr).getOperator();
			if (op != '.' && op != TokenId.MEMBER)
				throw new SyntaxError(this.lex);
		}

		return CallExpr.makeCall(expr, this.parseArgumentList(tbl));
	}

	/*
	 * new.expr : class.type "(" argument.list ")" | class.type array.size [
	 * array.initializer ] | primitive.type array.size [ array.initializer ]
	 */
	private NewExpr parseNew(SymbolTable tbl) throws CompileError
	{
		ArrayInit init = null;
		int t = this.lex.lookAhead();
		if (Parser.isBuiltinType(t))
		{
			this.lex.get();
			ASTList size = this.parseArraySize(tbl);
			if (this.lex.lookAhead() == '{')
				init = this.parseArrayInitializer(tbl);

			return new NewExpr(t, size, init);
		} else if (t == TokenId.Identifier)
		{
			ASTList name = this.parseClassType(tbl);
			t = this.lex.lookAhead();
			if (t == '(')
			{
				ASTList args = this.parseArgumentList(tbl);
				return new NewExpr(name, args);
			} else if (t == '[')
			{
				ASTList size = this.parseArraySize(tbl);
				if (this.lex.lookAhead() == '{')
					init = this.parseArrayInitializer(tbl);

				return NewExpr.makeObjectArray(name, size, init);
			}
		}

		throw new SyntaxError(this.lex);
	}

	/*
	 * par.expression : '(' expression ')'
	 */
	private ASTree parseParExpression(SymbolTable tbl) throws CompileError
	{
		if (this.lex.get() != '(')
			throw new SyntaxError(this.lex);

		ASTree expr = this.parseExpression(tbl);
		if (this.lex.get() != ')')
			throw new SyntaxError(this.lex);

		return expr;
	}

	/*
	 * postfix.expr : number.literal | primary.expr | method.expr | postfix.expr
	 * "++" | "--" | postfix.expr "[" array.size "]" | postfix.expr "."
	 * Identifier | postfix.expr ( "[" "]" )* "." CLASS | postfix.expr "#"
	 * Identifier | postfix.expr "." SUPER "#" is not an operator of regular
	 * Java. It separates a class name and a member name in an expression for
	 * static member access. For example, java.lang.Integer.toString(3) in
	 * regular Java can be written like this: java.lang.Integer#toString(3) for
	 * this compiler.
	 */
	private ASTree parsePostfix(SymbolTable tbl) throws CompileError
	{
		int token = this.lex.lookAhead();
		switch (token)
		{ // see also parseUnaryExpr()
			case LongConstant:
			case IntConstant:
			case CharConstant:
				this.lex.get();
				return new IntConst(this.lex.getLong(), token);
			case DoubleConstant:
			case FloatConstant:
				this.lex.get();
				return new DoubleConst(this.lex.getDouble(), token);
			default:
				break;
		}

		String str;
		ASTree index;
		ASTree expr = this.parsePrimaryExpr(tbl);
		int t;
		while (true)
			switch (this.lex.lookAhead())
			{
				case '(':
					expr = this.parseMethodCall(tbl, expr);
					break;
				case '[':
					if (this.lex.lookAhead(1) == ']')
					{
						int dim = this.parseArrayDimension();
						if (this.lex.get() != '.' || this.lex.get() != TokenId.CLASS)
							throw new SyntaxError(this.lex);

						expr = this.parseDotClass(expr, dim);
					} else
					{
						index = this.parseArrayIndex(tbl);
						if (index == null)
							throw new SyntaxError(this.lex);

						expr = Expr.make(TokenId.ARRAY, expr, index);
					}
					break;
				case PLUSPLUS:
				case MINUSMINUS:
					t = this.lex.get();
					expr = Expr.make(t, null, expr);
					break;
				case '.':
					this.lex.get();
					t = this.lex.get();
					if (t == TokenId.CLASS)
						expr = this.parseDotClass(expr, 0);
					else if (t == TokenId.SUPER)
						expr = Expr.make('.', new Symbol(this.toClassName(expr)), new Keyword(t));
					else if (t == TokenId.Identifier)
					{
						str = this.lex.getString();
						expr = Expr.make('.', expr, new Member(str));
					} else
						throw new CompileError("missing member name", this.lex);
					break;
				case '#':
					this.lex.get();
					t = this.lex.get();
					if (t != TokenId.Identifier)
						throw new CompileError("missing static member name", this.lex);

					str = this.lex.getString();
					expr = Expr.make(TokenId.MEMBER, new Symbol(this.toClassName(expr)), new Member(str));
					break;
				default:
					return expr;
			}
	}

	/*
	 * primary.expr : THIS | SUPER | TRUE | FALSE | NULL | StringL | Identifier
	 * | NEW new.expr | "(" expression ")" | builtin.type ( "[" "]" )* "." CLASS
	 * Identifier represents either a local variable name, a member name, or a
	 * class name.
	 */
	private ASTree parsePrimaryExpr(SymbolTable tbl) throws CompileError
	{
		int t;
		String name;
		Declarator decl;
		ASTree expr;

		switch (t = this.lex.get())
		{
			case THIS:
			case SUPER:
			case TRUE:
			case FALSE:
			case NULL:
				return new Keyword(t);
			case Identifier:
				name = this.lex.getString();
				decl = tbl.lookup(name);
				if (decl == null)
					return new Member(name); // this or static member
				else
					return new Variable(name, decl); // local variable
			case StringL:
				return new StringL(this.lex.getString());
			case NEW:
				return this.parseNew(tbl);
			case '(':
				expr = this.parseExpression(tbl);
				if (this.lex.get() == ')')
					return expr;
				else
					throw new CompileError(") is missing", this.lex);
			default:
				if (Parser.isBuiltinType(t) || t == TokenId.VOID)
				{
					int dim = this.parseArrayDimension();
					if (this.lex.get() == '.' && this.lex.get() == TokenId.CLASS)
						return this.parseDotClass(t, dim);
				}

				throw new SyntaxError(this.lex);
		}
	}

	/*
	 * return.statement : RETURN [ expression ] ";"
	 */
	private Stmnt parseReturn(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // RETURN
		Stmnt s = new Stmnt(t);
		if (this.lex.lookAhead() != ';')
			s.setLeft(this.parseExpression(tbl));

		if (this.lex.get() != ';')
			throw new CompileError("; is missing", this.lex);

		return s;
	}

	/*
	 * statement : [ label ":" ]* labeled.statement labeled.statement :
	 * block.statement | if.statement | while.statement | do.statement |
	 * for.statement | switch.statement | try.statement | return.statement |
	 * thorw.statement | break.statement | continue.statement |
	 * declaration.or.expression | ";" This method may return null (empty
	 * statement).
	 */
	public Stmnt parseStatement(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.lookAhead();
		if (t == '{')
			return this.parseBlock(tbl);
		else if (t == ';')
		{
			this.lex.get();
			return new Stmnt(TokenId.BLOCK); // empty statement
		} else if (t == TokenId.Identifier && this.lex.lookAhead(1) == ':')
		{
			this.lex.get(); // Identifier
			String label = this.lex.getString();
			this.lex.get(); // ':'
			return Stmnt.make(TokenId.LABEL, new Symbol(label), this.parseStatement(tbl));
		} else if (t == TokenId.IF)
			return this.parseIf(tbl);
		else if (t == TokenId.WHILE)
			return this.parseWhile(tbl);
		else if (t == TokenId.DO)
			return this.parseDo(tbl);
		else if (t == TokenId.FOR)
			return this.parseFor(tbl);
		else if (t == TokenId.TRY)
			return this.parseTry(tbl);
		else if (t == TokenId.SWITCH)
			return this.parseSwitch(tbl);
		else if (t == TokenId.SYNCHRONIZED)
			return this.parseSynchronized(tbl);
		else if (t == TokenId.RETURN)
			return this.parseReturn(tbl);
		else if (t == TokenId.THROW)
			return this.parseThrow(tbl);
		else if (t == TokenId.BREAK)
			return this.parseBreak(tbl);
		else if (t == TokenId.CONTINUE)
			return this.parseContinue(tbl);
		else
			return this.parseDeclarationOrExpression(tbl, false);
	}

	private Stmnt parseStmntOrCase(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.lookAhead();
		if (t != TokenId.CASE && t != TokenId.DEFAULT)
			return this.parseStatement(tbl);

		this.lex.get();
		Stmnt s;
		if (t == TokenId.CASE)
			s = new Stmnt(t, this.parseExpression(tbl));
		else
			s = new Stmnt(TokenId.DEFAULT);

		if (this.lex.get() != ':')
			throw new CompileError(": is missing", this.lex);

		return s;
	}

	/*
	 * switch.statement : SWITCH "(" expression ")" "{" switch.block "}"
	 * swtich.block : ( switch.label statement* )* swtich.label : DEFAULT ":" |
	 * CASE const.expression ":"
	 */
	private Stmnt parseSwitch(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // SWITCH
		ASTree expr = this.parseParExpression(tbl);
		Stmnt body = this.parseSwitchBlock(tbl);
		return new Stmnt(t, expr, body);
	}

	private Stmnt parseSwitchBlock(SymbolTable tbl) throws CompileError
	{
		if (this.lex.get() != '{')
			throw new SyntaxError(this.lex);

		SymbolTable tbl2 = new SymbolTable(tbl);
		Stmnt s = this.parseStmntOrCase(tbl2);
		if (s == null)
			throw new CompileError("empty switch block", this.lex);

		int op = s.getOperator();
		if (op != TokenId.CASE && op != TokenId.DEFAULT)
			throw new CompileError("no case or default in a switch block", this.lex);

		Stmnt body = new Stmnt(TokenId.BLOCK, s);
		while (this.lex.lookAhead() != '}')
		{
			Stmnt s2 = this.parseStmntOrCase(tbl2);
			if (s2 != null)
			{
				int op2 = s2.getOperator();
				if (op2 == TokenId.CASE || op2 == TokenId.DEFAULT)
				{
					body = (Stmnt) ASTList.concat(body, new Stmnt(TokenId.BLOCK, s2));
					s = s2;
				} else
					s = (Stmnt) ASTList.concat(s, new Stmnt(TokenId.BLOCK, s2));
			}
		}

		this.lex.get(); // '}'
		return body;
	}

	/*
	 * synchronized.statement : SYNCHRONIZED "(" expression ")" block.statement
	 */
	private Stmnt parseSynchronized(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // SYNCHRONIZED
		if (this.lex.get() != '(')
			throw new SyntaxError(this.lex);

		ASTree expr = this.parseExpression(tbl);
		if (this.lex.get() != ')')
			throw new SyntaxError(this.lex);

		Stmnt body = this.parseBlock(tbl);
		return new Stmnt(t, expr, body);
	}

	/*
	 * throw.statement : THROW expression ";"
	 */
	private Stmnt parseThrow(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // THROW
		ASTree expr = this.parseExpression(tbl);
		if (this.lex.get() != ';')
			throw new CompileError("; is missing", this.lex);

		return new Stmnt(t, expr);
	}

	/*
	 * try.statement : TRY block.statement [ CATCH "(" class.type Identifier ")"
	 * block.statement ]* [ FINALLY block.statement ]*
	 */
	private Stmnt parseTry(SymbolTable tbl) throws CompileError
	{
		this.lex.get(); // TRY
		Stmnt block = this.parseBlock(tbl);
		ASTList catchList = null;
		while (this.lex.lookAhead() == TokenId.CATCH)
		{
			this.lex.get(); // CATCH
			if (this.lex.get() != '(')
				throw new SyntaxError(this.lex);

			SymbolTable tbl2 = new SymbolTable(tbl);
			Declarator d = this.parseFormalParam(tbl2);
			if (d.getArrayDim() > 0 || d.getType() != TokenId.CLASS)
				throw new SyntaxError(this.lex);

			if (this.lex.get() != ')')
				throw new SyntaxError(this.lex);

			Stmnt b = this.parseBlock(tbl2);
			catchList = ASTList.append(catchList, new Pair(d, b));
		}

		Stmnt finallyBlock = null;
		if (this.lex.lookAhead() == TokenId.FINALLY)
		{
			this.lex.get(); // FINALLY
			finallyBlock = this.parseBlock(tbl);
		}

		return Stmnt.make(TokenId.TRY, block, catchList, finallyBlock);
	}

	/*
	 * unary.expr : "++"|"--" unary.expr | "+"|"-" unary.expr | "!"|"~"
	 * unary.expr | cast.expr | postfix.expr unary.expr.not.plus.minus is a
	 * unary expression starting without "+", "-", "++", or "--".
	 */
	private ASTree parseUnaryExpr(SymbolTable tbl) throws CompileError
	{
		int t;
		switch (this.lex.lookAhead())
		{
			case '+':
			case '-':
			case PLUSPLUS:
			case MINUSMINUS:
			case '!':
			case '~':
				t = this.lex.get();
				if (t == '-')
				{
					int t2 = this.lex.lookAhead();
					switch (t2)
					{
						case LongConstant:
						case IntConstant:
						case CharConstant:
							this.lex.get();
							return new IntConst(-this.lex.getLong(), t2);
						case DoubleConstant:
						case FloatConstant:
							this.lex.get();
							return new DoubleConst(-this.lex.getDouble(), t2);
						default:
							break;
					}
				}

				return Expr.make(t, this.parseUnaryExpr(tbl));
			case '(':
				return this.parseCast(tbl);
			default:
				return this.parsePostfix(tbl);
		}
	}

	/*
	 * while.statement : WHILE "(" expression ")" statement
	 */
	private Stmnt parseWhile(SymbolTable tbl) throws CompileError
	{
		int t = this.lex.get(); // WHILE
		ASTree expr = this.parseParExpression(tbl);
		Stmnt body = this.parseStatement(tbl);
		return new Stmnt(t, expr, body);
	}

	private String toClassName(ASTree name) throws CompileError
	{
		StringBuffer sbuf = new StringBuffer();
		this.toClassName(name, sbuf);
		return sbuf.toString();
	}

	private void toClassName(ASTree name, StringBuffer sbuf) throws CompileError
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
				this.toClassName(expr.oprand1(), sbuf);
				sbuf.append('.');
				this.toClassName(expr.oprand2(), sbuf);
				return;
			}
		}

		throw new CompileError("bad static member access", this.lex);
	}
}
