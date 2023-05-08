/*
 * @author robert
 * @date 21.07.2004
 * @version $Id: Datasource.java,v 1.13 2010-09-28 17:01:32 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.RuleTree;

/**
 * Interface for Datasources. Datasources can be configured to
 * store/retrieve data in a generalized way.<br>Instances of this interface
 * represent connections to the underlying storage system and are therefore not
 * threadsafe. <br> This API might still change, and is intended and maintened for usage
 * only. Don't implement your own datasources for now.<br> When the time
 * comes, do not implement this interface directly, but extend the abstract
 * class {@link com.gentics.api.lib.datasource.AbstractDatasource} instead.
 */
public interface Datasource extends Cloneable {

	/**
	 * Constant for the sortorder "NONE" (ie. order of returned row dependend on the underlying database and is most likely random.)
	 */
	public static final int SORTORDER_NONE = 0;

	/**
	 * Constant for the sortorder "ASCENDING"
	 */
	public static final int SORTORDER_ASC = 1;

	/**
	 * Constant for the sortorder "DESCENDING"
	 */
	public static final int SORTORDER_DESC = 2;

	/**
	 * Set the RuleTree to be used as filter when data is retrieved from this
	 * datasource. The RuleTree may refer to attributes of the fetched objects
	 * by "object.[attribute]".<br> Example: setting the RuleTree
	 * <i>object.name == "Username"</i> will filter the Result to those objects
	 * from the datasource that have the attribute "name" set to "Username".
	 * @param ruleTree filter RuleTree
	 * @deprecated RuleTree is deprecated, use {@link Expression} and {@link DatasourceFilter} instead. 
	 * @see #getResult(DatasourceFilter, String[])
	 * @see #createDatasourceFilter(Expression)
	 */
	public void setRuleTree(RuleTree ruleTree);

	/**
	 * Defines Attribute to select or store.
	 * @param names array of attribute names
	 * @deprecated Prefill Attributes can be given in {@link #getResult(DatasourceFilter, String[])} methods.
	 */
	public void setAttributeNames(String[] names);

	/**
	 * Returns data fetched from the Datasource using the given RuleTree as
	 * filter.
	 * @param start index of the first object to get after the filter and sorting have been
	 *        applied (counting starts with 0)
	 * @param count maximum number of objects to return, -1 for no limit
	 * @param sortBy property to sortby, null for unsorted result. May also
	 *        contain a comma separated list of attribute names
	 * @param sortOrder sort order, use one of {@link Datasource#SORTORDER_ASC},
	 *        {@link Datasource#SORTORDER_DESC}
	 *        {@link Datasource#SORTORDER_NONE}
	 * @return a Collection holding the retrieved objects
	 * @throws DatasourceNotAvailableException if the datasource is not available
	 * @deprecated Use {@link #getResult(DatasourceFilter, String[])} methods with DatasourceFilter instead.
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder) throws DatasourceNotAvailableException;

	/**
	 * 
	 * @return a Collection holding the retrieved objects
	 * @throws DatasourceNotAvailableException if the datasource is not available
	 * @deprecated Use {@link #getResult(DatasourceFilter, String[])} methods with DatasourceFilter instead.
	 */
	public Collection getResult() throws DatasourceNotAvailableException;

	/**
	 * Returns data fetched from the Datasource using the given RuleTree as filter.
	 * @param sortBy property to sortby, null for unsorted result. May also
	 *        contain a comma separated list of attribute names
	 * @param sortOrder sort order, use one of {@link Datasource#SORTORDER_ASC},
	 *        {@link Datasource#SORTORDER_DESC}
	 *        {@link Datasource#SORTORDER_NONE}
	 * @return a Collection holding the retrieved objects
	 * @throws DatasourceNotAvailableException if the datasource is not available
	 * @deprecated Use {@link #getResult(DatasourceFilter, String[])} methods with DatasourceFilter instead.
	 */
	public Collection getResult(String sortBy, int sortOrder) throws DatasourceNotAvailableException;

	/**
	 * Returns data fetched from the Datasource using the given RuleTree as filter.
	 * @param start index of the first object to get after the filter and sorting have been
	 *        applied (counting starts with 0)
	 * @param count maximum number of objects to return, -1 for no limit
	 * @param sortBy property to sortby, null for unsorted result. May also
	 *        contain a comma separated list of attribute names
	 * @param sortOrder sort order, use one of {@link Datasource#SORTORDER_ASC},
	 *        {@link Datasource#SORTORDER_DESC}
	 *        {@link Datasource#SORTORDER_NONE}
	 * @param specificParameters map of specific parameters, which will be
	 *        interpreted by some specific Datasources, may be null or empty (no
	 *        specific parameters)
	 * @return a Collection holding the retrieved objects
	 * @throws DatasourceNotAvailableException if the datasource is not available
	 * @deprecated use {@link #getResult(DatasourceFilter, String[])} methods with DatasourceFilter instead.
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder,
			Map specificParameters) throws DatasourceNotAvailableException;

	/**
	 * Get the number of rows matching the given rule.
	 * @return # of rows in the datasource
	 * @deprecated use {@link #getCount(DatasourceFilter)} instead.
	 */
	public int getCount();

	/**
	 * Get the number of rows matching the given rule.
	 * @return # of rows in the datasource
	 * @deprecated use {@link #getCount(DatasourceFilter)} instead.
	 * @throws DatasourceNotAvailableException if the datasource is not available
	 */
	public int getCount2() throws DatasourceNotAvailableException;

	/**
	 * Get the handlepool used by this Datasource. Internal method should not be
	 * invoked from outside.
	 * @return the handlepool
	 */
	public HandlePool getHandlePool();

	/**
	 * Clone the datasource (that means to create a copy that would
	 * work excactly like this datasource)
	 * @return cloned object
	 * @throws CloneNotSupportedException if cloning is not possible
	 */
	Object clone() throws CloneNotSupportedException;

	/**
	 * Return true when data in the underlying storage layer might have changed
	 * since the last invocation of {@link #getResult()} (or any of it's
	 * variants). The specific implementations can simply return true when a
	 * change cannot be determined other than refetching the data or can do any
	 * specific checks for modified data.
	 * @return true when data might have changed, false if no data was changed
	 *         (it is safe for the caller to reuse previously fetched data)
	 * @deprecated deprecated since a datasource should be stateless, use {@link #hasChanged(long)} instead.
	 */
	boolean hasChanged();
    
	/**
	 * Returns true when the data in the underlying storage layer might
	 * have changed since the given timestamp.
	 * 
	 * @param timestamp timestamp to check if data has propably changed - timestamp is a java timestamp - ie. milliseconds since 1970 like {@link System#currentTimeMillis()}
	 * @return true if data has changed since the given timestamp or if it is not impossible to determine if data was changed. - false if it is propably that data has not changed.
	 */
	boolean hasChanged(long timestamp);

	/**
	 * Create a datasource filter for the given expression. The datasource
	 * filter can later be used to filter datasources of the same type.
	 * @param expression expression to transform into a datasource filter
	 * @return datasource filter
	 * @throws ExpressionParserException When the DatasourceFilter can't be created
	 */
	DatasourceFilter createDatasourceFilter(Expression expression) throws ExpressionParserException;

	/**
	 * Get the resolvables from the datasource that match the given datasource
	 * filter (unsorted).
	 * @param filter datasource filter
	 * @param prefillAttributes array of attribute names to prefill (null or empty for no prefilling)
	 * @return collection of resolvables matching the filter
	 * @throws DatasourceException in case of errors
	 * @see #createDatasourceFilter(Expression)
	 */
	Collection<Resolvable> getResult(DatasourceFilter filter, String[] prefillAttributes) throws DatasourceException;

	/**
	 * Get the resolvables from the datasource that match the given datasource filter.
	 * @param filter datasource filter
	 * @param prefillAttributes array of attribute names to prefill (null or empty for no prefilling)
	 * @param start index of the first object to return
	 * @param count maximum number of objects to return (-1 for all objects).
	 * @param sortedColumns possible sorting (may be null for "no sorting")
	 * @return collection of resolvables matching the filter
	 * @throws DatasourceException in case of errors
	 * @see #createDatasourceFilter(Expression)
	 */
	Collection<Resolvable> getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns) throws DatasourceException;

	/**
	 * Get the resolvables from the datasource that match the given datasource filter.
	 * @param filter datasource filter
	 * @param prefillAttributes array of attribute names to prefill (null or empty for no prefilling)
	 * @param start index of the first object to return
	 * @param count maximum number of objects to return (-1 for all objects).
	 * @param sortedColumns possible sorting (may be null for "no sorting")
	 * @param specificParameters map of specific parameters, which will be
	 *        interpreted by some specific Datasources, may be null or empty (no
	 *        specific parameters)
	 * @return collection of resolvables matching the filter
	 * @throws DatasourceException in case of errors
	 * @see #createDatasourceFilter(Expression)
	 */
	Collection<Resolvable> getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, Map<String, Object> specificParameters) throws DatasourceException;

	/**
	 * Get the number of objects in the datasource that match the given filter
	 * @param filter datasource filter
	 * @return number of objects that match the filter
	 * @throws DatasourceException in case of errors
	 * @see #createDatasourceFilter(Expression)
	 */
	int getCount(DatasourceFilter filter) throws DatasourceException;

	/**
	 * Get the number of objects in the datasource that match the given filter
	 * @param filter datasource filter
	 * @param specificParameters map of specific parameters, which will be
	 *        interpreted by some specific Datasources, may be null or empty (no
	 *        specific parameters)
	 * @return number of objects that match the filter
	 * @throws DatasourceException in case of errors
	 * @see #createDatasourceFilter(Expression)
	 */
	int getCount(DatasourceFilter filter, Map<String, Object> specificParameters) throws DatasourceException;
    
	/**
	 * Returns the unique identifier for this Datasource.
	 * @return an unique identifier for this datasource.
	 */
	String getId();
    
	/**
	 * Verifies that the given attribute name is valid and exists in the
	 * datasource. The use behind this is, that it is possible to verify
	 * that a query for a given attribute would succeed. Ie. if the
	 * DatasourceFilter implementation does not check for existing 
	 * attributes this method may always return true.
	 * @param attributeName the attribute name to check
	 * @return true if the attribute is valid and exists, false otherwise.
	 * @throws DatasourceException in case of errors
	 */
	boolean isValidAttribute(String attributeName) throws DatasourceException;

	/**
	 * Inner class for definition of sorting
	 */
	public final static class Sorting implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * sorted attribute
		 */
		protected String columnName;

		/**
		 * sortorder
		 */
		protected int sortOrder;

		/**
		 * Create an instance of the sorting definition. The sortorder must be
		 * one of {@link Datasource#SORTORDER_ASC} or
		 * {@link Datasource#SORTORDER_DESC}.
		 * @param columnName sorted attribute
		 * @param sortOrder sortorder
		 */
		public Sorting(String columnName, int sortOrder) {
			this.columnName = columnName;
			this.sortOrder = sortOrder;
		}

		/**
		 * @return Returns the columnName.
		 */
		public String getColumnName() {
			return columnName;
		}

		/**
		 * @return Returns the sortOrder.
		 */
		public int getSortOrder() {
			return sortOrder;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof Sorting)) {
				return false;
			}
			Sorting sorting = (Sorting) obj;

			return sorting.columnName.equals(this.columnName) && sorting.sortOrder == this.sortOrder;
		}

		public int hashCode() {
			return this.columnName.hashCode() + this.sortOrder;
		}

		public String toString() {
			return new StringBuffer(this.columnName).append(":").append(this.sortOrder).toString();
		}
	}
}
