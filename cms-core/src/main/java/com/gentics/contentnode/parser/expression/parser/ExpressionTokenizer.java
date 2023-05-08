/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: ExpressionTokenizer.java,v 1.4 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.parser;

/**
 * The ExpressionTokenizer is used by the ExpressionParser to split the given code into tokens one by one.
 * The tokenizer is primarily used to determine the positions of the start- and end-positions of
 * the words in the code and therefore split the code into blocks with a token-type for better handling.
 *
 * The expressionparser usually uses an OperandFactory to find the keynames of the operands and implements
 * the syntax rules of the expression.
 *
 * The tokenizer stores information about the last found token and must therefore be instantiated for each
 * expression. It should therefore not hold any memory-excessive fields.
 */
public interface ExpressionTokenizer {

	/**
	 * Token type for unknown characters.
	 */
	public final static int TOKEN_UNKNOWN = 0;

	/**
	 * Token type for opening parenthesis
	 */
	public final static int TOKEN_PARENTHESIS_OPEN = 1;

	/**
	 * Token type for closing parenthesis
	 */
	public final static int TOKEN_PARENTHESIS_CLOSED = 2;

	/**
	 * Token type for variables.
	 */
	public final static int TOKEN_VARIABLE = 3;

	/**
	 * Token type for constants.
	 */
	public final static int TOKEN_CONSTANT = 4;

	/**
	 * Token type for functions.
	 */
	public final static int TOKEN_FUNCTION = 5;

	/**
	 * Token type for binary operands
	 */
	public final static int TOKEN_BINARY_OPERAND = 6;

	/**
	 * Token type for left operands.
	 */
	public final static int TOKEN_LEFT_UNARY_OPERAND = 7;

	/**
	 * Token type for right operands.
	 */
	public final static int TOKEN_RIGHT_UNARY_OPERAND = 8;

	/**
	 * Token type for word separators.
	 */
	public final static int TOKEN_SEPARATOR = 9;

	/**
	 * Token type for function argument separators.
	 */
	public final static int TOKEN_ARGUMENT_SEPARATOR = 10;

	/**
	 * Token type for an expression closing char, like ; or the matching surrounding closing quote.
	 */
	public final static int TOKEN_TERMINATOR = 11;

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
	int parseNextToken(String code, int pos, char escape, char enclosing);

	/**
	 * get the token type of the last found token.
	 * @return the type of the last token.
	 */
	int getToken();
}
