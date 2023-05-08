/*
 * @author stefan.hurjui
 * @date Apr 20, 2005
 * @version $Id: ObjectTypeBean.java,v 1.10 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.ChangeableBean;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.object.jaxb.Attributetype;
import com.gentics.lib.datasource.object.jaxb.Objecttype;
import com.gentics.lib.datasource.object.jaxb.impl.ObjecttypeImpl;

public class ObjectTypeBean extends ChangeableBean {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4226625515472538460L;

	private Integer type;

	private Integer oldType;

	private String name;

	private Map<String, ObjectAttributeBean> attributeTypes = new HashMap<String, ObjectAttributeBean>();

	private boolean excludeVersioning = false;

	public ObjectTypeBean() {}

	public ObjectTypeBean(Integer type, String name, boolean excludeVersioning) {
		super();
		this.type = type;
		this.oldType = type;
		this.name = name;
		this.excludeVersioning = excludeVersioning;
	}

	/**
	 * Create an instance of the ObjectTypeBean with data from the import object
	 * @param importObject imported object
	 */
	public ObjectTypeBean(Objecttype importObject) {
		this(importObject.getId(), importObject.getName(), importObject.isExcludeVersioning());
		// this is a new type
		this.oldType = null;

		// create also all AttributeTypes
		if (importObject.isSetAttributeTypes()) {
			Attributetype[] attributes = importObject.getAttributeTypes();

			for (int i = 0; i < attributes.length; ++i) {
				addAttributeType(new ObjectAttributeBean(attributes[i], type.intValue()));
			}
		}
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the type.
	 */
	public Integer getType() {
		return type;
	}

	/**
	 * @param type The type to set.
	 */
	public void setType(Integer type) {
		this.type = type;
	}

	/**
	 * set the type as string
	 * @param type type as string
	 */
	public void setType(String type) {
		this.type = new Integer(type);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return type != null ? type.toString() : "";
	}

	/**
	 * Get a faked contentid (identifier) property
	 * @return contentid (type)
	 */
	public String getContentid() {
		return toString();
	}

	/**
	 * Get the old type
	 * @return old type
	 */
	public Integer getOldType() {
		return oldType;
	}

	/**
	 * Set a new value for "oldType"
	 * @param oldType old type
	 */
	public void setOldType(String oldType) {
		setOldType(new Integer(oldType));
	}

	/**
	 * set a new value for "oldType"
	 * @param oldType old type
	 */
	public void setOldType(Integer oldType) {
		this.oldType = oldType;
	}

	/**
	 * Get the exportable object representation of this objecttype
	 * @return Objecttype instance
	 */
	public Objecttype getExportObject() {
		Objecttype exportObject = new ObjecttypeImpl();

		exportObject.setId(type);
		exportObject.setName(name);
		exportObject.setExcludeVersioning(excludeVersioning);

		// set all export objects for the attributes
		Attributetype[] exportAttributes = new Attributetype[attributeTypes.size()];
		int count = 0;

		for (ObjectAttributeBean attribute : attributeTypes.values()) {
			exportAttributes[count++] = attribute.getExportObject();
		}
		exportObject.setAttributeTypes(exportAttributes);

		return exportObject;
	}

	/**
	 * Clear all references to attribute types
	 */
	public void clearAttributeTypes() {
		attributeTypes.clear();
	}

	/**
	 * Add the given attributetype to the list of attributetypes, if not already
	 * added and the objecttype matches
	 * @param attributeType attribute type to add
	 */
	public void addAttributeType(ObjectAttributeBean attributeType) {
		if (attributeType.getObjecttype() == type.intValue()) {
			attributeTypes.put(attributeType.getName(), attributeType);
		}
	}

	/**
	 * Get all attributetypes of the objecttype
	 * @return array of attributetypes
	 */
	public ObjectAttributeBean[] getAttributeTypes() {
		return (ObjectAttributeBean[]) attributeTypes.values().toArray(new ObjectAttributeBean[attributeTypes.size()]);
	}
    
	@SuppressWarnings("deprecation")
	protected boolean isLobAttributeType(int attributeType) {
		return attributeType == GenticsContentAttribute.ATTR_TYPE_BLOB || attributeType == GenticsContentAttribute.ATTR_TYPE_BINARY
				|| attributeType == GenticsContentAttribute.ATTR_TYPE_TEXT_LONG;
	}
    
	/**
	 * returns all large object attribute types.
	 */
	public ObjectAttributeBean[] getLobAttributeTypes() {
		return getLobAttributeTypes(true);
	}
    
	/**
	 * returns all NON-large object attribute types.
	 */
	public ObjectAttributeBean[] getNonLobAttributeTypes() {
		return getLobAttributeTypes(false);
	}
    
	/**
	 * Returns either all "large object" - attributes, that do not write into the filesystem or if 'lobs' is false all
	 * EXCEPT these large object types.
	 * @param lobs
	 * @return
	 */
	protected ObjectAttributeBean[] getLobAttributeTypes(boolean lobs) {
		List<ObjectAttributeBean> lobAttributeTypes = new ArrayList<ObjectAttributeBean>(attributeTypes.size());

		for (ObjectAttributeBean type : attributeTypes.values()) {
			if (lobs == (!type.isFilesystem() && isLobAttributeType(type.getAttributetype()))) {
				lobAttributeTypes.add(type);
			}
		}
		return lobAttributeTypes.toArray(new ObjectAttributeBean[lobAttributeTypes.size()]);
	}

	/**
	 * return list of attribute types
	 * @return attribute types
	 */
	public List<ObjectAttributeBean> getAttributeTypesList() {
		// return a copy of attribute types list
		return new Vector<ObjectAttributeBean>(attributeTypes.values());
	}

	/**
	 * Get the modifiable map of attribute types
	 * @return map of attribute types
	 */
	public Map<String, ObjectAttributeBean> getAttributeTypesMap() {
		return attributeTypes;
	}

	/**
	 * Check whether the objecttype shall be excluded from versioning
	 * @return true for exclusion, false if not
	 */
	public boolean isExcludeVersioning() {
		return excludeVersioning;
	}

	/**
	 * Set the versioning exclusion
	 * @param excludeVersioning true for exclusion, false if not
	 */
	public void setExcludeVersioning(boolean excludeVersioning) {
		this.excludeVersioning = excludeVersioning;
	}

	/**
	 * Set the versioning exclusion
	 * @param excludeVersioning "true" for exclusion, "false" if not
	 */
	public void setExcludeVersioning(String excludeVersioning) {
		this.excludeVersioning = ObjectTransformer.getBoolean(excludeVersioning, this.excludeVersioning);
	}

	/**
	 * Check whether the objecttype shall be excluded from versioning
	 * @return true for exclusion, false if not
	 */
	public boolean getExcludeVersioning() {
		return excludeVersioning;
	}
    
	/**
	 * retrieve object type id
	 * @return id
	 */
	public String getId() {
		return type.toString();
	}
    
	/**
	 * get type of bean 
	 */
	public String getBeanType() {
		return "object";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ObjectTypeBean) {
			if (type == null || ((ObjectTypeBean) obj).type == null) {
				return false;
			}
			return ((ObjectTypeBean) obj).type.equals(type);
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return type != null ? type.hashCode() : super.hashCode();
	}

	/**
	 * Set whether this objecttype shall be versioned (reverse attribute of
	 * exclude versioning)
	 * @param versioning true for versioning, false if not
	 */
	public void setVersioning(String versioning) {
		this.excludeVersioning = !ObjectTransformer.getBoolean(versioning, !this.excludeVersioning);
	}

	/**
	 * Check whether the objecttype is versioned
	 * @return true when versioning, false if not
	 */
	public boolean getVersioning() {
		return !excludeVersioning;
	}

	/**
	 * Check whether the objecttype is versioned
	 * @return true when versioning, false if not
	 */
	public boolean isVersioning() {
		return !excludeVersioning;
	}

	/**
	 * Create a copy of this object
	 * @param mutable true to create a mutable copy, false for an immutable one
	 * @return copy of this object
	 */
	public ObjectTypeBean copy(boolean mutable) {
		ObjectTypeBean copy = mutable ? new ObjectTypeBean() : new ImmutableObjectTypeBean();

		copy.type = type;
		copy.oldType = oldType;
		copy.name = name;
		copy.excludeVersioning = excludeVersioning;
		if (attributeTypes != null) {
			copy.attributeTypes = new HashMap<String, ObjectAttributeBean>(attributeTypes.size());
			for (Map.Entry<String, ObjectAttributeBean> entry : attributeTypes.entrySet()) {
				copy.attributeTypes.put(entry.getKey(), entry.getValue().copy(mutable));
			}

			if (!mutable) {
				copy.attributeTypes = Collections.unmodifiableMap(copy.attributeTypes);
			}
		}
		return copy;
	}
}
