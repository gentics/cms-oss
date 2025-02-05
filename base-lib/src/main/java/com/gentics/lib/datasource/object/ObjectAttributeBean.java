package com.gentics.lib.datasource.object;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.ChangeableBean;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.object.jaxb.Attributetype;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * TODO comment this
 * @author norbert
 *
 */
public class ObjectAttributeBean extends ChangeableBean {
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(ObjectManagementManager.class.getName());

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6287379610185515347L;

	private String name;
	private int attributetype;
	private boolean optimized;
	private String quickname;
	private boolean multivalue;
	private int objecttype;
	private int linkedobjecttype;
	private String foreignlinkattribute;
	private String oldname;
	private String linkedobjecttypetext;
	private String foreignlinkattributerule;

	private boolean excludeVersioning = false;

	/**
	 * Flag to mark whether the attributetype stores its data in the filesystem
	 */
	private boolean filesystem = false;

	/**
	 * Create an empty instance of the attributetype
	 */
	public ObjectAttributeBean() {}

	/**
	 * Create an instance of the attributetype from the imported object
	 * @param importObject import object
	 * @param objectType objecttype of the attributetype
	 */
	public ObjectAttributeBean(Attributetype importObject, int objectType) {
		this(importObject.getName(), importObject.getAttributetype().intValue(), importObject.isOptimized(), importObject.getQuickName(),
				importObject.isMultivalue(), objectType, importObject.getLinkedobjecttype(), null, importObject.getForeignLinkAttribute(),
				importObject.getForeignLinkAttributeRule(), importObject.isExcludeVersioning(), importObject.isFilesystem());
	}

	/**
	 * Create an instance of the attribute type
	 * @param name name of the attribute
	 * @param attributeType type
	 * @param optimized true for optimized
	 * @param quickName name of the quick column for optimized
	 * @param multivalue true for multivalue
	 * @param objectType object type
	 * @param linkedObjectType linked object type (if attributeType is 2)
	 * @param linkedobjecttypetext linked object type as string (if attributeType is 2)
	 * @param foreignLinkAttributeType foreign linked attribute type (if attributeType is 7)
	 * @param foreignLinkAttributeRule foreign linked attribute type rule (if attributeType is 7)
	 * @param excludeVersioning true if excluded from versioning
	 * @param filesystem true if data is stored in filesystem
	 */
	public ObjectAttributeBean(String name, int attributeType, boolean optimized,
			String quickName, boolean multivalue, int objectType, int linkedObjectType, String linkedobjecttypetext,
			String foreignLinkAttributeType, String foreignLinkAttributeRule, boolean excludeVersioning, boolean filesystem) {
		this.name = name;
		this.oldname = name;
		this.attributetype = attributeType;
		this.optimized = optimized;
		this.quickname = quickName;
		this.multivalue = multivalue;
		this.objecttype = objectType;
		this.linkedobjecttype = linkedObjectType;
		this.foreignlinkattribute = foreignLinkAttributeType;
		this.linkedobjecttypetext = linkedobjecttypetext;
		this.foreignlinkattributerule = foreignLinkAttributeRule;
		this.excludeVersioning = excludeVersioning;
		this.filesystem = filesystem;
	}

	/**
	 * @return Returns the attributeType.
	 */
	public int getAttributetype() {
		return attributetype;
	}

	/**
	 * @param attributeType The attributeType to set.
	 */
	public void setAttributetype(int attributeType) {
		this.attributetype = attributeType;
	}

	public void setAttributetype(String attributetype) {
		this.attributetype = Integer.parseInt(attributetype);
	}

	/**
	 * @return Returns the foreignLinkAttributeType.
	 */
	public String getForeignlinkattribute() {
		return foreignlinkattribute != null ? foreignlinkattribute : "";
	}

	/**
	 * @param foreignLinkAttributeType The foreignLinkAttributeType to set.
	 */
	public void setForeignlinkattribute(String foreignLinkAttributeType) {
		this.foreignlinkattribute = foreignLinkAttributeType;
	}

	/**
	 * @return Returns the linkedObjectType.
	 */
	public int getLinkedobjecttype() {
		return linkedobjecttype;
	}

	/**
	 * @param linkedObjectType The linkedObjectType to set.
	 */
	public void setLinkedobjecttype(int linkedObjectType) {
		this.linkedobjecttype = linkedObjectType;
	}

	public void setLinkedobjecttype(String linkedObjectType) {
		this.linkedobjecttype = Integer.parseInt(linkedObjectType);
	}

	/**
	 * @return Returns the multivalue.
	 */
	public boolean getMultivalue() {
		return multivalue || attributetype == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ;
	}

	/**
	 * @param multivalue The multivalue to set.
	 */
	public void setMultivalue(boolean multivalue) {
		this.multivalue = multivalue;
	}

	public void setMultivalue(String multivalue) {
		this.multivalue = Boolean.valueOf(multivalue).booleanValue();
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
	 * @return Returns the objectType.
	 */
	public int getObjecttype() {
		return objecttype;
	}

	/**
	 * @param objectType The objectType to set.
	 */
	public void setObjecttype(int objectType) {
		this.objecttype = objectType;
	}

	public void setObjecttype(String objectType) {
		this.objecttype = Integer.parseInt(objectType);
	}

	/**
	 * @return Returns the optimized.
	 */
	public boolean getOptimized() {
		return optimized;
	}

	/**
	 * @param optimized The optimized to set.
	 */
	public void setOptimized(boolean optimized) {
		this.optimized = optimized;
	}

	public void setOptimized(String optimized) {
		this.optimized = Boolean.valueOf(optimized).booleanValue();
	}

	/**
	 * @return Returns the quickName.
	 */
	public String getQuickname() {
		return optimized ? (StringUtils.isEmpty(quickname) ? constructQuickColumnName(getName()) : quickname) : null;
	}

	/**
	 * @param quickName The quickName to set.
	 */
	public void setQuickname(String quickName) {
		this.quickname = quickName;
	}

	/**
	 * Get the attributetype as string (for display in lists)
	 * @return attribute type as string
	 */
	public String getAttributetypetext() {
		Object type = ObjectManagementManager.attributeTypes.get(Integer.toString(attributetype));

		if (type != null) {
			return type.toString();
		} else {
			return "";
		}
	}

	/**
	 * @return Returns the oldName.
	 */
	public String getOldname() {
		return oldname;
	}

	/**
	 * @param oldName The oldName to set.
	 */
	public void setOldname(String oldName) {
		this.oldname = oldName;
	}

	/**
	 * @return Returns the linkedobjecttypetext.
	 */
	public String getLinkedobjecttypetext() {
		return linkedobjecttypetext != null ? linkedobjecttypetext : "";
	}

	/**
	 * @return Returns the foreignlinkattributerule.
	 */
	public String getForeignlinkattributerule() {
		return foreignlinkattributerule != null ? foreignlinkattributerule : "";
	}

	/**
	 * @param foreignlinkattributerule The foreignlinkattributerule to set.
	 */
	public void setForeignlinkattributerule(String foreignlinkattributerule) {
		this.foreignlinkattributerule = foreignlinkattributerule;
	}

	/**
	 * get a faked contentid (identifier) of the attribute
	 * @return contentid (identifier)
	 */
	public String getContentid() {
		return Integer.toString(objecttype) + "_" + name;
	}

	public boolean equals(Object obj) {
		if (obj instanceof ObjectAttributeBean) {
			return ((ObjectAttributeBean) obj).getContentid().equals(getContentid());            
		}
		return false;
	}
    
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer toString = new StringBuffer();

		toString.append(objecttype).append(".").append(name);
		return toString.toString();
	}

	/**
	 * Get the exportable object representation of this attributetype
	 * @return Attributetype instance
	 */
	public Attributetype getExportObject() {
		Attributetype exportObject = new Attributetype();

		exportObject.setName(name);
		exportObject.setAttributetype(attributetype);
		exportObject.setOptimized(optimized);
		exportObject.setQuickName(quickname);
		exportObject.setMultivalue(multivalue);
		exportObject.setLinkedobjecttype(linkedobjecttype);
		exportObject.setForeignLinkAttribute(foreignlinkattribute);
		exportObject.setForeignLinkAttributeRule(foreignlinkattributerule);
		exportObject.setExcludeVersioning(excludeVersioning);
		exportObject.setFilesystem(filesystem);

		return exportObject;
	}

	/**
	 * @return Returns the excludeVersioning.
	 */
	public boolean isExcludeVersioning() {
		return excludeVersioning;
	}

	/**
	 * @return Returns the excludeVersioning.
	 */
	public boolean getExcludeVersioning() {
		return excludeVersioning;
	}

	/**
	 * @param excludeVersioning The excludeVersioning to set.
	 */
	public void setExcludeVersioning(boolean excludeVersioning) {
		this.excludeVersioning = excludeVersioning;
	}

	/**
	 * @param excludeVersioning The excludeVersioning to set.
	 */
	public void setExcludeVersioning(String excludeVersioning) {
		this.excludeVersioning = ObjectTransformer.getBoolean(excludeVersioning, this.excludeVersioning);
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
	 * Set whether the attributetype stores in the filesystem
	 * @param filesystem true for storing in the filesystem, false for storing in the db
	 */
	public void setFilesystem(boolean filesystem) {
		this.filesystem = filesystem;
	}

	/**
	 * Check whether the attributetype stores in the filesystem
	 * @return true for storing in the filesystem, false if not
	 */
	public boolean isFilesystem() {
		return filesystem;
	}

	/**
	 * Normalize the attribute by resetting unused options to default values
	 */
	public void normalizedAttribute() {
		switch (attributetype) {
		case 1:
		case 3:
		case 4:
		case 5:
		case 6:
			linkedobjecttype = 0;
			linkedobjecttypetext = null;
			foreignlinkattribute = null;
			foreignlinkattributerule = null;
			break;

		case 2:
			foreignlinkattribute = null;
			foreignlinkattributerule = null;
			break;

		case 7:
			multivalue = false;
			break;
		}
		if (attributetype != GenticsContentAttribute.ATTR_TYPE_TEXT_LONG && attributetype != GenticsContentAttribute.ATTR_TYPE_BLOB) {
			filesystem = false;
		}
	}
    
	/**
	 * get ObjectAttribute id (combination of type and name)
	 * @return id
	 */
	public String getId() {
		return objecttype + name;
	}
    
	/**
	 * get type of bean 
	 */
	public String getBeanType() {
		return "attribute";
	}

	/**
	 * Check whether the data of the attribute type is consistent.
	 * Currently this checks, whether the settings for optimized, multivalue and filesystem are set in a proper combination
	 * (optimized may neither be set together with multivalue or filesystem)
	 * @throws if the data is set inconsistent
	 */
	public void checkConsistency() throws ObjectManagementException {
		if (optimized) {
			if (multivalue) {
				throw new ObjectManagementException("Attribute '" + getName() + "' cannot both be optimized and multivalue");
			}
			if (filesystem) {
				throw new ObjectManagementException("Attribute '" + getName() + "' cannot both be optimized and filesystem");
			}
		}
	}

	/**
	 * Get debug string containing all properties of the attribute
	 * @return debug string
	 */
	public String getDebugString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("name: [").append(getName()).append("], ");
		buffer.append("objecttype: [").append(getObjecttype()).append("], ");
		buffer.append("attributetype: [").append(getAttributetype()).append("], ");
		buffer.append("optimized: [").append(getOptimized()).append("], ");
		buffer.append("quickname: [").append(getQuickname()).append("], ");
		buffer.append("multivalue: [").append(getMultivalue()).append("], ");
		buffer.append("exclude_versioning: [").append(getExcludeVersioning()).append("], ");
		buffer.append("linkedobjecttype: [").append(getLinkedobjecttype()).append("], ");
		buffer.append("foreignlinkattribute: [").append(getForeignlinkattribute()).append("], ");
		buffer.append("foreignlinkattributerule: [").append(getForeignlinkattributerule()).append("]");
		return buffer.toString();
	}

	/**
	 * Make a copy of this object
	 * @param mutable true to create a mutable copy, false for an immutable one
	 * @return copy of this object
	 */
	public ObjectAttributeBean copy(boolean mutable) {
		ObjectAttributeBean copy = mutable ? new ObjectAttributeBean() : new ImmutableObjectAttributeBean();

		copy.attributetype = attributetype;
		copy.excludeVersioning = excludeVersioning;
		copy.filesystem = filesystem;
		copy.foreignlinkattribute = foreignlinkattribute;
		copy.foreignlinkattributerule = foreignlinkattributerule;
		copy.linkedobjecttype = linkedobjecttype;
		copy.linkedobjecttypetext = linkedobjecttypetext;
		copy.multivalue = multivalue;
		copy.name = name;
		copy.objecttype = objecttype;
		copy.oldname = oldname;
		copy.optimized = optimized;
		copy.quickname = quickname;
		return copy;
	}

	/**
	 * Construct the quickcolumn's name out of the attributetype's name
	 * @param attributeName attributetype's name
	 * @return name of the quick column
	 */
	public static String constructQuickColumnName(String attributeName) {
		return "quick_" + attributeName.replaceAll("\\.", "_");
	}

	/**
	 * Compare the attribute and return true when the attributes are equal (all properties)
	 * @param originalAttribute original attribute
	 * @param newAttribute new attribute
	 * @return true when the attributes are equal, false if not
	 */
	public static boolean attributesEqual(ObjectAttributeBean originalAttribute,
			ObjectAttributeBean newAttribute) {
		return attributesEqual(originalAttribute, newAttribute, false);
	}

	/**
	 * Compare the attribute and return true when the attributes are equal (all properties)
	 * @param originalAttribute original attribute
	 * @param newAttribute new attribute
	 * @param ignoreOptimized true when the optimized flag shall be ignored, false if not
	 * @return true when the attributes are equal, false if not
	 */
	public static boolean attributesEqual(ObjectAttributeBean originalAttribute,
			ObjectAttributeBean newAttribute, boolean ignoreOptimized) {
		boolean equal = originalAttribute.getObjecttype() == newAttribute.getObjecttype()
				&& originalAttribute.getExcludeVersioning() == newAttribute.getExcludeVersioning()
				&& originalAttribute.getMultivalue() == newAttribute.getMultivalue()
				&& (ignoreOptimized || originalAttribute.getOptimized() == newAttribute.getOptimized())
				&& originalAttribute.getAttributetype() == newAttribute.getAttributetype()
				&& StringUtils.isEqual(originalAttribute.getForeignlinkattribute(), newAttribute.getForeignlinkattribute())
				&& StringUtils.isEqual(originalAttribute.getForeignlinkattributerule(), newAttribute.getForeignlinkattributerule())
				&& originalAttribute.getLinkedobjecttype() == newAttribute.getLinkedobjecttype()
				&& StringUtils.isEqual(originalAttribute.getName(), newAttribute.getName())
				&& (ignoreOptimized
						|| StringUtils.isEqual(originalAttribute.getQuickname(), newAttribute.getQuickname())
								&& originalAttribute.isFilesystem() == newAttribute.isFilesystem());

		if (logger.isDebugEnabled()) {
			if (equal) {
				logger.debug("Found no differences for " + originalAttribute.getName());
			} else {
				logger.debug(
						"Found differences for " + originalAttribute.getName() + "\n " + originalAttribute.getDebugString() + "\n " + newAttribute.getDebugString());
			}
		}
		return equal;
	}
}
