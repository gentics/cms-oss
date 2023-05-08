/*
 * @author stefan
 * @date 17.08.2007
 * @version $Id: NumDatasourceInfo.java,v 1.2 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.util.Collection;

import com.gentics.api.lib.datasource.DatasourceInfo;

/**
 * a datasourceinfo implementation containing only a number of affected rows.  
 */
public class NumDatasourceInfo implements DatasourceInfo {

	final int rows;
	
	public NumDatasourceInfo(int rows) {
		this.rows = rows;
	}
	
	public int getAffectedRecordCount() {
		return rows;
	}

	public Collection getAffectedRecords() {
		return null;
	}

}
