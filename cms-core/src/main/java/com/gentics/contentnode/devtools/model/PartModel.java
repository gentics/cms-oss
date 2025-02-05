package com.gentics.contentnode.devtools.model;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartModel {
	private String globalId;

	private Map<String, String> name;

	private String keyword;

	private boolean editable;

	private int typeId;

	private int order;

	private int mlId;

	private String policy;

	private boolean visible;

	private boolean required;

	private boolean inlineEditable;

	private String htmlClass;

	private boolean hideInEditor;

	private String externalEditorUrl;

	private int regexId;

	public String getGlobalId() {
		return globalId;
	}

	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	public Map<String, String> getName() {
		return name;
	}

	public void setName(Map<String, String> name) {
		this.name = name;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public int getTypeId() {
		return typeId;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getMlId() {
		return mlId;
	}

	public void setMlId(int mlId) {
		this.mlId = mlId;
	}

	public int getRegexId() {
		return regexId;
	}

	public void setRegexId(int regexId) {
		this.regexId = regexId;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isInlineEditable() {
		return inlineEditable;
	}

	public void setInlineEditable(boolean inlineEditable) {
		this.inlineEditable = inlineEditable;
	}

	public String getHtmlClass() {
		return htmlClass;
	}

	public void setHtmlClass(String htmlClass) {
		this.htmlClass = htmlClass;
	}

	public boolean isHideInEditor() {
		return hideInEditor;
	}

	public void setHideInEditor(boolean hideInEditor) {
		this.hideInEditor = hideInEditor;
	}

	public String getExternalEditorUrl() {
		return externalEditorUrl;
	}

	public void setExternalEditorUrl(String externalEditorUrl) {
		this.externalEditorUrl = externalEditorUrl;
	}
}
