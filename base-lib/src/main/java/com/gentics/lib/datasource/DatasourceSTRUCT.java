/*
 * DatasourceSTRUCT.java
 *
 * Created on 12. August 2004, 13:23
 * 
 * 
 */

package com.gentics.lib.datasource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Dietmar
 */
public class DatasourceSTRUCT {

	public String ID = null;

	// public String HandleID = null; //TODO remove
	private List<String> handleIDs;

	public String typeID = null;

	// public DatasourceHandleSTRUCT typeRef = null;

	// contains DataSourceHandleSTRUCT objects, key is DataSourceHandleSTRUCT.id
	private Map<String, DatasourceHandleSTRUCT> datasourceHandleSTRUCTs;

	public Map<String, String> parameterMap = new HashMap<String, String>();

	/** Creates a new instance of DatasourceSTRUCT */
	public DatasourceSTRUCT() {
		handleIDs = new LinkedList<String>();
		datasourceHandleSTRUCTs = new HashMap<String, DatasourceHandleSTRUCT>();
	}

	public void addHandleID(String handleID) {

		this.handleIDs.add(handleID);

	}

	public Iterator<String> getHandleIDsIterator() {
		return this.handleIDs.iterator();
	}

	public List<String> getHandleIDsList() {
		return this.handleIDs;
	}

	public void setDataSourceHandleSTRUCT(DatasourceHandleSTRUCT dsHandleStruct) {
		if (dsHandleStruct != null) {
			this.datasourceHandleSTRUCTs.put(dsHandleStruct.ID, dsHandleStruct);
		}
	}

	public DatasourceHandleSTRUCT getDataSourceHandleSTRUCT(String dsHandleStructID) {

		DatasourceHandleSTRUCT retDsHandleStructID = null;

		if (dsHandleStructID != null) {
			if (this.datasourceHandleSTRUCTs.containsKey(dsHandleStructID)) {
				retDsHandleStructID = (DatasourceHandleSTRUCT) this.datasourceHandleSTRUCTs.get(dsHandleStructID);
			}
		}

		return retDsHandleStructID;

	}

}
