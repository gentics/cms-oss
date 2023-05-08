/*
 * @author norbert
 * @date 27.07.2006
 * @version $Id: AbstractDatasource.java,v 1.7 2007-04-13 09:42:29 norbert Exp $
 */
package com.gentics.api.lib.datasource;

import java.util.Collection;

import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract basic implementation for datasources. Implementing your own datasources is
 * not supported right now. When the time has come, one should rather
 * extend this class than implement the interface {@link Datasource} directly
 * in order to
 * <ul>
 * <li>Use default implementation</li>
 * <li>Reduce implementation modification effort when the interface might
 * change in future releases.</li>
 * </ul>
 */
public abstract class AbstractDatasource implements Datasource {
    
	/**
	 * The unique identifier for this datasource.
	 * @see #getId()
	 */
	private String id;

	/**
	 * 
	 * @param id Id for this datasource.
	 */
	public AbstractDatasource(String id) {
		this.id = id;
	}
    
	/**
	 * If you use this super constructor as implementor, you should always
	 * call {@link #setId(String)} afterwards - but it is recommended to
	 * use {@link #AbstractDatasource(String)} instead.
	 */
	public AbstractDatasource() {}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Not implemented");
	}

	/**
	 * 
	 * @see com.gentics.api.lib.datasource.Datasource#getResult()
	 * @deprecated use {@link #getResult(DatasourceFilter, String[])} instead.
	 */
	public Collection getResult() throws DatasourceNotAvailableException {
		// default implementation: forward this call with default parameters
		return getResult(-1, -1, null, Datasource.SORTORDER_NONE, null);
	}

	/**
	 * 
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(int, int,
	 *      java.lang.String, int)
	 * @deprecated use {@link #getResult(DatasourceFilter, String[], int, int, Sorting[])} instead.
	 */
	public Collection getResult(int start, int count, String sortBy, int sortOrder) throws DatasourceNotAvailableException {
		// default implementation: forward this call with default parameters
		return getResult(start, count, sortBy, sortOrder, null);
	}

	/**
	 * 
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(java.lang.String,
	 *      int)
	 * @deprecated use {@link #getResult(DatasourceFilter, String[], int, int, Sorting[])} instead.
	 */
	public Collection getResult(String sortBy, int sortOrder) throws DatasourceNotAvailableException {
		// default implementation: forward this call with default parameters
		return getResult(-1, -1, sortBy, sortOrder, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public int getCount(DatasourceFilter filter) throws DatasourceException {
		return getCount(filter, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public Collection<Resolvable> getResult(DatasourceFilter filter, String[] prefillAttributes) throws DatasourceException {
		// default implementation: forward this call with default parameters
		return getResult(filter, prefillAttributes, -1, -1, null, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      int, int, com.gentics.api.lib.datasource.Datasource.Sorting[])
	 */
	public Collection<Resolvable> getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns) throws DatasourceException {
		// default implementation: forward this call with default parameters
		return getResult(filter, prefillAttributes, start, count, sortedColumns, null);
	}
    
	/**
	 * This default implementation simply always returns true.
	 * Implementors should overwrite this method if it is possible
	 * to determine when data was last changed.
	 * @see com.gentics.api.lib.datasource.Datasource#hasChanged(long)
	 */
	public boolean hasChanged(long timestamp) {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getId()
	 */
	public String getId() {
		return this.id;
	}
    
	/**
	 * Allows to set the id of this datasource.
	 * the preferred way is to use the constructor {@link #AbstractDatasource(String)} tough.
	 * @param id the id for this datasource.
	 * @see #getId()
	 */
	public void setId(String id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount()
	 */
	public int getCount() {
		int result = 0;

		try {
			result = getCount2();
		} catch (DatasourceNotAvailableException e) {
			NodeLogger.getNodeLogger(getClass()).error("Error while getting datasource count", e);
		}
		return result;
	}
    
	/**
	 * This default implementation will always return null because
	 * there is no generic way of determine if an attribute name is valid.
	 * 
	 * @see com.gentics.api.lib.datasource.Datasource#isValidAttribute(java.lang.String)
	 */
	public boolean isValidAttribute(String attributeName) throws DatasourceException {
		return true;
	}
}
