package com.gentics.lib.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.gentics.api.lib.datasource.AbstractDatasource;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.rule.RuleTree;

/**
 * created at Oct 17, 2004 this is a simple datasource, that uses a simple map
 * to produce results
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class MapDatasource extends AbstractDatasource {
	Collection rows;

	public MapDatasource(Collection rows) {
		super(null);
		this.rows = rows;
	}

	public void setRuleTree(RuleTree ruleTree) {}

	public void setAttributeNames(String names[]) {}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getResult(int, int,
	 *      java.lang.String, int, java.util.Map)
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder,
			Map specificParameters) throws DatasourceNotAvailableException {
		// default implementation ignores extra parameters
		ArrayList result = new ArrayList();
		Iterator it = rows.iterator();

		while (it.hasNext()) {
			Map map = (Map) it.next();

			result.add(new DefaultDatasourceRow(map));
		}
		return new DefaultDatasourceRecordSet(result);
	}

	public int getCount2() throws DatasourceNotAvailableException {
		return this.rows.size();
	}

	public HandlePool getHandlePool() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("if you have time, please implement me!");
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#hasChanged()
	 */
	public boolean hasChanged() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#createDatasourceFilter(com.gentics.lib.expressionparser.Expression)
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException {
		// FIXME implement this
		throw new FilterGeneratorException("Not yet implemented");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, java.util.Map)
	 */
	public int getCount(DatasourceFilter filter, Map specificParameters) throws DatasourceException {
		// FIXME implement this
		throw new DatasourceException("Not yet implemented");
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, int, int, com.gentics.api.lib.datasource.Datasource.Sorting[], java.util.Map)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start, int count, Sorting[] sortedColumns, Map specificParameters) throws DatasourceException {
		// FIXME implement this
		throw new DatasourceException("Not yet implemented");
	}
}
