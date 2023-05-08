/*
 * @author erwin mascher
 * @date 17.10.2004
 * @version $Id: ResolvableDatasource.java,v 1.15 2009-12-16 16:12:07 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableComparator;
import com.gentics.api.lib.rule.Rule;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.datasource.DefaultDatasourceRecordSet;
import com.gentics.lib.datasource.ResolvableDatasourceFilter;
import com.gentics.lib.datasource.ResolvableDatasourceRow;
import com.gentics.lib.log.NodeLogger;

/**
 * Simple Implementation of a Datasource that is based on a given
 * {@link java.util.Collection} of
 * {@link com.gentics.api.lib.resolving.Resolvable} objects. The class itself
 * implements also the interface {@link java.util.Collection} and serves as
 * wrapper for the underlying Collection of objects.
 * As Datasource this class supports filtering with {@link com.gentics.api.lib.rule.RuleTree}s, sorting and paging.
 */
public class ResolvableDatasource extends AbstractDatasource implements Collection {

	/**
	 * Internal storage
	 */
	private Collection rows = new Vector();

	/**
	 * currently used ruletree
	 */
	private RuleTree ruleTree;

	/**
	 * internal rule to apply the ruletree
	 */
	private Rule rule;

	/**
	 * logger object
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Create an empty instance of the ResolvableDatasource
	 *
	 */
	public ResolvableDatasource() {
		super(null);
	}

	/**
	 * Create an instance of the ResolvableDatasource that holds the objects given in the collection
	 * @param rows collection of Resolvable objects
	 */
	public ResolvableDatasource(Collection rows) {
		super(null);
		if (rows != null) {
			addAll(rows);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#setRuleTree(com.gentics.api.lib.rule.RuleTree)
	 */
	public void setRuleTree(RuleTree ruleTree) {
		this.ruleTree = ruleTree;
		if (this.ruleTree != null) {
			rule = new Rule(this.ruleTree);
		} else {
			rule = null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#setAttributeNames(java.lang.String[])
	 */
	public void setAttributeNames(String names[]) {// empty implementation
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getResult(int, int,
	 *      java.lang.String, int, java.util.Map)
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder,
			Map specificParameters) throws DatasourceNotAvailableException {
		// default implementation ignores extra parameters
		List result = new ArrayList();
		Iterator it = rows.iterator();

		if (sortBy == null || sortBy.length() == 0) {
			// no sorting shall be used, simply fetch the elements and eventually apply the rule
			int counter = 0;

			while (it.hasNext() && (count < 0 || counter < start + count)) {
				Resolvable resolvable = (Resolvable) it.next();
				// boolean ruleTreeValue = applyRuleTree(resolvable, ruleTree);
				boolean ruleTreeValue = rule != null ? rule.match(resolvable) : true;

				// if the rule tree can be applied, add the object to the result
				if (ruleTreeValue) {
					if (counter >= start) {

						// omit some rows when start is > 0
						result.add(new ResolvableDatasourceRow(resolvable));
					}
					counter++;
				}
				// else do nothing
			}
		} else {
			// we have to do some sorting, so FIRST get all elements (eventually
			// applying the rule), THEN sort them and LAST fetch the requested
			// sublist

			// fetch all objects matching the rule
			while (it.hasNext()) {
				Resolvable resolvable = (Resolvable) it.next();
				boolean ruleTreeValue = rule != null ? rule.match(resolvable) : true;

				// if the rule tree can be applied, add the object to the result
				if (ruleTreeValue) {
					result.add(new ResolvableDatasourceRow(resolvable));
				}
			}

			// now sort
			Sorter sorter = new Sorter(sortBy, sortOrder);

			try {
				Collections.sort(result, sorter);
			} catch (Exception ex) {
				logger.error("error while sorting: ", ex);
			}

			// finally trim the resultslist, if requested
			if (start >= 0) {
				result = result.subList(start, count < 0 ? result.size() : Math.min(start + count, result.size()));
			}
		}
		return new DefaultDatasourceRecordSet(result);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#getCount2()
	 */
	public int getCount2() throws DatasourceNotAvailableException {
		int count = 0;

		if (rows != null) {
			if (rule == null) {
				// no rule set, count all rows
				count = rows.size();
			} else if (rule.isConstant()) {
				// constant rule set, count all rows when the rule matches
				count = rule.match() ? rows.size() : 0;
			} else {
				// non-constant rule set, count the objects that match the rule
				for (Iterator i = rows.iterator(); i.hasNext();) {
					Resolvable res = (Resolvable) i.next();

					if (rule.match(res)) {
						count++;
					}
				}
			}
		}

		return count;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getHandlePool()
	 */
	public HandlePool getHandlePool() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Not yet implemented!");
	}

	/**
	 * Comparator class for sorting Resolvables
	 * TODO unify this (use the generic Comparator for resolvables)
	 * @author norbert
	 */
	protected class Sorter implements Comparator {

		/**
		 * array of sort attributes
		 */
		String[] sortAttributes = null;

		/**
		 * sort order, one of {@link Datasource#SORTORDER_ASC} or {@link Datasource#SORTORDER_DESC}
		 */
		int sortOrder = SORTORDER_ASC;

		/**
		 * create instance of the sorter
		 * @param sortBy comma separated list of sort attributes
		 * @param sortOrder sort order
		 */
		public Sorter(String sortBy, int sortOrder) {
			sortAttributes = sortBy.split(",");
			this.sortOrder = sortOrder;
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			Resolvable r1 = (Resolvable) ((ResolvableDatasourceRow) o1).toObject();
			Resolvable r2 = (Resolvable) ((ResolvableDatasourceRow) o2).toObject();

			int compareResult = 0;

			for (int i = 0; i < sortAttributes.length && compareResult == 0; ++i) {
				Object value1 = r1.get(sortAttributes[i]);
				Object value2 = r2.get(sortAttributes[i]);

				if (value1 instanceof Comparable) {
					if (value2 == null) {
						compareResult = 1;
					} else {
						compareResult = ((Comparable) value1).compareTo(value2);
					}
				} else if (value1 == null) {
					compareResult = value2 == null ? 0 : -1;
				}
			}

			if (sortOrder == SORTORDER_DESC) {
				compareResult = -compareResult;
			}
			return compareResult;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.Datasource#hasChanged()
	 */
	public boolean hasChanged() {
		return true;
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#hasChanged(long)
	 */
	public boolean hasChanged(long timestamp) {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return rows.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		rows.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return rows.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() {
		return rows.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(Object o) {
		return rows.add(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return rows.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		return rows.remove(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection c) {
		return rows.addAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection c) {
		return rows.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection c) {
		return removeAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection c) {
		return retainAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#iterator()
	 */
	public Iterator iterator() {
		return rows.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(java.lang.Object[])
	 */
	public Object[] toArray(Object[] a) {
		return rows.toArray(a);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#createDatasourceFilter(com.gentics.lib.expressionparser.Expression)
	 */
	public DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException {
		return new ResolvableDatasourceFilter(expression);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      int, int, com.gentics.api.lib.datasource.Datasource.Sorting[],
	 *      java.util.Map)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, Map specificParameters) throws DatasourceException {
		ResolvableDatasourceFilter resolvableDatasourceFilter = getAsResolvableDatasourceFilter(filter);
		ExpressionEvaluator evaluator = resolvableDatasourceFilter.getExpressionEvaluator();
		List<Resolvable> matchingObjects = new ArrayList<Resolvable>();

		try {
			// first get all matching objects
			matchingObjects.addAll(rows);
			evaluator.filter(resolvableDatasourceFilter.getExpression(), matchingObjects);

			// next do sorting (if requested)
			if (sortedColumns != null) {
				ResolvableComparator comparator = new ResolvableComparator(sortedColumns);

				Collections.sort(matchingObjects, comparator);
			}

			// normalize the start
			start = Math.max(start, 0);
			int endIndex = count < 0 ? matchingObjects.size() : Math.min(start + count, matchingObjects.size());

			// third, if start and/or count is given, fetch the sublist
			if (start > 0 || endIndex < matchingObjects.size()) {
				matchingObjects = new Vector(matchingObjects.subList(start, endIndex));
			}
		} catch (ExpressionParserException e) {
			throw new DatasourceException("Error while getting filtered results", e);
		}

		return matchingObjects;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, java.util.Map)
	 */
	public int getCount(DatasourceFilter filter, Map specificParameters) throws DatasourceException {
		Collection results = getResult(filter, null, 0, -1, null, specificParameters);

		return results.size();
	}

	/**
	 * Get the datasource filter as resolvable datasource filter
	 * @param filter filter
	 * @return resolvable datasource filter
	 * @throws DatasourceException in case of errors
	 */
	protected ResolvableDatasourceFilter getAsResolvableDatasourceFilter(DatasourceFilter filter) throws DatasourceException {
		if (filter instanceof ResolvableDatasourceFilter) {
			return (ResolvableDatasourceFilter) filter;
		} else {
			throw new DatasourceException("Incompatible filter");
		}
	}
}
