package com.gentics.contentnode.devtools.model;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConstructModel extends AbstractModel {
	private String keyword;

	private String icon;

	private Map<String, String> name;

	private Map<String, String> description;

	private String hopeditHook;

	private String liveEditorTagName;

	private boolean mayBeSubtag;

	private boolean mayContainsSubtags;

	private boolean autoEnable;

	private boolean newEditor;

	private String externalEditorUrl;

	private List<PartModel> parts;

	private ConstructCategoryModel category;

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Map<String, String> getName() {
		return name;
	}

	public void setName(Map<String, String> name) {
		this.name = name;
	}

	public Map<String, String> getDescription() {
		return description;
	}

	public void setDescription(Map<String, String> description) {
		this.description = description;
	}

	public String getHopeditHook() {
		return hopeditHook;
	}

	public void setHopeditHook(String hopeditHook) {
		this.hopeditHook = hopeditHook;
	}

	public String getLiveEditorTagName() {
		return liveEditorTagName;
	}

	public void setLiveEditorTagName(String liveEditorTagName) {
		this.liveEditorTagName = liveEditorTagName;
	}

	public boolean isMayBeSubtag() {
		return mayBeSubtag;
	}

	public void setMayBeSubtag(boolean mayBeSubtag) {
		this.mayBeSubtag = mayBeSubtag;
	}

	public boolean isMayContainsSubtags() {
		return mayContainsSubtags;
	}

	public void setMayContainsSubtags(boolean mayContainsSubtags) {
		this.mayContainsSubtags = mayContainsSubtags;
	}

	public boolean isAutoEnable() {
		return autoEnable;
	}

	public void setAutoEnable(boolean autoEnable) {
		this.autoEnable = autoEnable;
	}

	public boolean isNewEditor() {
		return newEditor;
	}

	public void setNewEditor(boolean newEditor) {
		this.newEditor = newEditor;
	}

	public String getExternalEditorUrl() {
		return externalEditorUrl;
	}

	public void setExternalEditorUrl(String externalEditorUrl) {
		this.externalEditorUrl = externalEditorUrl;
	}

	public List<PartModel> getParts() {
		return parts;
	}

	public void setParts(List<PartModel> parts) {
		this.parts = parts;
	}

	public ConstructCategoryModel getCategory() {
		return category;
	}

	public void setCategory(ConstructCategoryModel category) {
		this.category = category;
	}
}
