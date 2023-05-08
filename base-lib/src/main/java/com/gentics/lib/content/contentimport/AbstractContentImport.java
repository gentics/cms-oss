/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:44
 * @version $Id: AbstractContentImport.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNWriteableDatasource;

/**
 * This is a abstract base implementation of an importer which provides some 
 * common implementations for listener and parsing. 
 */
public abstract class AbstractContentImport implements GenticsContentImport {

	private int objType;

	private ContentImportParserFactory parserFactory;

	private ContentImportLogger logger;

	private List parsers;

	private List[] listener;

	/**
	 * initialize the import class.
	 * 
	 * @param objType the type of the objects to import. 
	 */
	protected AbstractContentImport(int objType) {
		super();
		this.objType = objType;
		this.parserFactory = null;
		logger = new ContentImportLogger();
		parsers = new ArrayList();
		listener = new ArrayList[4];
		for (int i = 0; i < 4; i++) {
			listener[i] = new ArrayList();
		}
	}

	public int getObjectType() {
		return objType;
	}

	public void setParserFactory(ContentImportParserFactory factory) {
		parserFactory = factory;
	}

	public ContentImportParserFactory getParserFactory() {
		return parserFactory;
	}

	public ContentImportLogger getLogger() {
		return logger;
	}

	public void addListener(int event, ContentImportListener listener) {
		this.listener[event].add(listener);
	}

	public void removeListener(int event, ContentImportListener listener) {
		this.listener[event].remove(listener);
	}

	/**
	 * trigger an event, calling all registered listeners for the event.
	 * @param event the event type to trigger.
	 * @param cnObj the current contentobject to pass to the listener.
	 */
	protected void triggerEvent(int event, GenticsContentObject cnObj) {
		for (Iterator it = listener[event].iterator(); it.hasNext();) {
			ContentImportListener evListener = (ContentImportListener) it.next();

			evListener.onEvent(event, this, cnObj);
		}
	}

	/**
	 * Parse a header row, initialize the import parser using the information in the headers.
	 * @param ds the datasource used to store data.
	 * @param lineNr the current line number
	 * @param line the current line splitted into fields.
	 */
	protected void loadHeader(CNWriteableDatasource ds, int lineNr, String[] line) {

		logger.setLineNr(lineNr);

		Set names = new HashSet(line.length);

		parserFactory.resetImportModules();

		for (int i = 0; i < line.length; i++) {

			String head = line[i];
			ContentImportParser importModule = null;

			importModule = parserFactory.getImportModule(head);
			parsers.add(importModule);
			if (importModule != null) {
				names.add(importModule.getAttributeName());
			} else {
				logger.addWarn(head, "Could not parse header of column " + i + "; ignoring header '" + head + "'.");
			}
		}

		String[] nameList = new String[names.size()];

		names.toArray(nameList);

		ds.setAttributeNames(nameList);

	}

	/**
	 * parse a new data row.
	 * @param ds the datasource used to store data. 
	 * @param lineNr the line nr of the current line.
	 * @param line the current line splitted into fields.
	 * @throws ContentImportException 
	 */
	protected void parseLine(CNWriteableDatasource ds, int lineNr, String[] line) throws ContentImportException {

		logger.setLineNr(lineNr);

		ContentObjectHelper coHelper = parserFactory.getObjectHelper();

		// create new ContentObject
		GenticsContentObject cnObj = null;

		try {
			cnObj = coHelper.createContentObject(objType);
		} catch (CMSUnavailableException e) {
			logger.addError("", "Could not create a new contentobject: " + e.getMessage());
			throw new ContentImportException("Could not create a new contentobject", e);
		} catch (NodeIllegalArgumentException e) {
			logger.addError("", "Could not create a new contentobject: " + e.getMessage());
			throw new ContentImportException("Could not create a new contentobject", e);
		}

		triggerEvent(EVENT_ON_START_ROW, cnObj);

		for (int i = 0; i < line.length; i++) {
			if (i >= parsers.size()) {
				continue;
			}
			ContentImportParser attrParser = (ContentImportParser) parsers.get(i);

			if (attrParser == null) {
				continue;
			}
			try {
				attrParser.parseInto(cnObj, line[i]);
			} catch (ContentImportException e) {
				// if an exception occurs, log it and dismiss the complete line
				logger.addWarn(attrParser.getAttributeName(), "Could not parse attribute value; " + e.getMessage());
				throw new ContentImportException("Could not parse attribute value", e);
			}
		}

		// store object
		try {
			String id = coHelper.storeContentObject(cnObj);

			logger.addImportId(id);
			logger.addInfo("", "Added new object with id '" + id + "'.");
		} catch (SQLException e) {
			logger.addError("", "Could not store the new contentobject: " + e.getMessage());
			throw new ContentImportException("Could not store the new contentobject", e);
		} catch (NodeIllegalArgumentException e) {
			logger.addError("", "Could not store the new contentobject: " + e.getMessage());
			throw new ContentImportException("Could not store the new contentobject", e);
		} catch (CMSUnavailableException e) {
			logger.addError("", "Could not store the new contentobject: " + e.getMessage());
			throw new ContentImportException("Could not store the new contentobject", e);
		} catch (DatasourceException e) {
			logger.addError("", "Could not store the new contentobject: " + e.getMessage());
			throw new ContentImportException("Could not store the new contentobject", e);
		}

		triggerEvent(EVENT_ON_ROW_FINISHED, cnObj);

	}
}
