package com.gentics.contentnode.staging;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.SelectOption;

/**
 * REST entity for the object property, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedProperty extends StagingBase {

	private static final long serialVersionUID = -1614447774972018436L;

	/**
	 * Property type
	 */
	private Type type;

	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Part ID
	 */
	private String partId;

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
	private String fileId;

	/**
	 * Image id (when the type is {@link Type#IMAGE})
	 */
	private String imageId;

	/**
	 * Folder id (when the type is {@link Type#FOLDER})
	 */
	private String folderId;

	/**
	 * Page id (when the type is {@link Type#PAGE} or {@link Type#PAGETAG} or {@link Type#TEMPLATETAG})
	 */
	private String pageId;

	/**
	 * Template id (when the type is {@link Type#TEMPLATETAG}).
	 */
	private String templateId;

	/**
	 * ContentTag id (when the type is {@link Type#PAGETAG})
	 */
	private String contentTagId;

	/**
	 * TemplateTag id (when the type is {@link Type#TEMPLATETAG} or {@link Type#PAGETAG})
	 */
	private String templateTagId;

	/**
	 * Node ID (when the type is {@link Type#NODE}) or channel ID when the type one of [{@link Type#PAGE}, {@link Type#FOLDER}, {@link Type#FILE}, {@link Type#IMAGE}].
	 */
	private String nodeId;

	/**
	 * Form id (when the type is {@link Type#FORM}
	 */
	private String formId;

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
	 * Reference to the datasource (if any)
	 */
	private StagedReference datasource;

	/**
	 * Overview (when the type is {@link Type#OVERVIEW})
	 */
	private StagedOverview overview;

	/**
	 * Reference hash.
	 */
	private String hash;

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getId() {
		return globalId;
	}

	public void setId(String globalId) {
		this.globalId = globalId;
	}

	public String getPartId() {
		return partId;
	}

	public void setPartId(String partId) {
		this.partId = partId;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public Boolean getBooleanValue() {
		return booleanValue;
	}

	public void setBooleanValue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getContentTagId() {
		return contentTagId;
	}

	public void setContentTagId(String contentTagId) {
		this.contentTagId = contentTagId;
	}

	public String getTemplateTagId() {
		return templateTagId;
	}

	public void setTemplateTagId(String templateTagId) {
		this.templateTagId = templateTagId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getFormId() {
		return formId;
	}

	public void setFormId(String formId) {
		this.formId = formId;
	}

	public List<String> getStringValues() {
		return stringValues;
	}

	public void setStringValues(List<String> stringValues) {
		this.stringValues = stringValues;
	}

	public List<SelectOption> getOptions() {
		return options;
	}

	public void setOptions(List<SelectOption> options) {
		this.options = options;
	}

	public List<SelectOption> getSelectedOptions() {
		return selectedOptions;
	}

	public void setSelectedOptions(List<SelectOption> selectedOptions) {
		this.selectedOptions = selectedOptions;
	}

	public StagedReference getDatasource() {
		return datasource;
	}

	public void setDatasource(StagedReference datasource) {
		this.datasource = datasource;
	}

	public StagedOverview getOverview() {
		return overview;
	}

	public void setOverview(StagedOverview overview) {
		this.overview = overview;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public String getTypeName() {
		return "property";
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}
}
