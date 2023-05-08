/*
 * @author alexander
 * @date 05.02.2008
 * @version $Id: CRSyncModifier.java,v 1.2 2008-03-07 12:53:44 norbert Exp $
 */
package com.gentics.api.portalnode.connector;

import com.gentics.api.lib.resolving.Changeable;

/**
 * A CRSyncModifier can be used to alter objects before they are stored in the
 * target repository when using the CRSync. For each stored object, the modify
 * method is called and the altered object is stored in the target repository.
 * @author alexander
 */
public interface CRSyncModifier {

	/**
	 * Modify the passed object before it is stored in the target repository
	 * @param changeable the object to modify
	 */
	public void modify(Changeable changeable);
}
