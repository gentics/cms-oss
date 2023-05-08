/** Java class "GenticsContentSearch.java" generated from Poseidon for UML.
 *  Poseidon for UML is developed by <A HREF="http://www.gentleware.com">Gentleware</A>.
 *  Generated with <A HREF="http://jakarta.apache.org/velocity/">velocity</A> template engine.
 */
package com.gentics.lib.content;

import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;

/**
 * @author l.herlt@gentics.com
 */
public interface GenticsContentSearch {
	static final int SEARCH_COND_EQUAL = 1;

	static final int SEARCH_COND_LOWER = 2;

	static final int SEARCH_COND_LOWER_EQUAL = 3;

	static final int SEARCH_COND_GREATER = 4;

	static final int SEARCH_COND_GREATER_EQUAL = 5;

	static final int SEARCH_COND_NOT_EQUAL = 6;

	static final int SEARCH_COND_EXPRESSION = 7;

	static final int SORT_ORDER_ASC = 1;

	static final int SORT_ORDER_DESC = 2;

	/**
	 * Add a search to the filter, which searches all available attributes for
	 * the given expression NOTE: this function will perform a attribute match
	 * on all attributes with SEARCH_COND_EXPRESSION
	 * @param value the expression, according to the search-expression specs
	 *        (todo: define specs)
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	void addAllAttributeSearch(String value) throws CMSUnavailableException,
				NodeIllegalArgumentException;

	/**
	 * Adds a new attribute match to the filter. multiple addAttributeMatch
	 * calls are connected via AND NOTE: if the attribute is multi-value, the
	 * same rules as for GenticsContentSearch#addAttributeMatch(String,
	 * String[], int) apply.
	 * @param name attribute name
	 * @param value value to match
	 * @param match match condition (one of GenticsContentSearch.SEARCH_COND_*)
	 * @throws com.gentics.lib.base.NodeIllegalArgumentException
	 * @see GenticsContentSearch#addAttributeMatch(String, String[], int)
	 */
	void addAttributeMatch(String name, String value, int match) throws CMSUnavailableException, NodeIllegalArgumentException;

	/**
	 * analogue to GenticsContentSearch#addAttributeMatch(String, String, int)
	 * match defaults to GenticsContentSearch.SEARCH_COND_EQUAL
	 * @param name
	 * @param value
	 * @throws CMSUnavailableException
	 * @throws com.gentics.lib.base.NodeIllegalArgumentException
	 * @see GenticsContentSearch#addAttributeMatch(String, String, int)
	 */
	void addAttributeMatch(String name, String value) throws CMSUnavailableException,
				NodeIllegalArgumentException;

	/**
	 * Adds a new attribute multivalue match to the filter. <br>
	 * NOTE: there is only one valid match-type for this function at the moment.
	 * <br>
	 * SEARCH_COND_EQUAL <br>
	 * <ul>
	 * <li>attribute is multivalue: will find content which's attribute "name"
	 * has at least one value that equals one of the given values</li>
	 * <li>attribute is singlevalue: will find content which's attribute "name"
	 * equals one of the given values</li>
	 * </ul>
	 * all other SEARCH_COND_* values will trigger an
	 * CMSIllegalArgumentException
	 * @param name attribute name
	 * @param values values to match with
	 * @param match match condition (must be
	 *        GenticsContentSearch.SEARCH_COND_EQUAL atm.)
	 * @throws NodeIllegalArgumentException
	 */
	void addAttributeMatch(String name, String[] values, int match) throws CMSUnavailableException, NodeIllegalArgumentException;

	/**
	 * Removes all attribute matches with the given name from the filter.
	 * @param name attribute name
	 */
	void removeAttributeMatch(String name);

	/**
	 * Get the current filter, which has been set with setFilter and/or
	 * addAttributeMatch. Can be used to add it to other searches.
	 * @return a reusable filter which can be passed to any GenticsContentSearch
	 *         class any time later
	 */
	GenticsContentFilter getFilter();

	/**
	 * Use an existing filter in this search. Overrides the current filter.
	 * @param filter a filter as returned by getFilter()
	 */
	void setFilter(GenticsContentFilter filter) throws NodeIllegalArgumentException;

	/**
	 * analouge to getResult(String, int), but does not apply any sorting.
	 * @return the contentobjects which match the added attribute-matches
	 * @throws CMSUnavailableException
	 * @see GenticsContentSearch#getResult(String, int)
	 */
	GenticsContentResult getResult() throws CMSUnavailableException,
				NodeIllegalArgumentException;

	/**
	 * Search the content repository for matching objects.
	 * @param sortAttribute the attribute by which the result should be sorted;
	 *        must be a single-value attribute
	 * @param sortOrder one of SORT_ORDER_ASC, SORT_ORDER_DESC
	 * @return the contentobjects which match the given attribute-searches
	 */
	GenticsContentResult getResult(String sortAttribute, int sortOrder) throws CMSUnavailableException, NodeIllegalArgumentException;

	GenticsContentResult getResult(String sortColumn, int sortOrder, int start, int count) throws CMSUnavailableException, NodeIllegalArgumentException;

	int getResultCount() throws CMSUnavailableException, NodeIllegalArgumentException;

	/**
	 * params sortColumns and sortOrders must be of same length or both null
	 * @param sortColumns
	 * @param sortOrders
	 * @param start
	 * @param count
	 * @return
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	GenticsContentResult getResult(String[] sortColumns, int[] sortOrders, int start, int count) throws CMSUnavailableException, NodeIllegalArgumentException;
} // end GenticsContentSearch

