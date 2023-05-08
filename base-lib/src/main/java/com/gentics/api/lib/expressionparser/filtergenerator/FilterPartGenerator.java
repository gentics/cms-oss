/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: FilterPartGenerator.java,v 1.2 2006-08-23 15:32:18 norbert Exp $
 */
package com.gentics.api.lib.expressionparser.filtergenerator;

import java.io.Serializable;

import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;

/**
 * Interface for a filter part generator added to the filter during generation.
 * Using instances of {@link FilterPartGenerator} allows generation of
 * filterparts that are dynamically generated when the filter is used to fetch
 * results from a datasource.
 * @see FilterPart#addFilterPartGenerator(FilterPartGenerator)
 */
public interface FilterPartGenerator extends Serializable {

	/**
	 * Method that is called by the filter when it finally merges all filter
	 * parts to use the filter.
	 * @param request expression request
	 * @return the part result
	 * @throws ExpressionParserException in case of errors
	 */
	FilterPart getFilterPart(ExpressionQueryRequest request) throws ExpressionParserException;
}
