/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: AbstractExpressionParser.java,v 1.3 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.parser;

import com.gentics.contentnode.parser.expression.Expression;
import com.gentics.contentnode.render.RenderResult;

/**
 * This is a generic, abstract implementation of an expression parser which
 * uses a tokenizer and an operandfactory to create a new Expression.
 */
public abstract class AbstractExpressionParser implements ExpressionParser {

	protected AbstractExpressionParser() {}

	/**
	 * This parses a given code template, starting at a given position and returns a parsed expression and
	 * the position after the last character of the expression. The default {@link #CHAR_ESCAPE} is used
	 * as escape character.
	 *
	 * @see #parse(RenderResult, String, int, char, char)
	 * @param result a RenderResult to log any messages.
	 * @param code the code which contains the expression.
	 * @param startPos the first character of the expression to be parsed.
	 * @param enclosure the quote character which encloses the complete expression string, or \0 if not surrounded by quotes.
	 * @return the expressionparser result.
	 */
	public ExpressionParserResult parse(RenderResult result, String code, int startPos, char enclosure) {
		return parse(result, code, startPos, enclosure, CHAR_ESCAPE);
	}

	/**
	 * This parses a given code template, starting at a given position and returns a parsed expression and
	 * the position after the last character of the expression. The escape character to use can be passed
	 * as own parameter.
	 * The Tokenizer and the OperandFactory from the implementing class are used to find and generate the operands.
	 *
	 * @param result a RenderResult to log any messages.
	 * @param code the code which contains the expression.
	 * @param startPos the first character of the expression to be parsed.
	 * @param enclosure the quote character which encloses the complete expression string, or \0 if not surrounded by quotes.
	 * @param escape the character which should be used as escape character.
	 * @return the expressionparser result.
	 */
	public ExpressionParserResult parse(RenderResult result, String code, int startPos, char enclosure, char escape) {

		Expression expression = null;
		int pos = startPos;

		ExpressionTokenizer tokenizer = getTokenizer();

		while (pos < code.length()) {
			pos = tokenizer.parseNextToken(code, pos, escape, enclosure);

			break;
		}

		return new ExpressionParserResult(expression, pos);
	}

	/**
	 * Get a new Tokenizer. The Tokenizer must be a new instance to ensure threadsave
	 * behaviour.
	 * @return a new tokenizer to parse the expression.
	 */
	protected abstract ExpressionTokenizer getTokenizer();

	/**
	 * get the current operandfactory which should be used to create new operands.
	 * @return the reference to the used operandfactory.
	 */
	protected abstract OperandFactory getOperandFactory();
}
