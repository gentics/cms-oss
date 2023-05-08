/*
 * Created on 27.07.2004
 *
 * TODO implement
 *
 * Changes:
 * 2004.10.13 RRE
 * method setAttributes added
 *
 */
package com.gentics.lib.datasource;

import java.util.Collection;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.rule.Rule;
import com.gentics.lib.log.NodeLogger;

/**
 * DataSourceSearch queries a DataSource and fetches a subset of data according
 * a defined ruleset
 * @author raoul TODO implement
 */
// TODO: define paging interface!
public class DatasourceSearch {
	Rule rule;

	Datasource dataSource;

	int start, count;

	int sortOrder;

	String orderby;

	public DatasourceSearch(Datasource dataSource, Rule rule) {
		// TODO: implement
		start = -1;
		count = -1;
		sortOrder = Datasource.SORTORDER_NONE;
		orderby = null;
		this.rule = rule;
		this.dataSource = dataSource;
		this.dataSource.setRuleTree(rule.getRuleTree());
	}

	public void setDatasource(Datasource dataSource) {
		this.dataSource = dataSource;
		this.dataSource.setRuleTree(rule.getRuleTree());
	}

	public void setRule(Rule rule) {
		this.rule = rule;
		this.dataSource.setRuleTree(rule.getRuleTree());
	}

	public void setLimit(int count) {
		setLimit(0, count);
	}

	public void setLimit(int start, int count) {
		// TODO implement exceptions for illegal arguments
		this.start = start;
		this.count = count;
	}

	public void setSort(String sortBy, int sortOrder) {
		this.orderby = sortBy;
		this.sortOrder = sortOrder;
	}

	public Collection getResult() {

		Collection datasource = null;

		try {

			datasource = this.dataSource.getResult(start, count, orderby, sortOrder);

		} catch (DatasourceNotAvailableException e) {
			// 20040924 added by RRE
			NodeLogger.getLogger(getClass()).error("DatasourceSearch.setSort() an DatasourceNotAvailableException occured", e);
		}

		return datasource;
	}

	/**
	 * public void setAttributeNames(String[] attributes) attribute names will
	 * be passed to the datasource * RRE 2004.10.13 added method
	 * @param attributes
	 */
	public void setAttributeNames(String[] attributes) {

		this.dataSource.setAttributeNames(attributes);

	}
}
