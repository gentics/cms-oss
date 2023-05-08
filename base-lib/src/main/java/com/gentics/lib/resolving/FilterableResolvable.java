/*
 * @author herbert
 * @date May 7, 2008
 * @version $Id: FilterableResolvable.java,v 1.2 2008-05-26 15:05:57 norbert Exp $
 */
package com.gentics.lib.resolving;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;

/**
 * Resolvable which supports filtering when retrieving a foreign link or multi value object link attribute.
 * 
 * @author herbert
 */
public interface FilterableResolvable {

	/**
	 * Returns all linked objects of the given attribute matching the given filter.<br>
	 * <br>
	 * Can only be used on (multivalue) Object links and Foreign links. It will throw an {@link IllegalArgumentException} on all other attribute types.
	 * 
	 * @param key attribute name
	 * @param filter
	 * @return list of all matched objects.
	 * @throws DatasourceException 
	 */
	public DatasourceFilter getFiltered(String key, String filter) throws DatasourceException;
}
