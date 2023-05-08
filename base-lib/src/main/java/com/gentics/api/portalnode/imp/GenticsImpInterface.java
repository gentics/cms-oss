/*
 * @author norbert
 * @date 21.04.2005
 * @version $Id: GenticsImpInterface.java,v 1.2 2006-03-14 15:19:00 stefan Exp $
 * @gentics.sdk
 */
package com.gentics.api.portalnode.imp;

import java.util.Map;

/**
 * interface for small imps that are available in dungeons, modules, templates,
 * ...
 * If the imp stores any private data which is not threadsafe (i.e. can be 
 * modified while using the imp), the com.gentics.api.portalnode.imp.GenticsStatefulImpInterface
 * must be used.
 * 
 * @author norbert
 */
public interface GenticsImpInterface {

	/**
	 * initialize the imp with the parameters defined in the &lt;parameters&gt;
	 * tag of the imp. Init is called before any other parameters are set 
	 * and is called only once when the imp is created, not when the imp is reused.
	 * 
	 * @param impId the configured id of the imp, must be returned by {@link #getImpId()} 
	 * @param parameters configuration of the imp
	 * @throws ImpException if errors occured
	 */
	void init(String impId, Map parameters) throws ImpException;
    
	/**
	 * get the id of the imp by which it has been initialized.
	 * @return the id if this configured imp.
	 */
	String getImpId();
}
