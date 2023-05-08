/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: AbstractGenericUnaryFunction.java,v 1.2 2006-07-21 14:45:08 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

/**
 * Abstract base class for generic unary functions.
 */
public abstract class AbstractGenericUnaryFunction extends AbstractGenericFunction {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return 1;
	}
}
