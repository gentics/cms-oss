/*
 * @author Stefan Hepp
 * @date 30.04.2005 20:20
 * @version $Id: ContentImportLogger.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A logger to store errors, infos and imported object ids.
 */
public class ContentImportLogger {

	public static final int LOG_ERROR = 1;

	public static final int LOG_WARN = 2;

	public static final int LOG_INFO = 3;

	public static final int LOG_DEBUG = 4;

	private List errors;

	private List importIds;

	private int lineNr;

	/**
	 * create a new logger.
	 */
	public ContentImportLogger() {
		errors = new ArrayList();
		importIds = new ArrayList();
		lineNr = 0;
	}

	/**
	 * get the current line number.
	 * @return the current line number.
	 */
	public int getLineNr() {
		return lineNr;
	}

	/**
	 * set the current line number.
	 * @param lineNr the current line number.
	 */
	public void setLineNr(int lineNr) {
		this.lineNr = lineNr;
	}

	/**
	 * clear all errors.
	 */
	public void clearErrors() {
		errors.clear();
	}

	/**
	 * get a list of all error messages up to a given level.
	 * @param level the max level of messages to return.
	 * @return a list of all matching messages.
	 */
	public ContentImportError[] getMessages(int level) {
		Collection errors = new Vector();

		for (Iterator it = this.errors.iterator(); it.hasNext();) {
			ContentImportError error = (ContentImportError) it.next();

			if (error.getLevel() <= level) {
				errors.add(error);
			}
		}

		ContentImportError[] rs = new ContentImportError[errors.size()];

		errors.toArray(rs);

		return rs;
	}

	/**
	 * get a list of all imported object ids.
	 * @return a list of all newly created object ids.
	 */
	public String[] getObjectImportIds() {
		String[] ids = new String[importIds.size()];

		importIds.toArray(ids);
		return ids;
	}

	/**
	 * add a new error.
	 * @param attrName the current attribute, or an empty string if unknown.
	 * @param msg the error message.
	 */
	public void addError(String attrName, String msg) {
		addDebugMsg(LOG_ERROR, attrName, msg);
	}

	/**
	 * add a new warning.
	 * @param attrName the current attribute, or an empty string if unknown.
	 * @param msg the warning message.
	 */
	public void addWarn(String attrName, String msg) {
		addDebugMsg(LOG_WARN, attrName, msg);
	}

	/**
	 * add a new info message.
	 * @param attrName the current attribute, or an empty string if unknown.
	 * @param msg the info message.
	 */
	public void addInfo(String attrName, String msg) {
		addDebugMsg(LOG_INFO, attrName, msg);
	}

	/**
	 * add a new debug message.
	 * @param attrName the current attribute, or an empty string if unknown.
	 * @param msg the debug message.
	 */
	public void addDebug(String attrName, String msg) {
		addDebugMsg(LOG_DEBUG, attrName, msg);
	}

	/**
	 * add a new message.
	 * @param level the level of the message.
	 * @param attrName the current attribute, or an empty string if unknown.
	 * @param msg the message to add.
	 */
	public void addDebugMsg(int level, String attrName, String msg) {
		addDebugMsg(level, lineNr, attrName, msg);
	}

	/**
	 * add a new message with a line number.
	 * @param level the level of the message.
	 * @param line the number of the line which produced the error.
	 * @param attrName the current attribute, or an empty string if unknown.
	 * @param msg the message.
	 */
	public void addDebugMsg(int level, int line, String attrName, String msg) {
		errors.add(new ContentImportError(level, attrName, line, msg));
	}

	/**
	 * log the id of a newly created object. This list should only contain 
	 * ids of objects which have been created during the import.
	 * @param id the id of an imported object.
	 */
	public void addImportId(String id) {
		importIds.add(id);
	}
}
