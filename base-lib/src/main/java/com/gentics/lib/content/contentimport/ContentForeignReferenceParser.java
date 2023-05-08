/*
 * @author Stefan Hepp
 * @date 29.04.2005 18:06
 * @version $Id: ContentForeignReferenceParser.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentObject;

/**
 * An import parser implementation which stores attribute values as foreign object values.
 */
public class ContentForeignReferenceParser extends AbstractImportParser {

	private ForeignReferenceHelper refHelper;

	private String foreignName;

	private ContentImportParser refParser;

	/**
	 * init the foreign attribute parser.
	 * @param logger the logger to use.
	 * @param refHelper the reference helper to store the foreign object values.
	 * @param attrName the attribute name of the field.
	 * @param foreignName the name of the foreign attribute.
	 * @throws ContentImportException
	 */
	public ContentForeignReferenceParser(ContentImportLogger logger,
			ForeignReferenceHelper refHelper, String attrName, String foreignName) throws ContentImportException {
		super(logger, attrName);
		this.refHelper = refHelper;
		this.foreignName = foreignName;
		refParser = null;
	}

	/**
	 * set the parser used to store the foreign attributes.
	 * If not set, the value will be stored as foreign attribute value by the helper.
	 * @param parser the parser used to store foreign values.
	 */
	public void setReferenceParser(ContentImportParser parser) {
		refParser = parser;
		// set format to child parser
		refParser.setFormat(getFormat());
		super.setFormat("");
	}

	/**
	 * set a format option. The option is passed on to the foreign parser, as
	 * the format applies to the value which is stored in the foreign attribute.
	 */
	public void setFormat(String format) {
		if (refParser != null) {
			refParser.setFormat(format);
		} else {
			super.setFormat(format);
		}
	}

	/**
	 * get the parser of the foreign attribute.
	 * @return the parser of the foreign attribute, or null if not set.
	 */
	public ContentImportParser getReferenceParser() {
		return refParser;
	}

	public void parseInto(GenticsContentObject cnObj, String value) throws ContentImportException {

		if (value == null || "".equals(value)) {
			return;
		}

		if (refParser != null) {

			refHelper.parseAttribute(refParser, value);

		} else {

			try {
				refHelper.setAttribute(foreignName, parseFormat(value));
			} catch (NodeIllegalArgumentException e) {
				throw new ContentImportException("Could not append attribute.", e);
			} catch (CMSUnavailableException e) {
				throw new ContentImportException("Could not connect to db.", e);
			}

		}
	}
}
