/*
 * @author norbert
 * @date 27.07.2006
 * @version $Id: AbstractLDAPFunction.java,v 1.1 2006-07-27 15:00:52 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.lib.datasource.LDAPDatasourceFilter;
import com.gentics.lib.expressionparser.functions.AbstractGenericFunction;

/**
 * Abstract base class for ldapdatasource functions.
 */
public abstract class AbstractLDAPFunction extends AbstractGenericFunction {

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class[] getSupportedDatasourceClasses() {
		return LDAPDatasourceFilter.LDAPDATASOURCEFILTER_FUNCTION;
	}
}
