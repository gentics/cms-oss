/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: Operand.java,v 1.4 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression;

import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * The Operand is the main interface for all expression-parts. An operand
 * can be evaluated and returns the result of the evaluation as object.
 * Usually an operand is either a BinaryOperand, a Left- or RightUnaryOperand,
 * a Function, a Constant, or a Variable (which is simply a 'named operand container').
 *
 * Note: the result value can also be null. Comparator-Operands will always return a Boolean.
 */
public interface Operand {

	/**
	 * get the operand's symbol. This only returns the operand's symbol itself,
	 * without symbols for sub-operands or arguments. The symbol is used to
	 * find an operand within an expression-code.
	 *
	 * @return the operand-implementation's unique symbol, or null if not applicable.
	 */
	String getOperandSymbol();

	/**
	 * Evaluate the current operand. The type and value of the result depends on the
	 * implementation.
	 *
	 * @param renderType a renderType with some infos about the current rendering methods.
	 * @param renderResult a renderResult where messages and logs can be added.
	 * @return the result of the operation.
	 */
	Object evaluate(RenderType renderType, RenderResult renderResult);
    
}
