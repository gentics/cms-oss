/*
 * Created on 16.11.2004
 *
 */
package com.gentics.lib.datasource;

import java.util.Collection;

import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.datasource.DatasourceRecordSet;

/**
 * TODO comment this
 * @author norbert
 */
public class DefaultDatasourceInfo implements DatasourceInfo {
	private DatasourceRecordSet datasourceRecordSet;

	public DefaultDatasourceInfo() {
		datasourceRecordSet = null;
	}
    
	public DefaultDatasourceInfo(DatasourceRecordSet dataSet) {
		datasourceRecordSet = dataSet;
	}
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceInfo#getAffectedRecords()
	 */
	public Collection getAffectedRecords() {
		// TODO Auto-generated method stub
		return datasourceRecordSet;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceInfo#getAffectedRecordCount()
	 */
	public int getAffectedRecordCount() {
		return datasourceRecordSet != null ? datasourceRecordSet.size() : 0;
	}

	/**
	 * @param datasourceRecordSet The datasourceRecordSet to set.
	 */
	public void setDatasourceRecordSet(DatasourceRecordSet datasourceRecordSet) {
		this.datasourceRecordSet = datasourceRecordSet;
	}
}
