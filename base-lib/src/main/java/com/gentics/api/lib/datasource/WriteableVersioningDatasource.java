/*
 * @author norbert
 * @date 23.05.2005
 * @version $Id: WriteableVersioningDatasource.java,v 1.2 2006-08-04 15:56:08 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.util.Collection;

import com.gentics.api.lib.auth.GenticsUser;

/**
 * Interface for writeable datasources that may support versioning
 */
public interface WriteableVersioningDatasource extends WriteableDatasource {

	/**
	 * method to (automatically) update future changes that are due to become
	 * valid
	 * @return true when something was changed, false if not
	 */
	boolean updateDueFutureChanges();
}
