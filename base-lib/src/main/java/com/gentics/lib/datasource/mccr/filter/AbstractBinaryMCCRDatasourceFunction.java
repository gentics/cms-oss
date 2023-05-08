package com.gentics.lib.datasource.mccr.filter;

import com.gentics.lib.expressionparser.functions.AbstractGenericBinaryFunction;

/**
 * Abstract base class for binary mccrdatasource functions.
 */
public abstract class AbstractBinaryMCCRDatasourceFunction extends AbstractGenericBinaryFunction {

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class<?>[] getSupportedDatasourceClasses() {
		return MCCRDatasourceFilter.MCCRFILTER_FUNCTION;
	}
}
