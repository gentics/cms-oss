package com.gentics.contentnode.devtools.model;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectTagDefinitionCategoryModel extends AbstractModel {
	private Map<String, String> name;

	private int sortOrder;

	public Map<String, String> getName() {
		return name;
	}

	public void setName(Map<String, String> name) {
		this.name = name;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
