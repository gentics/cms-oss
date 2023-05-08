/*
 * @author norbert
 * @date 25.07.2006
 * @version $Id: AbstractBinaryCNDatasourceFunction.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.lib.datasource.CNDatasourceFilter;
import com.gentics.lib.expressionparser.functions.AbstractGenericBinaryFunction;

/**
 * Abstract base class for binary cndatasource functions.
 */
public abstract class AbstractBinaryCNDatasourceFunction extends AbstractGenericBinaryFunction {

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class[] getSupportedDatasourceClasses() {
		return CNDatasourceFilter.CNDATASOURCEFILTER_FUNCTION;
	}
}
