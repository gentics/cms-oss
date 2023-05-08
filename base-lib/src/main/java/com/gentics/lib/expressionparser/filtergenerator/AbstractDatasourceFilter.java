/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: AbstractDatasourceFilter.java,v 1.8 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.lib.expressionparser.filtergenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract base implementation of a datasource filter. Implementors should
 * override this class instead of implementing interface
 * {@link DatasourceFilter}.
 */
public abstract class AbstractDatasourceFilter implements DatasourceFilter {

	/**
	 * map of base object to resolve. keys are the base names, values are
	 * resolvables
	 */
	protected Map baseObjects;

	/**
	 * property resolver for resolving the base objects
	 */
	private PropertyResolver resolver;

	/**
	 * custom property resolver (can be set using {@link #setCustomResolver(PropertyResolver)}).
	 */
	private PropertyResolver customResolver;

	/**
	 * the filter part
	 */
	private FilterPart filterPart = new NestedFilterPart(this);

	/**
	 * expression string of the filter
	 */
	private String expressionString;

	/**
	 * logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Create an instance of the abstract datasource filter
	 *
	 */
	public AbstractDatasourceFilter() {
		this(new HashMap());
	}

	/**
	 * Create an instance of sharing the given base objects map
	 * @param baseObjects base objects
	 */
	public AbstractDatasourceFilter(Map baseObjects) {
		this.baseObjects = baseObjects;
		resolver = new PropertyResolver(new MapResolver(this.baseObjects));
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#generateConstantFilterPart(java.lang.String, java.lang.Object[])
	 */
	public FilterPart generateConstantFilterPart(String constantString, Object[] params) throws FilterGeneratorException {
		// create a constant filter part
		return new ConstantFilterPart(this, constantString, params);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#generateVariableFilterPart(java.lang.String)
	 */
	public FilterPart generateVariableFilterPart(String expressionName, int expectedValueType) throws FilterGeneratorException {
		// create a constant filter part
		return new ConstantFilterPart(this, getVariableName(expressionName, expectedValueType), null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#addBaseResolvable(java.lang.String,
	 *      com.gentics.api.lib.resolving.Resolvable)
	 */
	public void addBaseResolvable(String baseName, Resolvable resolvable) throws FilterGeneratorException {
		if (customResolver != null) {
			throw new FilterGeneratorException("Cannot add resolvables, since a custom resolver has been set");
		}

		if ("object".equals(baseName)) {
			throw new FilterGeneratorException("Cannot add a resolvable with basename {object} to a DatasourceFilter");
		}
		baseObjects.put(baseName, resolvable);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getResolver()
	 */
	public PropertyResolver getResolver() {
		// return the custom resolver if one has been set
		return customResolver != null ? customResolver : resolver;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getMainFilterPart()
	 */
	public FilterPart getMainFilterPart() {
		return filterPart;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter#getExpressionString()
	 */
	public String getExpressionString() {
		return expressionString;
	}

	/**
	 * Set the expression string
	 * @param expressionString expression string
	 */
	public void setExpressionString(String expressionString) {
		this.expressionString = expressionString;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#setResolver(com.gentics.api.lib.resolving.PropertyResolver)
	 */
	public void setCustomResolver(PropertyResolver resolver) {
		customResolver = resolver;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#doPostProcessing(java.util.List, com.gentics.api.lib.expressionparser.ExpressionQueryRequest)
	 */
	public void doPostProcessing(List<Resolvable> result, ExpressionQueryRequest request) throws ExpressionParserException {
		getMainFilterPart().doPostProcessing(result, request);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#hasPostProcessors()
	 */
	public boolean hasPostProcessors() {
		return getMainFilterPart().hasPostProcessors();
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#addPostProcessor(com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor, com.gentics.api.lib.expressionparser.EvaluableExpression)
	 */
	public void addPostProcessor(PostProcessor postProcessor, EvaluableExpression data) throws ExpressionParserException {
		getMainFilterPart().addPostProcessor(postProcessor, data);
	}
}
