/*
 * @author l.herlt@gentics.com
 * @version $Id: GenticsContentObject.java,v 1.2 2010-09-28 17:01:27 norbert Exp $
 */
package com.gentics.lib.content;

import java.util.List;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.StreamingResolvable;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.datasource.VersionedObject;
import com.gentics.lib.db.DBHandle;

/**
 * interface for GenticsContentObjects
 */
public interface GenticsContentObject extends Changeable, VersionedObject, StreamingResolvable {

	public static final int OBJ_TYPE_PAGE = 10007;

	public static final int OBJ_TYPE_FOLDER = 10002;

	public static final int OBJ_TYPE_FILE = 10008;
    
	public static final int OBJ_TYPE_TEMPLATE = 10006;
    
	public static final int OBJ_TYPE_IMAGE = 10011;

	public static final String STR_OBJ_TYPE_PAGE = "10007";

	public static final String STR_OBJ_TYPE_FOLDER = "10002";

	public static final String STR_OBJ_TYPE_FILE = "10008";

	/**
	 * Get a specific attribute of this ContentObject. Single-Value Attributes
	 * can always be retrieved, but the returned value is either a zero-length
	 * String if AttributeType == ATTR_TYPE_TEXT, a default value (depending on
	 * the CMS) if AttributeType == ATTR_TYPE_INTEGER or null if AttributeType ==
	 * ATTR_TYPE_OBJ if they are not filled with a valid value by the user/CMS
	 * Mulit-Value Attributes return null on the first attempt to retrieve a
	 * value via GenticsContentAttribute::getNextContentObject() resp.
	 * GenticsContentAttribute::getNextValue() if they are not filled with a
	 * valid value by the user/CMS or the user did not select any choices
	 * @param name the name of the attribute which is to be retrieved
	 * @return the attribute or null if this ContentObject does not contain a
	 *         attribute with this name
	 */
	GenticsContentAttribute getAttribute(String name) throws CMSUnavailableException,
				NodeIllegalArgumentException;

	/**
	 * Get the ID of this ContentObject.
	 * @return the id of the object
	 */
	int getObjectId();

	/**
	 * Set the ID of this ContentObject.
	 */
	public void setObjectId(int obj_id);

	/**
	 * Get the type of this ContentObject.
	 * @return one of GenticsContentObject.OBJ_TYPE_*
	 */
	int getObjectType();

	/**
	 * returns the object's id which can be used to load this object. In fact
	 * this is "type.id"
	 * @return this object's identifier "type.id"
	 */
	String getContentId();

	/**
	 * get the update timestamp for the object
	 * @return timestamp of the last update
	 */
	int getUpdateTimestamp();

	/**
	 * get the mother id of the object
	 * @return id of the object's mother
	 */
	int getMotherObjectId();

	/**
	 * get the mother type of the object
	 * @return objecttype of the object's mother
	 */
	int getMotherObjectType();

	/**
	 * get the contentid of the object's mother
	 * @return contentid of the object's mother
	 */
	String getMotherContentId();

	/**
	 * set the mother for this object
	 * @param motherObject
	 */
	void setMotherObject(GenticsContentObject motherObject);

	/**
	 * Set the mother's contentid
	 * @param motherContentId contentid of the mother (may be null or empty)
	 * @throws NodeIllegalArgumentException when the contentid is not valid
	 */
	void setMotherContentId(String motherContentId) throws NodeIllegalArgumentException;

	/**
	 * check whether the contentobject is already persistent in the underlying
	 * data storage
	 * @return true when the contentobject is persistent, false if not
	 */
	boolean exists();

	/**
	 * gets all possible attributes of the loaded objecttype, from
	 * contentattributetype table. use
	 * {@link DatatypeHelper#getComplexDatatype(DBHandle, String)}to get
	 * information about the attribute definition.
	 * @return list of {@GenticsContentAttribute}.
	 */
	List getAttributeDefinitions();

	/**
	 * When setting the values of an attribute, the sortorder should be checked for consistency.
	 * If the sortorder is found to be inconsistent, this method should be called to reflect that fact.
	 * This information is used in the CRSync implementation.
	 * @param name name of the attribute
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	public void setAttributeNeedsSortorderFixed(String name) throws NodeIllegalArgumentException,
				CMSUnavailableException;
    
	/**
	 * set a list of values for an attribute
	 * @param name name of the attribute
	 * @param values array of values to set
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	public void setAttribute(String name, Object[] values) throws NodeIllegalArgumentException,
				CMSUnavailableException;

	/**
	 * set a value for an attribute
	 * @param name name of the attribute
	 * @param value value to be set for the attribute
	 * @throws NodeIllegalArgumentException
	 * @throws CMSUnavailableException
	 */
	public void setAttribute(String name, Object value) throws NodeIllegalArgumentException,
				CMSUnavailableException;

	/**
	 * get all set or read attributes from this object.
	 * @param omitMetaAttributes flag to omit meta attributes (like "contentid", ...)
	 * @return array of attributenames.
	 */
	public String[] getAccessedAttributeNames(boolean omitMetaAttributes);

	/**
	 * Get all set attributes from this object
	 * @return array of attributenames.
	 */
	public String[] getModifiedAttributeNames();

	/**
	 * Get the datasource used to handle this object
	 * @return datasource used to handle this object
	 */
	Datasource getDatasource();

	/**
	 * Set a custom updatetimestamp for the object. The custom updatetimestamp
	 * will be set instead of the current timestamp when storing a modified
	 * object.
	 * @param timestamp timestamp to use or -1 to unset
	 * @throws NodeIllegalArgumentException when the datasource does not support
	 *         setting custom updatetimestamps
	 */
	void setCustomUpdatetimestamp(int timestamp) throws NodeIllegalArgumentException;

	/**
	 * Get the custom updatetimestamp or -1 if non set
	 * @return custom updatetimestamp or -1
	 */
	int getCustomUpdatetimestamp();

	/**
	 * Set the value for a prefetched attribute
	 * @param attributeType attribute type
	 * @param value value of the prefetched attribute
	 */
	void setPrefetchedAttribute(AttributeType attributeType, Object value);
}
