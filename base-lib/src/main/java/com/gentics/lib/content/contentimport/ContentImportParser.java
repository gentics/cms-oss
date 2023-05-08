/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:35
 * @version $Id: ContentImportParser.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import com.gentics.lib.content.GenticsContentObject;

/**
 * The ContentImportParser processes and stores a value into a contentobject.
 */
public interface ContentImportParser {

	/**
	 * get the attribute name of the object where this parser stores its values.
	 * @return the name of the attribute where values are stored.
	 */
	String getAttributeName();

	/**
	 * get the currently set file prefix path.
	 * @return the file prefix path, or null if not set.
	 */
	String getFilePrefix();
    
	/**
	 * set the file prefix path.
	 * @param prefix the file prefix path.
	 */
	void setFilePrefix(String prefix);
    
	/**
	 * parse the value into an attribute and set it to the contentobject.
	 * @param cnObj the contentobject where the value should be stored to.
	 * @param value the value to parse and store.
	 * @throws ContentImportException
	 */
	void parseInto(GenticsContentObject cnObj, String value) throws ContentImportException;

	/**
	 * set a format string, defined by the import configuration (in the header).
	 * @param format a format option string.
	 */
	void setFormat(String format);

	/**
	 * get the currently used logger of the parser, should be set to the main logger.
	 * @return the currently used logger.
	 */
	ContentImportLogger getLogger();
}
