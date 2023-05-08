/*
 * @author norbert
 * @date 15.01.2010
 * @version $Id: FailingHandler.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.cnmappublishhandler;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * @author norbert
 *
 */
public class FailingHandler extends LogHandler {

	/* (non-Javadoc)
	 * @see com.gentics.tests.contentnode.cnmappublishhandler.LogHandler#updateObject(com.gentics.api.lib.resolving.Resolvable)
	 */
	public void updateObject(Resolvable object) throws CnMapPublishException {
		super.updateObject(object);
		throw new CnMapPublishException("We fail!");
	}

	/* (non-Javadoc)
	 * @see com.gentics.tests.contentnode.cnmappublishhandler.LogHandler#createObject(com.gentics.api.lib.resolving.Resolvable)
	 */
	public void createObject(Resolvable object) throws CnMapPublishException {
		super.createObject(object);
		throw new CnMapPublishException("We fail!");
	}

	/* (non-Javadoc)
	 * @see com.gentics.tests.contentnode.cnmappublishhandler.LogHandler#deleteObject(com.gentics.api.lib.resolving.Resolvable)
	 */
	public void deleteObject(Resolvable object) throws CnMapPublishException {
		super.deleteObject(object);
		throw new CnMapPublishException("We fail!");
	}
}
