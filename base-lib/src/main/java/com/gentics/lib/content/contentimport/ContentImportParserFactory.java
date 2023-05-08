/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:34
 * @version $Id: ContentImportParserFactory.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

/**
 * An interface for factories which create new import parser and objecthelper to create new objects.
 */
public interface ContentImportParserFactory {

	/**
	 * reset the factory before a new set of headers is processed. This is needed 
	 * to reset any combined fields like foreign refs with more than one field. 
	 */
	void resetImportModules();

	/**
	 * get an import parser for a given header.
	 * @param header the header containing the format and attribute name of the column.
	 * @return an configured import parser, or null if the header is empty or invalid.
	 */
	ContentImportParser getImportModule(String header);

	/**
	 * get an object helper which can be used to create new content objects.
	 * @return an object helper.
	 */
	ContentObjectHelper getObjectHelper();

}
