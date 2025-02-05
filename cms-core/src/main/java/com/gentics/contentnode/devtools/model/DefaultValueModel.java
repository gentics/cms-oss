package com.gentics.contentnode.devtools.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.SelectOption;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultValueModel {
	private String stringValue;

	private List<String> stringValues;

	private Boolean booleanValue;

	private String pageId;

	private String fileId;

	private String imageId;

	private String folderId;

	private String templateId;

	private String nodeId;

	private String contentTagId;

	private String templateTagId;

	private String datasourceId;

	private List<SelectOption> options;

	private OverviewModel overview;

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public List<String> getStringValues() {
		return stringValues;
	}

	public void setStringValues(List<String> stringValues) {
		this.stringValues = stringValues;
	}

	public Boolean getBooleanValue() {
		return booleanValue;
	}

	public void setBooleanValue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
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

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
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

	public String getDatasourceId() {
		return datasourceId;
	}

	public void setDatasourceId(String datasourceId) {
		this.datasourceId = datasourceId;
	}

	public List<SelectOption> getOptions() {
		return options;
	}

	public void setOptions(List<SelectOption> options) {
		this.options = options;
	}

	public OverviewModel getOverview() {
		return overview;
	}

	public void setOverview(OverviewModel overview) {
		this.overview = overview;
	}
}
