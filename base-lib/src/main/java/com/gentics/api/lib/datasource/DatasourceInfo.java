/*
 * @author haymo
 * @date 26.08.2004
 * @version $Id: DatasourceInfo.java,v 1.2 2006-08-04 15:56:08 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.util.Collection;

/**
 * Interface for datasource manipulation information
 */
public interface DatasourceInfo {

	/**
	 * Returns affected row objects
	 * @return affected rows as DatasourceRecordSet
	 */
	public Collection getAffectedRecords();

	/**
	 * Returns number of affected rows
	 * @return number of affected rows
	 */
	public int getAffectedRecordCount();
}
