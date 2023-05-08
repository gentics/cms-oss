/*
 * @author norbert
 * @date 25.03.2005
 * @version $Id: SubRuleResolver.java,v 1.6 2006-03-27 15:54:12 herbert Exp $
 */
package com.gentics.lib.parser.rule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.RuleTree;

/**
 * TODO comment this
 * @author norbert
 */
public class SubRuleResolver implements Resolvable {

	/**
	 * ruletree used for the subrule
	 */
	private RuleTree ruleTree;

	/**
	 * datasource used to fetch the objects matching the subrule
	 */
	private Datasource datasource;

	/**
	 * string containing the subrule
	 */
	private String subRule;

	/**
	 * cached results
	 */
	private Map cachedResults = new HashMap();

	/**
	 * results fetched from the datasource
	 */
	private Collection recordSet;

	/**
	 * create instance of the SubRuleResolver
	 * @param ruleTree ruletree for the subrule
	 * @param datasource datasource to use
	 */
	public SubRuleResolver(RuleTree ruleTree, Datasource datasource, String subRule) throws ParserException {
		this.ruleTree = ruleTree;
		this.datasource = datasource;
		this.subRule = subRule;
		init();
	}

	/**
	 * initialize the subrule resolver
	 */
	public void init() throws ParserException {
		ruleTree.parse(subRule);
		datasource.setRuleTree(ruleTree);
		cachedResults.clear();
		recordSet = null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		if (recordSet == null) {
			try {
				// when the recordset is null, we have to fetch the results from
				// the datasource first
				recordSet = datasource.getResult();
			} catch (DatasourceNotAvailableException e) {
				// TODO think about the logger here
				e.printStackTrace();
				return null;
			}
		}

		if (!cachedResults.containsKey(key)) {
			List results = new Vector();

			for (Iterator iter = recordSet.iterator(); iter.hasNext();) {
				DatasourceRow element = (DatasourceRow) iter.next();
				String rowResult = element.getString(key);

				if (rowResult != null && !results.contains(rowResult)) {
					results.add(rowResult);
				}
			}

			cachedResults.put(key, results);
		}

		return cachedResults.get(key);
	}

	public Object get(String key) {
		return getProperty(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getPropertyNames()
	 */
	public HashMap getPropertyNames() {
		return null;
	}
}
