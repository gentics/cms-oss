/**
 *
 * @author robert
 * @date 23.09.2004
 * @version $Id: WrapDatasource.java,v 1.18 2006-09-19 14:19:33 norbert Exp $
 *
 * WrapDatasource
 * wraps datasource, is needed for datasourcehandle failover 
 *
 */
package com.gentics.lib.datasource;

import java.util.Collection;
import java.util.Map;

import com.gentics.api.lib.datasource.AbstractDatasource;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.log.NodeLogger;

/**
 * @author robert
 */
public class WrapDatasource extends AbstractDatasource {

	private Datasource datasource;

	/**
	 * maximum number of retries for getting result of wrapped datasource
	 */
	private final static int MAX_TRIES = 3;

	public WrapDatasource(Datasource wrapDataSource) {
		super(null);
		this.datasource = wrapDataSource;

	}

	/**
	 * Returns the wrapped datasource
	 * 
	 * @return
	 */
	public Datasource getWrappedDatasource() {
		return datasource;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#setRuleTree(com.gentics.lib.parser.rule.RuleTree)
	 */
	public void setRuleTree(RuleTree ruleTree) {
		// TODO Auto-generated method stub
		this.datasource.setRuleTree(ruleTree);

	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#setAttributeNames(java.lang.String[])
	 */
	public void setAttributeNames(String[] names) {
		// TODO Auto-generated method stub
		this.datasource.setAttributeNames(names);

	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getResult(int, int,
	 *      java.lang.String, int, java.util.Map)
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder,
			Map specificParameters) throws DatasourceNotAvailableException {

		int numbersOfRetry = 0;
		boolean succeeded = false;

		Collection datasourceRecordSet = null;

		DatasourceNotAvailableException datasourceNotAvailableException = new DatasourceNotAvailableException(
				"WrapDatasource getResult - current datasource is not available!");

		// Failover
		// while ((NUMBERS_OF_RETRY<=MAX_TRIES)||(!succeeded)) {
		while ((numbersOfRetry <= MAX_TRIES) || (succeeded)) {

			try {

				datasourceRecordSet = this.datasource.getResult(start, count, sortBy, sortOrder, specificParameters);
				succeeded = true;
				break;

			} catch (DatasourceNotAvailableException e) {
				NodeLogger.getLogger(getClass()).warn("WrapDatasource getResult failed, number of retry: " + numbersOfRetry);
				datasourceNotAvailableException = e;
				succeeded = false;
			}

			numbersOfRetry++;

		}

		if (!succeeded) {

			NodeLogger.getLogger(getClass()).error("----> WrapDatasource getResult failed, number of retry: " + numbersOfRetry);
			throw datasourceNotAvailableException;
		}

		return datasourceRecordSet;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getCount()
	 */
	public int getCount2() throws DatasourceNotAvailableException {
		// do fallback here
		int numbersOfRetry = 0;
		boolean succeeded = false;

		int result = 0;

		DatasourceNotAvailableException datasourceNotAvailableException = new DatasourceNotAvailableException(
				"WrapDatasource getCount - current datasource is not available!");

		// Failover
		while ((numbersOfRetry <= MAX_TRIES) || (succeeded)) {
			try {
				result = this.datasource.getCount2();
				succeeded = true;
				break;

			} catch (DatasourceNotAvailableException e) {
				NodeLogger.getLogger(getClass()).warn("WrapDatasource getResult failed, number of retry: " + numbersOfRetry);
				datasourceNotAvailableException = e;
				succeeded = false;
			}

			numbersOfRetry++;
		}

		if (!succeeded) {
			NodeLogger.getLogger(getClass()).error("----> WrapDatasource getCount failed, number of retry: " + numbersOfRetry);
			throw datasourceNotAvailableException;
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getHandlePool()
	 */
	public HandlePool getHandlePool() {
		return this.datasource.getHandlePool();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return new WrapDatasource((Datasource) datasource.clone());
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#hasChanged()
	 */
	public boolean hasChanged() {
		return datasource.hasChanged();
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#createDatasourceFilter(com.gentics.lib.expressionparser.Expression)
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException {
		return datasource.createDatasourceFilter(expression);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, java.util.Map)
	 */
	public int getCount(DatasourceFilter filter, Map specificParameters) throws DatasourceException {
		// do fallback here
		int numbersOfRetry = 0;
		boolean succeeded = false;

		int result = 0;

		DatasourceException datasourceNotAvailableException = new DatasourceException("WrapDatasource getCount - current datasource is not available!");

		// Failover
		while ((numbersOfRetry <= MAX_TRIES) || (succeeded)) {
			try {
				result = datasource.getCount(filter, specificParameters);
				succeeded = true;
				break;

			} catch (DatasourceException e) {
				NodeLogger.getLogger(getClass()).warn("WrapDatasource getCount failed, number of retry: " + numbersOfRetry);
				datasourceNotAvailableException = e;
				succeeded = false;
			}

			numbersOfRetry++;
		}

		if (!succeeded) {
			NodeLogger.getLogger(getClass()).error("----> WrapDatasource getCount failed, number of retry: " + numbersOfRetry);
			throw datasourceNotAvailableException;
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, int, int, com.gentics.api.lib.datasource.Datasource.Sorting[], java.util.Map)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start, int count, Sorting[] sortedColumns, Map specificParameters) throws DatasourceException {
		// do fallback here
		int numbersOfRetry = 0;
		boolean succeeded = false;

		Collection result = null;

		DatasourceException datasourceNotAvailableException = new DatasourceException("WrapDatasource getResult - current datasource is not available!");

		// Failover
		while ((numbersOfRetry <= MAX_TRIES) || (succeeded)) {
			try {
				result = datasource.getResult(filter, prefillAttributes, start, count, sortedColumns, specificParameters);
				succeeded = true;
				break;

			} catch (DatasourceException e) {
				NodeLogger.getLogger(getClass()).warn("WrapDatasource getResult failed, number of retry: " + numbersOfRetry);
				datasourceNotAvailableException = e;
				succeeded = false;
			}

			numbersOfRetry++;
		}

		if (!succeeded) {
			NodeLogger.getLogger(getClass()).error("----> WrapDatasource getResult failed, number of retry: " + numbersOfRetry);
			throw datasourceNotAvailableException;
		}

		return result;
	}
}
