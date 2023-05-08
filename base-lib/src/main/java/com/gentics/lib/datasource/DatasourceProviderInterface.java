/*
 * Created on 08.03.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gentics.lib.datasource;

import com.gentics.api.lib.datasource.Datasource;

/**
 * @author Dietmar TODO To change the template for this generated type comment
 *         go to Window - Preferences - Java - Code Style - Code Templates
 */
public interface DatasourceProviderInterface {
	public abstract Datasource createDatasource(String datasourceId);
}
