/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: AbstractGenericBinaryFunction.java,v 1.2 2006-07-21 14:45:08 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

/**
 * Abstract base class for generic binary functions.
 */
public abstract class AbstractGenericBinaryFunction extends AbstractGenericFunction {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return 2;
	}
}
