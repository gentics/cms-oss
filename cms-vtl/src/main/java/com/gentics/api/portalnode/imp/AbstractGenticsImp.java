/*
 * @author stefan
 * @date Mar 8, 2006
 * @version $Id$
 */
package com.gentics.api.portalnode.imp;

import java.util.Map;

import com.gentics.lib.log.NodeLogger;

/**
 * Abstract implementation for imps.
 * This abstract class implements some common functions for imps.
 */
public abstract class AbstractGenticsImp implements GenticsImpInterface {

	private String impId;

	/**
	 * logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	protected AbstractGenticsImp() {}
    
	public String getImpId() {
		return impId;
	}

	public void init(String impId, Map parameters) throws ImpException {
		this.impId = impId;
	}
    
}
