/*
 * @author norbert
 * @date 27.07.2006
 * @version $Id: AbstractVersioningDatasource.java,v 1.5 2006-09-25 08:26:26 norbert Exp $
 */
package com.gentics.api.lib.datasource;

import java.util.Collection;
import java.util.Map;

import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;

/**
 * Abstract basic implementation for versioning datasources. Implementors should
 * rather override this class than implement the interface {@link Datasource}
 * directly in order to
 * <ul>
 * <li>Use default implementation</li>
 * <li>Reduce implementation modification effort when the interface might
 * change in future releases.</li>
 * </ul>
 */
public abstract class AbstractVersioningDatasource extends AbstractDatasource implements
		VersioningDatasource {
    
	public AbstractVersioningDatasource(String id) {
		super(id);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      java.util.Map)
	 */
	public int getCount(DatasourceFilter filter, Map specificParameters) throws DatasourceException {
		return getCount(filter, specificParameters, -1);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public int getCount(DatasourceFilter filter) throws DatasourceException {
		return getCount(filter, null, -1);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.VersioningDatasource#getCount(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, int)
	 */
	public int getCount(DatasourceFilter filter, int versionTimestamp) throws DatasourceException {
		return getCount(filter, null, versionTimestamp);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.VersioningDatasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter, int)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int versionTimestamp) throws DatasourceException {
		return getResult(filter, prefillAttributes, -1, -1, null, null, versionTimestamp);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.VersioningDatasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      int, int, com.gentics.api.lib.datasource.Datasource.Sorting[], int)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, int versionTimestamp) throws DatasourceException {
		return getResult(filter, prefillAttributes, start, count, sortedColumns, null, versionTimestamp);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.Datasource#getResult(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter,
	 *      int, int, com.gentics.api.lib.datasource.Datasource.Sorting[],
	 *      java.util.Map)
	 */
	public Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, Map specificParameters) throws DatasourceException {
		return getResult(filter, prefillAttributes, start, count, sortedColumns, specificParameters, -1);
	}
}
