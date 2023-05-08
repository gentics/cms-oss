/*
 * @author norbert
 * @date 23.08.2006
 * @version $Id: ExpressionQueryRequest.java,v 1.1 2006-08-23 15:32:18 norbert Exp $
 */
package com.gentics.api.lib.expressionparser;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.PropertyResolver;

/**
 * Request object that holds information passed to evaluable expressions and functions
 */
public class ExpressionQueryRequest {

	/**
	 * datasource filter (when a filter is generated or used)
	 */
	private DatasourceFilter filter;

	/**
	 * datasource (when the filter is generated or used for a datasource)
	 */
	private Datasource datasource;

	/**
	 * sorting information (when a filter is used)
	 */
	private Sorting[] sorting;

	/**
	 * start index (when a filter is used)
	 */
	private int start;

	/**
	 * maximum number of objects to filter (when a filter is used)
	 */
	private int count;

	/**
	 * versiontimestamp (when a filter is used on a versioning datasource)
	 */
	private int versionTimestamp;

	/**
	 * additional parameters
	 */
	private Map parameters = new HashMap();

	/**
	 * property resolver
	 */
	private PropertyResolver resolver;

	/**
	 * Get the count
	 * @return Returns the count.
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Set the count
	 * @param count The count to set.
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Get the datasource
	 * @return Returns the datasource.
	 */
	public Datasource getDatasource() {
		return datasource;
	}

	/**
	 * Set the datasource
	 * @param datasource The datasource to set.
	 */
	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}

	/**
	 * Get the filter
	 * @return Returns the filter.
	 */
	public DatasourceFilter getFilter() {
		return filter;
	}

	/**
	 * Set the filter
	 * @param filter The filter to set.
	 */
	public void setFilter(DatasourceFilter filter) {
		this.filter = filter;
	}

	/**
	 * Get the parameters
	 * @return Returns the parameters.
	 */
	public Map getParameters() {
		return parameters;
	}

	/**
	 * Set the parameters
	 * @param parameters The parameters to set.
	 */
	public void setParameters(Map parameters) {
		this.parameters.clear();
		if (parameters != null) {
			this.parameters.putAll(parameters);
		}
	}

	/**
	 * Get the property resolver
	 * @return Returns the resolver.
	 */
	public PropertyResolver getResolver() {
		return resolver;
	}

	/**
	 * Set the property resolver
	 * @param resolver The resolver to set.
	 */
	public void setResolver(PropertyResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Get the sorting
	 * @return Returns the sorting.
	 */
	public Sorting[] getSorting() {
		return sorting;
	}

	/**
	 * Set the sorting
	 * @param sorting The sorting to set.
	 */
	public void setSorting(Sorting[] sorting) {
		this.sorting = sorting;
	}

	/**
	 * Get the start index
	 * @return Returns the start.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * Set the start index
	 * @param start The start to set.
	 */
	public void setStart(int start) {
		this.start = start;
	}

	/**
	 * Get the version timestamp
	 * @return Returns the versionTimestamp.
	 */
	public int getVersionTimestamp() {
		return versionTimestamp;
	}

	/**
	 * Set the version timestamp
	 * @param versionTimestamp The versionTimestamp to set.
	 */
	public void setVersionTimestamp(int versionTimestamp) {
		this.versionTimestamp = versionTimestamp;
	}

	/**
	 * Create an instance of the query request
	 * @param resolver property resolver
	 * @param parameters parameters
	 */
	public ExpressionQueryRequest(PropertyResolver resolver, Map parameters) {
		if (parameters != null) {
			this.parameters.putAll(parameters);
		}
		this.resolver = resolver;
	}

	/**
	 * Create an instance of the query request
	 * @param filter datasource filter
	 * @param datasource datasource
	 * @param resolver property resolver
	 */
	public ExpressionQueryRequest(DatasourceFilter filter, Datasource datasource, PropertyResolver resolver) {
		this.filter = filter;
		this.datasource = datasource;
		this.resolver = resolver;
	}

	/**
	 * Create an instance of the query request
	 * @param filter datasource filter
	 * @param datasource datasource
	 * @param start start index
	 * @param count objects count
	 * @param sorting sorting information
	 * @param versionTimestamp version timestamp
	 * @param resolver property resolver
	 * @param parameters parameters
	 */
	public ExpressionQueryRequest(DatasourceFilter filter, Datasource datasource, int start,
			int count, Sorting[] sorting, int versionTimestamp, PropertyResolver resolver,
			Map parameters) {
		this(resolver, parameters);
		this.filter = filter;
		this.datasource = datasource;
		this.sorting = sorting;
		this.start = start;
		this.count = count;
		this.versionTimestamp = versionTimestamp;
	}
}
