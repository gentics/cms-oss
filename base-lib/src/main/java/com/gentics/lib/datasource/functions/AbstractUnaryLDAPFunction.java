/*
 * @author norbert
 * @date 27.07.2006
 * @version $Id: AbstractUnaryLDAPFunction.java,v 1.1 2006-07-27 15:00:52 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.lib.datasource.LDAPDatasourceFilter;
import com.gentics.lib.expressionparser.functions.AbstractGenericUnaryFunction;

/**
 * Abstract base class for unary ldapdatasource functions.
 */
public abstract class AbstractUnaryLDAPFunction extends AbstractGenericUnaryFunction {

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class[] getSupportedDatasourceClasses() {
		return LDAPDatasourceFilter.LDAPDATASOURCEFILTER_FUNCTION;
	}
}
