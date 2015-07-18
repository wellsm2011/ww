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

public class Lex implements TokenId
{
	// !"#$%&'( )*+,-./0 12345678 9:;<=>?
	private static final int[]			equalOps	=
	{ TokenId.NEQ, 0, 0, 0, TokenId.MOD_E, TokenId.AND_E, 0, 0, 0, TokenId.MUL_E, TokenId.PLUS_E, 0, TokenId.MINUS_E, 0, TokenId.DIV_E, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, TokenId.LE, TokenId.EQ,
			TokenId.GE, 0 };
	private static final KeywordTable	ktable		= new KeywordTable();

	static
	{
		Lex.ktable.append("abstract", TokenId.ABSTRACT);
		Lex.ktable.append("boolean", TokenId.BOOLEAN);
		Lex.ktable.append("break", TokenId.BREAK);
		Lex.ktable.append("byte", TokenId.BYTE);
		Lex.ktable.append("case", TokenId.CASE);
		Lex.ktable.append("catch", TokenId.CATCH);
		Lex.ktable.append("char", TokenId.CHAR);
		Lex.ktable.append("class", TokenId.CLASS);
		Lex.ktable.append("const", TokenId.CONST);
		Lex.ktable.append("continue", TokenId.CONTINUE);
		Lex.ktable.append("default", TokenId.DEFAULT);
		Lex.ktable.append("do", TokenId.DO);
		Lex.ktable.append("double", TokenId.DOUBLE);
		Lex.ktable.append("else", TokenId.ELSE);
		Lex.ktable.append("extends", TokenId.EXTENDS);
		Lex.ktable.append("false", TokenId.FALSE);
		Lex.ktable.append("final", TokenId.FINAL);
		Lex.ktable.append("finally", TokenId.FINALLY);
		Lex.ktable.append("float", TokenId.FLOAT);
		Lex.ktable.append("for", TokenId.FOR);
		Lex.ktable.append("goto", TokenId.GOTO);
		Lex.ktable.append("if", TokenId.IF);
		Lex.ktable.append("implements", TokenId.IMPLEMENTS);
		Lex.ktable.append("import", TokenId.IMPORT);
		Lex.ktable.append("instanceof", TokenId.INSTANCEOF);
		Lex.ktable.append("int", TokenId.INT);
		Lex.ktable.append("interface", TokenId.INTERFACE);
		Lex.ktable.append("long", TokenId.LONG);
		Lex.ktable.append("native", TokenId.NATIVE);
		Lex.ktable.append("new", TokenId.NEW);
		Lex.ktable.append("null", TokenId.NULL);
		Lex.ktable.append("package", TokenId.PACKAGE);
		Lex.ktable.append("private", TokenId.PRIVATE);
		Lex.ktable.append("protected", TokenId.PROTECTED);
		Lex.ktable.append("public", TokenId.PUBLIC);
		Lex.ktable.append("return", TokenId.RETURN);
		Lex.ktable.append("short", TokenId.SHORT);
		Lex.ktable.append("static", TokenId.STATIC);
		Lex.ktable.append("strictfp", TokenId.STRICT);
		Lex.ktable.append("super", TokenId.SUPER);
		Lex.ktable.append("switch", TokenId.SWITCH);
		Lex.ktable.append("synchronized", TokenId.SYNCHRONIZED);
		Lex.ktable.append("this", TokenId.THIS);
		Lex.ktable.append("throw", TokenId.THROW);
		Lex.ktable.append("throws", TokenId.THROWS);
		Lex.ktable.append("transient", TokenId.TRANSIENT);
		Lex.ktable.append("true", TokenId.TRUE);
		Lex.ktable.append("try", TokenId.TRY);
		Lex.ktable.append("void", TokenId.VOID);
		Lex.ktable.append("volatile", TokenId.VOLATILE);
		Lex.ktable.append("while", TokenId.WHILE);
	}

	private static boolean isBlank(int c)
	{
		return c == ' ' || c == '\t' || c == '\f' || c == '\r' || c == '\n';
	}

	private static boolean isDigit(int c)
	{
		return '0' <= c && c <= '9';
	}

	private int lastChar;

	private StringBuffer textBuffer;

	private Token currentToken;

	private Token lookAheadTokens;

	private String input;

	private int position, maxlen, lineNumber;

	/**
	 * Constructs a lexical analyzer.
	 */
	public Lex(String s)
	{
		this.lastChar = -1;
		this.textBuffer = new StringBuffer();
		this.currentToken = new Token();
		this.lookAheadTokens = null;

		this.input = s;
		this.position = 0;
		this.maxlen = s.length();
		this.lineNumber = 0;
	}

	public int get()
	{
		if (this.lookAheadTokens == null)
			return this.get(this.currentToken);
		else
		{
			Token t;
			this.currentToken = t = this.lookAheadTokens;
			this.lookAheadTokens = this.lookAheadTokens.next;
			return t.tokenId;
		}
	}

	private int get(Token token)
	{
		int t;
		do
			t = this.readLine(token);
		while (t == '\n');
		token.tokenId = t;
		return t;
	}

	private int getc()
	{
		if (this.lastChar < 0)
			if (this.position < this.maxlen)
				return this.input.charAt(this.position++);
			else
				return -1;
		else
		{
			int c = this.lastChar;
			this.lastChar = -1;
			return c;
		}
	}

	public double getDouble()
	{
		return this.currentToken.doubleValue;
	}

	public long getLong()
	{
		return this.currentToken.longValue;
	}

	private int getNextNonWhiteChar()
	{
		int c;
		do
		{
			c = this.getc();
			if (c == '/')
			{
				c = this.getc();
				if (c == '/')
					do
						c = this.getc();
					while (c != '\n' && c != '\r' && c != -1);
				else if (c == '*')
					while (true)
					{
						c = this.getc();
						if (c == -1)
							break;
						else if (c == '*')
							if ((c = this.getc()) == '/')
							{
								c = ' ';
								break;
							} else
								this.ungetc(c);
					}
				else
				{
					this.ungetc(c);
					c = '/';
				}
			}
		} while (Lex.isBlank(c));
		return c;
	}

	public String getString()
	{
		return this.currentToken.textValue;
	}

	public String getTextAround()
	{
		int begin = this.position - 10;
		if (begin < 0)
			begin = 0;

		int end = this.position + 10;
		if (end > this.maxlen)
			end = this.maxlen;

		return this.input.substring(begin, end);
	}

	/**
	 * Looks at the next token.
	 */
	public int lookAhead()
	{
		return this.lookAhead(0);
	}

	public int lookAhead(int i)
	{
		Token tk = this.lookAheadTokens;
		if (tk == null)
		{
			this.lookAheadTokens = tk = this.currentToken; // reuse an object!
			tk.next = null;
			this.get(tk);
		}

		for (; i-- > 0; tk = tk.next)
			if (tk.next == null)
			{
				Token tk2;
				tk.next = tk2 = new Token();
				this.get(tk2);
			}

		this.currentToken = tk;
		return tk.tokenId;
	}

	private int readCharConst(Token token)
	{
		int c;
		int value = 0;
		while ((c = this.getc()) != '\'')
			if (c == '\\')
				value = this.readEscapeChar();
			else if (c < 0x20)
			{
				if (c == '\n')
					++this.lineNumber;

				return TokenId.BadToken;
			} else
				value = c;

		token.longValue = value;
		return TokenId.CharConstant;
	}

	private int readDouble(StringBuffer sbuf, int c, Token token)
	{
		if (c != 'E' && c != 'e' && c != 'D' && c != 'd')
		{
			sbuf.append((char) c);
			for (;;)
			{
				c = this.getc();
				if ('0' <= c && c <= '9')
					sbuf.append((char) c);
				else
					break;
			}
		}

		if (c == 'E' || c == 'e')
		{
			sbuf.append((char) c);
			c = this.getc();
			if (c == '+' || c == '-')
			{
				sbuf.append((char) c);
				c = this.getc();
			}

			while ('0' <= c && c <= '9')
			{
				sbuf.append((char) c);
				c = this.getc();
			}
		}

		try
		{
			token.doubleValue = Double.parseDouble(sbuf.toString());
		} catch (NumberFormatException e)
		{
			return TokenId.BadToken;
		}

		if (c == 'F' || c == 'f')
			return TokenId.FloatConstant;
		else
		{
			if (c != 'D' && c != 'd')
				this.ungetc(c);

			return TokenId.DoubleConstant;
		}
	}

	private int readEscapeChar()
	{
		int c = this.getc();
		if (c == 'n')
			c = '\n';
		else if (c == 't')
			c = '\t';
		else if (c == 'r')
			c = '\r';
		else if (c == 'f')
			c = '\f';
		else if (c == '\n')
			++this.lineNumber;

		return c;
	}

	private int readIdentifier(int c, Token token)
	{
		StringBuffer tbuf = this.textBuffer;
		tbuf.setLength(0);

		do
		{
			tbuf.append((char) c);
			c = this.getc();
		} while (Character.isJavaIdentifierPart((char) c));

		this.ungetc(c);

		String name = tbuf.toString();
		int t = Lex.ktable.lookup(name);
		if (t >= 0)
			return t;
		else
		{
			/*
			 * tbuf.toString() is executed quickly since it does not need memory
			 * copy. Using a hand-written extensible byte-array class instead of
			 * StringBuffer is not a good idea for execution speed. Converting a
			 * byte array to a String object is very slow. Using an extensible
			 * char array might be OK.
			 */
			token.textValue = name;
			return TokenId.Identifier;
		}
	}

	private int readLine(Token token)
	{
		int c = this.getNextNonWhiteChar();
		if (c < 0)
			return c;
		else if (c == '\n')
		{
			++this.lineNumber;
			return '\n';
		} else if (c == '\'')
			return this.readCharConst(token);
		else if (c == '"')
			return this.readStringL(token);
		else if ('0' <= c && c <= '9')
			return this.readNumber(c, token);
		else if (c == '.')
		{
			c = this.getc();
			if ('0' <= c && c <= '9')
			{
				StringBuffer tbuf = this.textBuffer;
				tbuf.setLength(0);
				tbuf.append('.');
				return this.readDouble(tbuf, c, token);
			} else
			{
				this.ungetc(c);
				return this.readSeparator('.');
			}
		} else if (Character.isJavaIdentifierStart((char) c))
			return this.readIdentifier(c, token);
		else
			return this.readSeparator(c);
	}

	private int readNumber(int c, Token token)
	{
		long value = 0;
		int c2 = this.getc();
		if (c == '0')
			if (c2 == 'X' || c2 == 'x')
				for (;;)
				{
					c = this.getc();
					if ('0' <= c && c <= '9')
						value = value * 16 + c - '0';
					else if ('A' <= c && c <= 'F')
						value = value * 16 + c - 'A' + 10;
					else if ('a' <= c && c <= 'f')
						value = value * 16 + c - 'a' + 10;
					else
					{
						token.longValue = value;
						if (c == 'L' || c == 'l')
							return TokenId.LongConstant;
						else
						{
							this.ungetc(c);
							return TokenId.IntConstant;
						}
					}
				}
			else if ('0' <= c2 && c2 <= '7')
			{
				value = c2 - '0';
				for (;;)
				{
					c = this.getc();
					if ('0' <= c && c <= '7')
						value = value * 8 + c - '0';
					else
					{
						token.longValue = value;
						if (c == 'L' || c == 'l')
							return TokenId.LongConstant;
						else
						{
							this.ungetc(c);
							return TokenId.IntConstant;
						}
					}
				}
			}

		value = c - '0';
		while ('0' <= c2 && c2 <= '9')
		{
			value = value * 10 + c2 - '0';
			c2 = this.getc();
		}

		token.longValue = value;
		if (c2 == 'F' || c2 == 'f')
		{
			token.doubleValue = value;
			return TokenId.FloatConstant;
		} else if (c2 == 'E' || c2 == 'e' || c2 == 'D' || c2 == 'd' || c2 == '.')
		{
			StringBuffer tbuf = this.textBuffer;
			tbuf.setLength(0);
			tbuf.append(value);
			return this.readDouble(tbuf, c2, token);
		} else if (c2 == 'L' || c2 == 'l')
			return TokenId.LongConstant;
		else
		{
			this.ungetc(c2);
			return TokenId.IntConstant;
		}
	}

	private int readSeparator(int c)
	{
		int c2, c3;
		if ('!' <= c && c <= '?')
		{
			int t = Lex.equalOps[c - '!'];
			if (t == 0)
				return c;
			else
			{
				c2 = this.getc();
				if (c == c2)
					switch (c)
					{
						case '=':
							return TokenId.EQ;
						case '+':
							return TokenId.PLUSPLUS;
						case '-':
							return TokenId.MINUSMINUS;
						case '&':
							return TokenId.ANDAND;
						case '<':
							c3 = this.getc();
							if (c3 == '=')
								return TokenId.LSHIFT_E;
							else
							{
								this.ungetc(c3);
								return TokenId.LSHIFT;
							}
						case '>':
							c3 = this.getc();
							if (c3 == '=')
								return TokenId.RSHIFT_E;
							else if (c3 == '>')
							{
								c3 = this.getc();
								if (c3 == '=')
									return TokenId.ARSHIFT_E;
								else
								{
									this.ungetc(c3);
									return TokenId.ARSHIFT;
								}
							} else
							{
								this.ungetc(c3);
								return TokenId.RSHIFT;
							}
						default:
							break;
					}
				else if (c2 == '=')
					return t;
			}
		} else if (c == '^')
		{
			c2 = this.getc();
			if (c2 == '=')
				return TokenId.EXOR_E;
		} else if (c == '|')
		{
			c2 = this.getc();
			if (c2 == '=')
				return TokenId.OR_E;
			else if (c2 == '|')
				return TokenId.OROR;
		} else
			return c;

		this.ungetc(c2);
		return c;
	}

	private int readStringL(Token token)
	{
		int c;
		StringBuffer tbuf = this.textBuffer;
		tbuf.setLength(0);
		for (;;)
		{
			while ((c = this.getc()) != '"')
			{
				if (c == '\\')
					c = this.readEscapeChar();
				else if (c == '\n' || c < 0)
				{
					++this.lineNumber;
					return TokenId.BadToken;
				}

				tbuf.append((char) c);
			}

			for (;;)
			{
				c = this.getc();
				if (c == '\n')
					++this.lineNumber;
				else if (!Lex.isBlank(c))
					break;
			}

			if (c != '"')
			{
				this.ungetc(c);
				break;
			}
		}

		token.textValue = tbuf.toString();
		return TokenId.StringL;
	}

	private void ungetc(int c)
	{
		this.lastChar = c;
	}
}

class Token
{
	public Token	next	= null;
	public int		tokenId;

	public long		longValue;
	public double	doubleValue;
	public String	textValue;
}
