/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:39
 * @version $Id: ContentAttributeParser.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;

/**
 * A parser implementation which processes fields as simple field values, and uses the 
 * AbstractImportParser to format values before storing them.
 */
public class ContentAttributeParser extends AbstractImportParser {

	/**
	 * init the attribute parser.
	 * @param logger the logger to use.
	 * @param coHelper the helper class to use to create objects and attributes.
	 * @param attrName the name of the attribute where values should be stored.
	 * @throws ContentImportException
	 */
	public ContentAttributeParser(ContentImportLogger logger, ContentObjectHelper coHelper,
			String attrName) throws ContentImportException {
		super(logger, attrName);

		try {
			GenticsContentAttribute attr = coHelper.createContentObject(coHelper.getBaseObjectType()).getAttribute(attrName);

			if (attr == null) {
				throw new ContentImportException("Attribute does not exist.");
			}
		} catch (NodeIllegalArgumentException e) {
			throw new ContentImportException("Attribute does not exist.", e);
		} catch (CMSUnavailableException e) {
			throw new ContentImportException("Could not check for attribute.", e);
		}
	}

	public void parseInto(GenticsContentObject cnObj, String value) throws ContentImportException {
		setAttributeValue(cnObj, parseFormat(value));
	}
}
