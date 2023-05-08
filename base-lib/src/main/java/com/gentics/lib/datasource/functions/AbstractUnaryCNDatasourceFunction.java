/*
 * @author norbert
 * @date 25.07.2006
 * @version $Id: AbstractUnaryCNDatasourceFunction.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.lib.datasource.CNDatasourceFilter;
import com.gentics.lib.expressionparser.functions.AbstractGenericUnaryFunction;

/**
 * Abstract base class for unary cndatasource functions.
 */
public abstract class AbstractUnaryCNDatasourceFunction extends AbstractGenericUnaryFunction {

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class[] getSupportedDatasourceClasses() {
		return CNDatasourceFilter.CNDATASOURCEFILTER_FUNCTION;
	}
}
