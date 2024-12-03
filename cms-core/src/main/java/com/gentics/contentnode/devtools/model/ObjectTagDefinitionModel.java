package com.gentics.contentnode.devtools.model;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectTagDefinitionModel extends AbstractModel {
	private ObjectTagDefinitionTypeModel type;

	private String keyword;

	private Map<String, String> name;

	private Map<String, String> description;

	private String constructId;

	private Boolean required;

	private Boolean inheritable;

	private Boolean syncContentset;

	private Boolean syncChannelset;

	private Boolean syncVariants;

	private Boolean restricted;

	private ObjectTagDefinitionCategoryModel category;

	private List<String> nodeIds;

	public ObjectTagDefinitionTypeModel getType() {
		return type;
	}

	public void setType(ObjectTagDefinitionTypeModel type) {
		this.type = type;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
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

	public String getConstructId() {
		return constructId;
	}

	public void setConstructId(String constructId) {
		this.constructId = constructId;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public Boolean getInheritable() {
		return inheritable;
	}

	public void setInheritable(Boolean inheritable) {
		this.inheritable = inheritable;
	}

	public Boolean getSyncContentset() {
		return syncContentset;
	}

	public void setSyncContentset(Boolean syncContentset) {
		this.syncContentset = syncContentset;
	}

	public Boolean getSyncChannelset() {
		return syncChannelset;
	}

	public void setSyncChannelset(Boolean syncChannelset) {
		this.syncChannelset = syncChannelset;
	}

	public Boolean getSyncVariants() {
		return syncVariants;
	}

	public void setSyncVariants(Boolean syncVariants) {
		this.syncVariants = syncVariants;
	}

	public Boolean getRestricted() {
		return restricted;
	}

	public void setRestricted(Boolean restricted) {
		this.restricted = restricted;
	}

	public ObjectTagDefinitionCategoryModel getCategory() {
		return category;
	}

	public void setCategory(ObjectTagDefinitionCategoryModel category) {
		this.category = category;
	}

	public List<String> getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(List<String> nodeIds) {
		this.nodeIds = nodeIds;
	}
}
