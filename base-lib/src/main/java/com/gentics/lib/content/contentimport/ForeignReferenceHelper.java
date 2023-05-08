/*
 * @author Stefan Hepp
 * @date 30.04.2005 19:29
 * @version $Id: ForeignReferenceHelper.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.util.Collection;
import java.util.Vector;

import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;

/**
 * A helper class to store foreign objects which may have more than one imported field.
 */
public class ForeignReferenceHelper implements ContentImportListener {
	private ContentImportLogger logger;

	private ContentObjectHelper coHelper;

	private GenticsContentObject refObject;

	private String attrName;

	private int refObjectType;

	private boolean modified;

	/**
	 * create a new foreign reference helper.
	 * @param logger the logger to use.
	 * @param coHelper the object helper to use to create and store objects.
	 * @param attrName the attribute name of the object.
	 * @throws ContentImportException
	 */
	public ForeignReferenceHelper(ContentImportLogger logger, ContentObjectHelper coHelper,
			String attrName) throws ContentImportException {
		this.attrName = attrName;
		this.refObject = null;
		this.modified = false;
		this.logger = logger;
		this.coHelper = coHelper;

		// get Foreign ref information
		try {
			GenticsContentAttribute attr = coHelper.createContentObject(coHelper.getBaseObjectType()).getAttribute(attrName);

			if (attr == null) {
				throw new ContentImportException("Attribute does not exist.");
			}
			if (attr.getAttributeType() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
				refObjectType = attr.getLinkedObjectType();
			} else {
				throw new ContentImportException("Attribute is not a foreign-linked object.");
			}
		} catch (NodeIllegalArgumentException e) {
			throw new ContentImportException("Attribute does not exist.", e);
		} catch (CMSUnavailableException e) {
			throw new ContentImportException("Could not check for attribute.", e);
		}

	}

	/**
	 * get the object type of the foreign objects.
	 * @return the object type of the foreign objects.
	 */
	public int getReferenceObjectType() {
		return refObjectType;
	}

	/**
	 * register the helper as listener to the importer.
	 * @param importer the importer which is used.
	 */
	public void registerImport(GenticsContentImport importer) {
		importer.addListener(GenticsContentImport.EVENT_ON_START_ROW, this);
		// importer.addListener(GenticsContentImport.EVENT_ON_ROW_FINISHED,
		// this);
	}

	/**
	 * unregister the helper as listener from the importer.
	 * @param importer the importer which is used.
	 */
	public void unregisterImport(GenticsContentImport importer) {
		importer.removeListener(GenticsContentImport.EVENT_ON_START_ROW, this);
		// importer.removeListener(GenticsContentImport.EVENT_ON_ROW_FINISHED,
		// this);
	}

	/**
	 * get the currently used contentobjecthelper.
	 * @return the used contentobjecthelper.
	 */
	public ContentObjectHelper getContentObjectHelper() {
		return coHelper;
	}

	/**
	 * parse a field into a foreign object attribute.
	 * @param parser the parser for the foreign attribute.
	 * @param value the value of the field to store.
	 * @throws ContentImportException
	 */
	public void parseAttribute(ContentImportParser parser, String value) throws ContentImportException {
		parser.parseInto(refObject, value);
		modified = true;
	}

	/**
	 * set a value for a foreign object attribute.
	 * @param attrName the name of the foreign object attribute.
	 * @param value the value to store.
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	public void setAttribute(String attrName, Object value) throws NodeIllegalArgumentException, CMSUnavailableException {
		refObject.setAttribute(attrName, value);
		modified = true;
	}

	public void onEvent(int event, GenticsContentImport importer, GenticsContentObject cnObj) {
		if (event == GenticsContentImport.EVENT_ON_START_ROW) {
			initObject();
		} else if (event == GenticsContentImport.EVENT_ON_ROW_FINISHED) {// does not work this way!
			// storeObject(cnObj);
		}
	}

	/**
	 * nice try, but is done now by the foreignreferenceparser.
	 * TODO remove
	 * @param cnObj
	 */
	private void storeObject(GenticsContentObject cnObj) {
		if (modified && refObject != null) {
			try {
				// GenticsContentAttribute foreignObjects =
				// cnObj.getAttribute(attrName);
				Object foreignObjects = null;

				if (foreignObjects == null) {
					foreignObjects = new Vector();
				} else if (!(foreignObjects instanceof Collection)) {
					logger.addError(attrName, "Could not get the list of foreign objects. (not a list)");
					return;
				}
				((Collection) foreignObjects).add(refObject);
				cnObj.setAttribute(attrName, foreignObjects);
			} catch (NodeIllegalArgumentException e) {
				logger.addError(attrName, "Could not store new foreign object; " + e.getMessage());
			} catch (CMSUnavailableException e) {
				logger.addError(attrName, "Could not store new foreign object; " + e.getMessage());
			}
		}
	}

	/**
	 * get the foreign object.
	 * @return get the foreign, modified object, or null if nothing has been stored.
	 */
	public GenticsContentObject getForeignObject() {
		return modified ? refObject : null;
	}

	/**
	 * create and initialize a new foreign object. 
	 */
	private void initObject() {
		try {
			refObject = coHelper.createContentObject(refObjectType);
		} catch (NodeIllegalArgumentException e) {
			refObject = null;
			logger.addError(attrName, "Could not create new foreign object; " + e.getMessage());
		} catch (CMSUnavailableException e) {
			refObject = null;
			logger.addError(attrName, "Could not create new foreign object; " + e.getMessage());
		}
		modified = false;
	}
}
