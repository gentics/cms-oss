/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: DefaultExpressionTokenizer.java,v 1.4 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.parser;

/**
 * A generic implementation of an {@link ExpressionTokenizer} which parses
 * an expression using standard expression syntax and an operandfactory to
 * recognize operand symbols.
 */
public class DefaultExpressionTokenizer implements ExpressionTokenizer {

	private int token;
	private OperandFactory operandFactory;

	/**
	 * Create a new Tokenizer which uses the given operandfactory.
	 * @param operandFactory the operandfactory to use.
	 */
	public DefaultExpressionTokenizer(OperandFactory operandFactory) {
		this.operandFactory = operandFactory;
		this.token = TOKEN_UNKNOWN;
	}

	/**
	 * get the current token type.
	 * @return the last found token type.
	 */
	public int getToken() {
		return token;
	}

	/**
	 * find the next token in the code, and return the position of the next char after the token, which is
	 * also the first char of the next token.
	 *
	 * @param code the code to parse.
	 * @param pos the position of the first character of the token.
	 * @param escape the escape character.
	 * @param enclosing the quote character enclosing the expression, or \0 if not quoted.
	 * @return the position of the first char of the next token.
	 */
	public int parseNextToken(String code, int pos, char escape, char enclosing) {

		char c = code.charAt(pos);

		if (c == '(') {
			token = TOKEN_PARENTHESIS_OPEN;
			return pos + 1;
		}
		if (c == ')') {
			token = TOKEN_PARENTHESIS_CLOSED;
			return pos + 1;
		}
		if (isSeparator(c)) {
			token = TOKEN_SEPARATOR;
			return pos + 1;
		}
		if (c == ',') {
			token = TOKEN_ARGUMENT_SEPARATOR;
			return pos + 1;
		}
		if ((enclosing != 0 && c == enclosing) || c == ';') {
			token = TOKEN_TERMINATOR;
			return pos + 1;
		}

		// gosh, that was easy, now the tricky stuff..

		// TODO handle escape correctly if enclosing != 0
		if (c == '\'' || c == '"') {
			token = TOKEN_CONSTANT;
			pos = parseString(code, pos + 1, c, escape, enclosing);
			return pos;
		}

		return pos + 1;
	}

	private int parseString(String code, int pos, char c, char escape, char enclosing) {

		return pos;
	}

	private boolean isSeparator(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r';
	}
}
