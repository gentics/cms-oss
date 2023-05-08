/** Java class "GenticsContentAttribute.java" generated from Poseidon for UML.
 *  Poseidon for UML is developed by <A HREF="http://www.gentleware.com">Gentleware</A>.
 *  Generated with <A HREF="http://jakarta.apache.org/velocity/">velocity</A> template engine.
 */
package com.gentics.lib.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;

/**
 * @author l.herlt@gentics.com
 */
public interface GenticsContentAttribute {
	static final int ATTR_TYPE_UNKOWN = 0;

	static final int ATTR_TYPE_TEXT = 1;

	static final int ATTR_TYPE_OBJ = 2;

	static final int ATTR_TYPE_INTEGER = 3;

	/**
	 * @deprecated use ATTR_TYPE_BLOB or ATTR_TYPE_TEXT_LONG instead
	 */
	static final int ATTR_TYPE_BINARY = 4;

	static final int ATTR_TYPE_TEXT_LONG = 5;

	static final int ATTR_TYPE_BLOB = 6;

	static final int ATTR_TYPE_ALL = 98;

	static final int ATTR_TYPE_ALL_TEXT = 99;

	/**
	 * attribute type for foreign linked objects
	 */
	static final int ATTR_TYPE_FOREIGNOBJ = 7;

	/**
	 * attribute type for long integers
	 */
	static final int ATTR_TYPE_LONG = 8;

	/**
	 * attribute type for floating point numbers, double precision
	 */
	static final int ATTR_TYPE_DOUBLE = 9;

	/**
	 * attribute type for dates
	 */
	static final int ATTR_TYPE_DATE = 10;

	// ALL
	static final String ATTR_OBJECT_TYPE = "obj_type";

	static final String ATTR_EDIT_TIMESTAMP = "edittimestamp";

	static final String ATTR_EDITOR = "editor";

	static final String ATTR_EDITOR_EMAIL = "editoremail";

	static final String ATTR_CREATOR = "creator";

	static final String ATTR_CREATOR_EMAIL = "creatoremail";

	static final String ATTR_CREATE_TIMESTAMP = "createtimestamp";

	static final String ATTR_DESCRIPTION = "description";

	static final String ATTR_CONTENT_ID = "contentid";

	static final String ATTR_FOLDER_ID = "folder_id";

	static final String ATTR_NAME = "name";

	// T_PAGE
	static final String ATTR_PAGE_PUBLISH_TIMESTAMP = "publishtimestamp";

	static final String ATTR_PAGE_PUBLISHER = "publisher";

	static final String ATTR_PAGE_PUBLISHER_EMAIL = "publisheremail";

	static final String ATTR_PAGE_CONTENT = "content";

	static final String ATTR_PAGE_PRIORITY = "priority";

	// T_FILE
	static final String ATTR_FILE_URL = "url";

	static final String ATTR_FILE_CONTENT = "content";

	// T_FOLDER

	/**
	 * Get the type of this attribute.
	 * @return one of GenticsContentAttribute.ATTR_TYPE_*
	 */
	int getAttributeType();

	/**
	 * Get the real attribute type (without any transformations)
	 * @return the real attribute type
	 */
	int getRealAttributeType();

	boolean isMultivalue();

	String getAttributeName();

	GenticsContentObject getParent();

	/**
	 * get the type of the linked object, when the attribute is an object link
	 * or 0
	 * @return type of the linked object
	 */
	int getLinkedObjectType();

	/**
	 * get the name of the foreign link attribute, when the attribute is a
	 * foreign link, or null if not
	 * @return name of the foreign link attribute
	 */
	String getForeignLinkAttribute();

	/**
	 * if getAttributeType() == ATTR_TYPE_TEXT, this method returns the first of
	 * multiple attribute values. if there are no (more) values, this method
	 * returns null
	 * @return attribute values
	 */
	String getNextValue() throws CMSUnavailableException;

	Object getNextObjectValue() throws CMSUnavailableException;

	byte[] getNextBinaryValue() throws CMSUnavailableException;

	/**
	 * if getAttributeType() == ATTR_TYPE_OBJ, this method returns the first of
	 * multiple attribute objects. if there are no (more) values, this method
	 * returns null
	 * @return GenticsContentObjects
	 */
	GenticsContentObject getNextContentObject() throws CMSUnavailableException;

	/**
	 * @return Iterator over String
	 */
	Iterator valueIterator();

	/**
	 * @return Iterator over byte[]
	 */
	Iterator binaryIterator();

	/**
	 * @return Iterator over GenticsContentObject
	 */
	Iterator objectIterator();

	/**
	 * @return the amount of values that this attribute will return. this is
	 *         always 1 for non-multivalues, and >= 0 for multivalues
	 */
	int countValues();

	/**
	 * reset the internal iterator (such that at the next invocation of any
	 * getNext** method, the first object will be returned)
	 */
	void resetIterator();

	/**
	 * check whether the attribute is set (has some values) or not
	 * @return true when the attribute is set, false if not
	 */
	boolean isSet();

	/**
	 * get all values of the attribute as (unmodifiable) list
	 * @return all values as list
	 */
	List getValues();

	/**
	 * Get all values of the attribute as list of {@link FilesystemAttributeValue},
	 * if the attribute stores its values in the filesystem
	 * @return list of filesystem attribute values
	 * @throws DatasourceException if the attribute does not store in the filesystem
	 */
	List<FilesystemAttributeValue> getFSValues() throws DatasourceException;

	/**
	 * Check whether this attribute equals the given attribute. This includes a comparison of the values.
	 * @param attribute attribute to compare with
	 * @return true when the attributes are equal, false if not
	 */
	boolean equals(GenticsContentAttribute attribute);

	/**
	 * Checks whether the attribute is cacheable or not
	 * @return true when the attribute is cacheable, false if not
	 */
	boolean isCacheable();

	/**
	 * Return true if the attribute has the value stored in the filesystem
	 * @return true if the value is stored in the filesystem, false if not
	 */
	boolean isFilesystem();

	/**
	 * Normalize the given value (i.e. prepare the value to be stored into the
	 * database for this attribute)
	 * @param value given value
	 * @return normalized value
	 * @throws NodeIllegalArgumentException when the value cannot be normalized
	 */
	Object normalizeValue(Object value) throws NodeIllegalArgumentException;

	/**
	 * Check whether the sortorder for the multivalue attribute needs to be fixed
	 * @return true when the sortorder needs to be fixed, false if not
	 */
	boolean needsSortorderFixed();
    
	/**
	 * This function must be called to specify whether the sortorder needs to be
	 * fixed (e.g. if it's inconsistent in the database.)
	 * This information is used in the CRSync implementation.
	 */
	void setNeedsSortorderFixed();

	/**
	 * Get the n<sup>th</sup> value of this attribute as InputStream, if the attribute is stored in the filesystem and
	 * and 0 &lt;= n &lt; number of values
	 * @param n index of the value (starting with 0)
	 * @return InputStream
	 * @throws IOException if the attribute is not stored in the filesystem
	 * @throws ArrayIndexOutOfBoundsException if n is out of bounds
	 */
	InputStream getInputStream(int n) throws IOException,
				ArrayIndexOutOfBoundsException;
}
