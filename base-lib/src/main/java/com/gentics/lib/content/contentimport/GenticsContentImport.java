/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:20
 * @version $Id: GenticsContentImport.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import com.gentics.lib.datasource.CNWriteableDatasource;

/**
 * This is the main import interface, for importer, which import data from
 * any source into a writeable datasource. 
 */
public interface GenticsContentImport {

	/**
	 * occurs before a new data row is processed.
	 */
	final int EVENT_ON_START_ROW = 0;

	/**
	 * occurs after each data row has been processed.
	 */
	final int EVENT_ON_ROW_FINISHED = 1;

	/**
	 * occurs before the first row to import.
	 */
	final int EVENT_ON_START_IMPORT = 2;

	/**
	 * occurs after the last row has been imported.
	 */
	final int EVENT_ON_IMPORT_FINISHED = 3;

	/**
	 * get the object type of the object which will be imported.
	 * @return the type of the objects to create.
	 */
	int getObjectType();

	/**
	 * set the factory which will be used to create importparser.
	 * @param factory the factory to use.
	 */
	void setParserFactory(ContentImportParserFactory factory);

	/**
	 * get the currently used import factory.
	 * @return the current import factory, or null if not set. 
	 */
	ContentImportParserFactory getParserFactory();

	/**
	 * run the import, store the imported objects to the writeable datasource. <br>
	 * TODO make WriteableDatasource out of this.
	 * @param ds the datasource where the imported objects will be stored.
	 */
	void doImport(CNWriteableDatasource ds);

	/**
	 * get the logger which is used to store logs.
	 * @return the current import logger.
	 */
	ContentImportLogger getLogger();

	/**
	 * add a listener which will be called when the given event occurs. More than
	 * one listener can be registered per event. 
	 * 
	 * @param event the type of the event, one of the EVENT_ON_* constants.
	 * @param listener the listener to call on the event.
	 */
	void addListener(int event, ContentImportListener listener);

	/**
	 * remove a previously registered listener from an event.
	 * @param event the event where the listener is registered.
	 * @param listener the listener to remove, must be the same instance which is registered.
	 */
	void removeListener(int event, ContentImportListener listener);
}
