/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:43
 * @version $Id: ContentReferenceParser.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.sql.SQLException;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;

/**
 * An import parser implementation which parses reference attributes. 
 * The referenced object is searched by the foreign attribute name. If it 
 * is not found, the object can be created. The referenced object type is
 * determined by the tagmap.
 */
public class ContentReferenceParser extends AbstractImportParser {

	private String refObjName;

	private int refObjType;

	private ContentObjectHelper coHelper;

	/**
	 * create a new reference parser.
	 * @param logger the logger to use.
	 * @param coHelper the object helper to use to store and create objects.
	 * @param attrName the attribute name of the field.
	 * @param refObjName the attribute name of the referenced object.
	 * @throws ContentImportException
	 */
	public ContentReferenceParser(ContentImportLogger logger, ContentObjectHelper coHelper,
			String attrName, String refObjName) throws ContentImportException {
		super(logger, attrName);
		this.coHelper = coHelper;
		this.refObjName = refObjName;
		refObjType = 0;

		// get type of object
		try {
			GenticsContentAttribute attr = coHelper.createContentObject(coHelper.getBaseObjectType()).getAttribute(attrName);

			if (attr == null) {
				throw new ContentImportException("Attribute does not exist.");
			}
			if (attr.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_OBJ) {
				refObjType = attr.getLinkedObjectType();
			} else {
				throw new ContentImportException("Attribute is not a linked object.");
			}
		} catch (NodeIllegalArgumentException e) {
			throw new ContentImportException("Attribute does not exist.", e);
		} catch (CMSUnavailableException e) {
			throw new ContentImportException("Could not check for attribute.", e);
		}
	}

	public void parseInto(GenticsContentObject cnObj, String value) throws ContentImportException {

		if ("".equals(value)) {
			return;
		}

		if (refObjType == 0) {
			setAttributeValue(cnObj, parseFormat(value));
		} else {
			// get the id of the object by attribute-name
			GenticsContentObject obj = null;

			try {
				obj = coHelper.getObjectByAttribute(refObjType, refObjName, parseFormatAsString(value));
			} catch (NodeIllegalArgumentException e) {
				getLogger().addError(getAttributeName(), "Could not get object for value '" + value + "'; " + e.getMessage());
				return;
			} catch (CMSUnavailableException e) {
				getLogger().addError(getAttributeName(), "Could not get object for value '" + value + "'; " + e.getMessage());
				return;
			}

			if (obj == null) {
				if (hasFormat("create")) {
					try {
						obj = coHelper.createContentObject(refObjType);
						obj.setAttribute(refObjName, parseFormat(value));
						coHelper.storeContentObject(obj);
						getLogger().addInfo(getAttributeName(), "Created referred object '" + obj.getContentId() + "'.");
						getLogger().addImportId(obj.getContentId());
					} catch (NodeIllegalArgumentException e) {
						getLogger().addError(getAttributeName(), "Could not create object for value '" + value + "'");
						return;
					} catch (CMSUnavailableException e) {
						getLogger().addError(getAttributeName(), "Could not create object for value '" + value + "'");
						return;
					} catch (SQLException e) {
						getLogger().addError(getAttributeName(), "Could not create object for value '" + value + "'");
						return;
					} catch (DatasourceException e) {
						getLogger().addError(getAttributeName(), "Could not create object for value '" + value + "'");
						return;
					}
				} else {
					getLogger().addError(getAttributeName(), "Could not find object for value '" + value + "'");
					return;
				}
			}

			setAttributeValue(cnObj, obj.getContentId());
		}
	}
}
