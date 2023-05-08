/*
 * @author norbert
 * @date 31.07.2006
 * @version $Id: SimpleDatasource.java,v 1.1 2006-09-07 11:33:15 laurin Exp $
 */
package com.gentics.lib.datasource.simple;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;

/**
 * Convenience interface for readable datasources. Provides basic methods to
 * fetch objects.
 */
public interface SimpleDatasource extends Datasource {

	/**
	 * Get the object with the given id from the datasource or null if the
	 * object does not exist.
	 * @param id id of the object
	 * @param prefillAttributes array of attribute names to prefill, may be null
	 *        or empty for no attribute prefilling
	 * @return the object with the given id or null if the object does not exist
	 * @throws DatasourceException when an error occurs while fetching the
	 *         object
	 */
	SimpleObject getObject(String id, String[] prefillAttributes) throws DatasourceException;

	/**
	 * Get all objects matching the given filter
	 * @param filter datasource filter to filter returned objects
	 * @param prefillAttributes array of attribute names to prefill, may be null
	 *        or empty for no attribute prefilling
	 * @param start index of the first object to fetch (paging)
	 * @param count maximum number of objects to fetch, -1 for no restriction
	 * @param sortColumns array of sort columns, may be null for no sorting
	 * @return array of objects matching the given rule
	 * @throws DatasourceException when an error occurs while fetching the
	 *         objects (datasource not accessible, rule invalid, ...)
	 */
	SimpleObject[] getObjects(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortColumns) throws DatasourceException;

	/**
	 * Get the objects with the given ids
	 * @param ids array of object ids to fetch
	 * @param prefillAttributes array of attribute names to prefill, may be null
	 *        or empty for no attribute prefilling
	 * @param start index of the first object to fetch (paging)
	 * @param count maximum number of objects to fetch, -1 for no restriction
	 * @param sortColumns array of sort columns, may be null for no sorting
	 * @return array of objects with the given ids
	 * @throws DatasourceException when an error occurs while fetching the
	 *         objects (datasource not accessible, ...)
	 */
	SimpleObject[] getObjectsByID(String[] ids, String[] prefillAttributes, int start, int count,
			Sorting[] sortColumns) throws DatasourceException;

	/**
	 * Get the attribute with given name for the object with the given id
	 * @param id id of the object
	 * @param attributeName name of the attribute to fetch
	 * @return the attribute instance
	 * @throws DatasourceException
	 */
	SimpleAttribute getAttribute(String id, String attributeName) throws DatasourceException;
}
