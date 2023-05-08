/*
 * @author raoul
 * @date 28.07.2004
 * @version $Id: DatasourceRecordSet.java,v 1.1 2006-01-13 15:25:41 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.util.List;

/**
 * Interface for DatasourceRecordSet that hold DatasourceRows retrieved from a Datasource
 */
public interface DatasourceRecordSet extends List {

	/**
	 * Get the DatasourceRow with the given index
	 * @param rowNum starting with 0
	 * @return the DatasourceRow or null
	 */
	public DatasourceRow getRow(int rowNum);

	/**
	 * Add a DatasourceRow at the end of the DatasourceRecordSet
	 * @param dsRow DatasourceRow to be added
	 */
	public void addRow(DatasourceRow dsRow);
}
