/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: ResolveObjectFilterPart.java,v 1.5 2006-08-23 15:32:18 norbert Exp $
 */
package com.gentics.lib.expressionparser.filtergenerator;

import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;

/**
 * Implementation of a {@link FilterPart} that resolves an object path when
 * merged.
 */
public class ResolveObjectFilterPart extends GenericFilterPart {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -1351610802627401506L;

	/**
	 * object path
	 */
	private String objectPath;

	/**
	 * expected value type
	 */
	private int expectedValueType;

	/**
	 * Create an instance of this filter part
	 * @param filter datasource filter
	 * @param objectPath object path
	 * @param expectedValueType expected value type
	 */
	public ResolveObjectFilterPart(DatasourceFilter filter, String objectPath,
			int expectedValueType) {
		super(filter);
		this.objectPath = objectPath;
		this.expectedValueType = expectedValueType;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#mergeInto(com.gentics.lib.expressionparser.filtergenerator.MergedFilter)
	 */
	public void mergeInto(MergedFilter mergedFilter) throws ExpressionParserException {
		try {
			// resolve the object path into a literal value, generate a
			// filterpart for it and merge it into the mergedFilter
			filter.generateLiteralFilterPart(mergedFilter.getRequest().getResolver().resolve(objectPath), expectedValueType).mergeInto(mergedFilter);
		} catch (FilterGeneratorException e) {
			throw e;
		} catch (Exception e) {
			throw new FilterGeneratorException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer string = new StringBuffer();

		string.append("resolvepart {").append(objectPath).append("}");
		return string.toString();
	}
}
