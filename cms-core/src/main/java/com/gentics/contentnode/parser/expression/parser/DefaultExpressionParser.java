/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: DefaultExpressionParser.java,v 1.2 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.parser;

/**
 * This is a default implementation of an ExpressionParser, which uses the {@link AbstractExpressionParser}
 * implementation, a {@link DefaultOperandFactory} and a {@link DefaultExpressionTokenizer} to parse
 * expressions.
 */
public class DefaultExpressionParser extends AbstractExpressionParser {

	private OperandFactory operandFactory;

	/**
	 * Main constructor to create a new expressionparser which initializes a factory
	 * with all known generic operands.
	 */
	public DefaultExpressionParser() {
		super();
		operandFactory = new DefaultOperandFactory(true);
	}

	/**
	 * Get a new default tokenizer which uses the operandfactory.
	 * @return a new tokenizer.
	 */
	protected ExpressionTokenizer getTokenizer() {
		return new DefaultExpressionTokenizer(operandFactory);
	}

	/**
	 * get the operandfactory used to create operands.
	 * @return the current operandfactory.
	 */
	protected OperandFactory getOperandFactory() {
		return operandFactory;
	}

}
