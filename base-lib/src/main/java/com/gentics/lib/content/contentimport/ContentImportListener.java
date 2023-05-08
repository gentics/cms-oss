/*
 * @author Stefan Hepp
 * @date 01.05.2005 00:10
 * @version $Id: ContentImportListener.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import com.gentics.lib.content.GenticsContentObject;

/**
 * A listener implementation can be registered to an imported which is called when 
 * an event occurs.
 */
public interface ContentImportListener {
    
	/**
	 * callback method which is called when an event occurs.
	 * @see GenticsContentImport
	 * @param event the type of the event.
	 * @param importer the importer class which triggered the event.
	 * @param cnObj the current object which is processed or null if not available.
	 */
	void onEvent(int event, GenticsContentImport importer, GenticsContentObject cnObj);
}
