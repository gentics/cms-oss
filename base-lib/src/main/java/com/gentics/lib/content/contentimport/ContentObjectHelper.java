/*
 * @author Stefan Hepp
 * @date 30.04.2005 23:09
 * @version $Id: ContentObjectHelper.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNDatasourceRecordSet;
import com.gentics.lib.datasource.CNDatasourceRow;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.parser.rule.DefaultRuleTree;

/**
 * A helper class to create new objects and store objects into the datasource.
 */
public class ContentObjectHelper {

	private int baseObjectType;

	private CNWriteableDatasource ds;

	/**
	 * create a new object helper.
	 * @param baseObjectType the type of the object which are stored and created.
	 * @param ds the datasource to store.
	 */
	public ContentObjectHelper(int baseObjectType, CNWriteableDatasource ds) {
		this.baseObjectType = baseObjectType;
		this.ds = ds;
	}

	/**
	 * create a new instance which uses a different base type.
	 * @param baseObjectType the base type to use for the new instance.
	 * @return a new instance which uses the given base type.
	 */
	public ContentObjectHelper getInstance(int baseObjectType) {
		return new ContentObjectHelper(baseObjectType, ds);
	}

	/**
	 * create a new content object.
	 * @param objectType the objecttype of the new object.
	 * @return a new object of the given type.
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	public GenticsContentObject createContentObject(int objectType) throws NodeIllegalArgumentException, CMSUnavailableException {
		return GenticsContentFactory.createContentObject(objectType, ds);
	}

	/**
	 * get an existing object by id from the datasource.
	 * @param contentId the id of the object to retrieve.
	 * @return the object, or null if not found.
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	public GenticsContentObject getObjectById(String contentId) throws NodeIllegalArgumentException, CMSUnavailableException {
		return GenticsContentFactory.createContentObject(contentId, ds);
	}

	/**
	 * Store an object using the datasource.
	 * @param cnObj object to update
	 * @return the id of the contentobject
	 * @throws SQLException
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 * @throws DatasourceException
	 */
	public String storeContentObject(GenticsContentObject cnObj) throws SQLException,
				NodeIllegalArgumentException, CMSUnavailableException, DatasourceException {
		// TODO reuse cndatasourcerecordset
		CNDatasourceRecordSet rs = new CNDatasourceRecordSet(ds);

		rs.addRow(new CNDatasourceRow(cnObj));

		// hackedihack!
		String[] names = ds.getAttributeNames();

		ds.setAttributeNames(cnObj.getAccessedAttributeNames(false));

		if (cnObj.exists()) {
			ds.update(rs);
		} else {
			ds.insert(rs);
		}

		ds.setAttributeNames(names);

		return cnObj.getContentId();
	}

	/**
	 * delete a content object from the datasource.
	 * @param cnObj the contentobject to delete.
	 * @throws DatasourceException
	 */
	public void deleteContentObject(GenticsContentObject cnObj) throws DatasourceException {
		CNDatasourceRecordSet rs = new CNDatasourceRecordSet(ds);

		rs.addRow(new CNDatasourceRow(cnObj));

		if (cnObj.exists()) {
			ds.delete(rs);
		}
	}

	/**
	 * Store a foreign object.
	 * @param cnObj the base object.
	 * @param attrName the attribute name of the foreign reference.
	 * @param foreignObjects a list of foreign genticscontentobjects to store.
	 * @return a list of the ids of all newly created contentobjects. 
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 * @throws SQLException
	 * @throws DatasourceException
	 */
	public String[] storeForeignObjects(GenticsContentObject cnObj, String attrName,
			Collection foreignObjects) throws NodeIllegalArgumentException,
				CMSUnavailableException, SQLException, DatasourceException {

		List newIds = new Vector();

		// get foreign link information
		GenticsContentAttribute attr = cnObj.getAttribute(attrName);

		if (attr == null) {
			throw new NodeIllegalArgumentException("No such attribute '" + attrName + "'.");
		}
		if (attr.getAttributeType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
			throw new NodeIllegalArgumentException("Attribute '" + attrName + "' is not a foreign object.");
		}
		String foreignKey = attr.getForeignLinkAttribute();

		String contentId = cnObj.getContentId();

		if (contentId == null || "".equals(contentId)) {
			throw new DatasourceException("Contentobject has no id.");
		}

		boolean exists;

		for (Iterator it = foreignObjects.iterator(); it.hasNext();) {
			Object refObj = it.next();

			if (!(refObj instanceof GenticsContentObject)) {
				continue;
			}

			GenticsContentObject fObj = (GenticsContentObject) refObj;

			fObj.setAttribute(foreignKey, contentId);

			exists = fObj.exists();
			storeContentObject(fObj);
			if (!exists) {
				newIds.add(fObj.getContentId());
			}

		}

		String[] ids = new String[newIds.size()];

		newIds.toArray(ids);
		return ids;
	}

	/**
	 * find an object by an attribute value.
	 * @param objectType the type of the object to find.
	 * @param attrName the name of the attribute to search.
	 * @param value the value of the attribute to search.
	 * @return the first matching object, or null if not found.
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	public GenticsContentObject getObjectByAttribute(int objectType, String attrName,
			String value) throws NodeIllegalArgumentException, CMSUnavailableException {

		// TODO fix hacks

		String rule = "object." + attrName + " == \"" + value.replace('"', '\'') + "\" && object.obj_type == " + objectType;
		RuleTree ruleTree = new DefaultRuleTree();

		try {
			ruleTree.parse(rule);
		} catch (ParserException e) {
			return null;
		}

		ds.setRuleTree(ruleTree);
		Collection rs;

		try {
			rs = ds.getResult();
		} catch (DatasourceNotAvailableException e) {
			throw new CMSUnavailableException("Error while getting result", e);
		}

		GenticsContentObject resObj = null;

		Iterator it = rs.iterator();

		if (it.hasNext()) {
			DatasourceRow row = (DatasourceRow) it.next();
			String cId = row.getString("contentid");

			resObj = GenticsContentFactory.createContentObject(cId, ds);
		}

		return resObj;
	}

	/**
	 * get the base type of the helper.
	 * @return
	 */
	public int getBaseObjectType() {
		return baseObjectType;
	}
}
