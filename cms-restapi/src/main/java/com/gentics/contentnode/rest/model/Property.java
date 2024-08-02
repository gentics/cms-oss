/*
 * @author norbert
 * @date 26.04.2010
 * @version $Id: Property.java,v 1.2.6.1 2011-03-08 12:30:15 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Property object, representing a property of a {@link Tag} (a part
 * of a tag in GCN)
 * @author norbert
 */
@XmlRootElement
public class Property implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3988506502786832101L;

	/**
	 * Enumeration of property types
	 */
	@XmlEnum(String.class)
	@XmlType(name = "PropertyType")
	public static enum Type {
		STRING, RICHTEXT, BOOLEAN, FILE, IMAGE, FOLDER, PAGE, TEMPLATETAG, PAGETAG, LIST, ORDEREDLIST, UNORDEREDLIST, SELECT, MULTISELECT, DATASOURCE, OVERVIEW, UNKNOWN, TABLE, LOCALFILE, VELOCITY, BREADCRUMB, NAVIGATION, NODE, FORM, CMSFORM, HANDLEBARS;

		/**
		 * Get the Type from the typeid
		 * @param typeId type id
		 * @return Type
		 */
		public static Type get(int typeId) {
			switch (typeId) {
			case 1: // Text
			case 9: // Text (short)
			case 37: // Text (custom form)
				return STRING;

			case 2: // Text/HTML
			case 3: // HTML
			case 10: // Text/HTML (long)
			case 21: // HTML (long)
			case 26: // Java Editor
			case 27: // DHTML Editor
			case 36: // HTML (custom form)
				return RICHTEXT;

			case 4: // URL (page)
				return PAGE;

			case 6: // URL (image)
				return IMAGE;

			case 8: // URL (file)
			case 38: // URL (file)
				return FILE;

			case 11: // Tag (page)
				return PAGETAG;

			case 13: // Overview
				return OVERVIEW;

			case 15: // List
				return LIST;
			case 16: // List (unordered)
				return UNORDEREDLIST;
			case 17: // List (ordered)
				return ORDEREDLIST;

			case 18: // Select (image-height)
			case 19: // Select (image-width)
			case 24: // Select (class)
				return STRING;

			case 22: // File (localpath)
				return LOCALFILE;

			case 29: // Select (single)
				return SELECT;

			case 30: // Select (multiple)
				return MULTISELECT;

			case 20: // Tag (template)
				return TEMPLATETAG;

			case 25: // URL (folder)
			case 39: // URL (folder)
				return FOLDER;

			case 31: // Checkbox
				return BOOLEAN;

			case 32: // Datasource
				return DATASOURCE;

			case 33: // Velocoty
				return VELOCITY;

			case 34: // Breadcrumb
				return BREADCRUMB;

			case 35: // Navigation
				return NAVIGATION;

			case 40:
				return NODE;

			case 41: // Form
				return FORM;

			case 42: // CMS Form
				return CMSFORM;

			case 43: // Handlebars
				return HANDLEBARS;

			default: // unknown types (e.g. customer specific extensions)
				return UNKNOWN;
			}
		}
	}

	;

	/**
	 * Property type
	 */
	private Type type;

	/**
	 * Property (value) id
	 */
	private Integer id;

	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Part ID
	 */
	private Integer partId;

	/**
	 * String value (when the type is {@link Type#STRING} or {@link Type#RICHTEXT} or {@link Type#PAGE})
	 */
	private String stringValue;

	/**
	 * Boolean value (when the type is {@link Type#BOOLEAN})
	 */
	private Boolean booleanValue;

	/**
	 * File id (when the type is {@link Type#FILE})
	 */
	private Integer fileId;

	/**
	 * Image id (when the type is {@link Type#IMAGE})
	 */
	private Integer imageId;

	/**
	 * Folder id (when the type is {@link Type#FOLDER})
	 */
	private Integer folderId;

	/**
	 * Page id (when the type is {@link Type#PAGE} or {@link Type#PAGETAG} or {@link Type#TEMPLATETAG})
	 */
	private Integer pageId;

	/**
	 * Template id (when the type is {@link Type#TEMPLATETAG}).
	 */
	private Integer templateId;

	/**
	 * ContentTag id (when the type is {@link Type#PAGETAG})
	 */
	private Integer contentTagId;

	/**
	 * TemplateTag id (when the type is {@link Type#TEMPLATETAG} or {@link Type#PAGETAG})
	 */
	private Integer templateTagId;

	/**
	 * Node ID (when the type is {@link Type#NODE}) or channel ID when the type one of [{@link Type#PAGE}, {@link Type#FOLDER}, {@link Type#FILE}, {@link Type#IMAGE}].
	 */
	private Integer nodeId;

	/**
	 * Form id (when the type is {@link Type#FORM}
	 */
	private Integer formId;

	/**
	 * List of Strings (when the type is {@link Type#LIST})
	 */
	private List<String> stringValues;

	/**
	 * Options (when the type is {@link Type#SELECT} or {@link Type#MULTISELECT})
	 */
	private List<SelectOption> options;

	/**
	 * Currently selected Options (when the type is {@link Type#SELECT} or {@link Type#MULTISELECT} or {@link Type#DATASOURCE})
	 */
	private List<SelectOption> selectedOptions;

	/**
	 * The ID of the {@link com.gentics.contentnode.object.Datasource}
	 * that contains the {@link #options} and {@link #selectedOptions}
	 * if type is {@link Type#SELECT} or {@link Type#MULTISELECT}.
	 */
	private Integer datasourceId;
    
	/**
	 * Overview (when the type is {@link Type#OVERVIEW})
	 */
	private Overview overview;

	/**
	 * Constructor of the property object
	 */
	public Property() {}

	/**
	 * Property Type
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * String value
	 * @return the stringValue
	 */
	public String getStringValue() {
		return stringValue;
	}

	/**
	 * Boolean value
	 * @return the booleanValue
	 */
	public Boolean getBooleanValue() {
		return booleanValue;
	}

	/**
	 * File ID
	 * @return the fileId
	 */
	public Integer getFileId() {
		return fileId;
	}

	/**
	 * Image ID
	 * @return the imageId
	 */
	public Integer getImageId() {
		return imageId;
	}

	/**
	 * Folder ID
	 * @return the folderId
	 */
	public Integer getFolderId() {
		return folderId;
	}

	/**
	 * Page ID
	 * @return the pageId
	 */
	public Integer getPageId() {
		return pageId;
	}

	/**
	 * Form ID
	 * @return the formId
	 */
	public Integer getFormId() {
		return formId;
	}

	/**
	 * String values
	 * @return the stringValues
	 */
	public List<String> getStringValues() {
		return stringValues;
	}

	/**
	 * Possible options
	 * @return the options
	 */
	public List<SelectOption> getOptions() {
		return options;
	}

	/**
	 * Selected options
	 * @return the selectedOptions
	 */
	public List<SelectOption> getSelectedOptions() {
		return selectedOptions;
	}

	/**
	 * Datasource ID
	 * @return the ID of the {@link com.gentics.contentnode.object.Datasource}
	 */
	public Integer getDatasourceId() {
		return datasourceId;
	}
    
	/**
	 * Overview
	 * @return the overview
	 */
	public Overview getOverview() {
		return overview;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * @param stringValue the stringValue to set
	 */
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	/**
	 * @param booleanValue the booleanValue to set
	 */
	public void setBooleanValue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	/**
	 * @param fileId the fileId to set
	 */
	public void setFileId(Integer fileId) {
		this.fileId = fileId;
	}

	/**
	 * @param imageId the imageId to set
	 */
	public void setImageId(Integer imageId) {
		this.imageId = imageId;
	}

	/**
	 * @param folderId the folderId to set
	 */
	public void setFolderId(Integer folderId) {
		this.folderId = folderId;
	}

	/**
	 * @param pageId the pageId to set
	 */
	public void setPageId(Integer pageId) {
		this.pageId = pageId;
	}

	/**
	 * @param formId the formId to set
	 */
	public void setFormId(Integer formId) {
		this.formId = formId;
	}

	/**
	 * @param stringValues the stringValues to set
	 */
	public void setStringValues(List<String> stringValues) {
		this.stringValues = stringValues;
	}

	/**
	 * @param options the options to set
	 */
	public void setOptions(List<SelectOption> options) {
		this.options = options;
	}

	/**
	 * @param selectedOptions the selectedOptions to set
	 */
	public void setSelectedOptions(List<SelectOption> selectedOptions) {
		this.selectedOptions = selectedOptions;
	}

	/**
	 * @param the ID of the {@link com.gentics.contentnode.object.Datasource}
	 */
	public void setDatasourceId(Integer datasourceId) {
		this.datasourceId = datasourceId;
	}
    
	/**
	 * @param overview the overview to set
	 */
	public void setOverview(Overview overview) {
		this.overview = overview;
	}

	/**
	 * Template ID
	 * @return the templateId
	 */
	public Integer getTemplateId() {
		return templateId;
	}

	/**
	 * ContentTag ID
	 * @return the contentTagId
	 */
	public Integer getContentTagId() {
		return contentTagId;
	}

	/**
	 * TemplateTag ID
	 * @return the templateTagId
	 */
	public Integer getTemplateTagId() {
		return templateTagId;
	}

	/**
	 * @param templateId the templateId to set
	 */
	public void setTemplateId(Integer templateId) {
		this.templateId = templateId;
	}

	/**
	 * @param contentTagId the contentTagId to set
	 */
	public void setContentTagId(Integer contentTagId) {
		this.contentTagId = contentTagId;
	}

	/**
	 * @param templateTagId the templateTagId to set
	 */
	public void setTemplateTagId(Integer templateTagId) {
		this.templateTagId = templateTagId;
	}

	/**
	 * Get the node ID.
	 * @return The node ID.
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 * @param nodeId The node ID to set.
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the part id
	 * @return part id
	 */
	public Integer getPartId() {
		return partId;
	}

	/**
	 * Set the part id
	 * @param partId part id
	 */
	public void setPartId(Integer partId) {
		this.partId = partId;
	}

	/**
	 * Local ID
	 * @return property id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the property id
	 * @param id id
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Global ID
	 * @return global ID
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId global ID
	 */
	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}
}
