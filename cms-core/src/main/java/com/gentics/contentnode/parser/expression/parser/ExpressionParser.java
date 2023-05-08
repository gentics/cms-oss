/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ExpressionParser.java,v 1.2 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.parser;

import com.gentics.contentnode.render.RenderResult;

/**
 * An ExpressionParser parses a given code template, starting at a given position and creates a new expression
 * from the code. The position of the end of the expression is also returned.
 * The Expressionparser should be a stateless implementation, so that the same instance can be used
 * in several threads simultaniously.
 */
public interface ExpressionParser {

	/**
	 * Default escape character.
	 */
	final static char CHAR_ESCAPE = '\\';

	/**
	 * This parses a given code template, starting at a given position and returns a parsed expression and
	 * the position after the last character of the expression. The default {@link #CHAR_ESCAPE} is used
	 * as escape character.
	 *
	 * @see ExpressionParserResult
	 * @param result a RenderResult to log any messages.
	 * @param code the code which contains the expression.
	 * @param startPos the first character of the expression to be parsed.
	 * @param enclosure the quote character which encloses the complete expression string, or \0 if not surrounded by quotes.
	 * @return the expressionparser result.
	 */
	ExpressionParserResult parse(RenderResult result, String code, int startPos, char enclosure);

	/**
	 * This parses a given code template, starting at a given position and returns a parsed expression and
	 * the position after the last character of the expression. The escape character to use can be passed
	 * as own parameter.
	 *
	 * @see ExpressionParserResult
	 * @param result a RenderResult to log any messages.
	 * @param code the code which contains the expression.
	 * @param startPos the first character of the expression to be parsed.
	 * @param enclosure the quote character which encloses the complete expression string, or \0 if not surrounded by quotes.
	 * @param escape the character which should be used as escape character.
	 * @return the expressionparser result.
	 */ 
	ExpressionParserResult parse(RenderResult result, String code, int startPos, char enclosure, char escape);
}
