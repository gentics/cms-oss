package com.gentics.contentnode.object;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;

/**
 * Model of a construct, which is used for generation of the implementation hash
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedConstructModel extends AbstractModel {
	/**
	 * Keyword
	 */
	private String keyword;

	/**
	 * Parts of the construct
	 */
	private List<StagedPartModel> parts;

	/**
	 * Get the keyword
	 * @return keyword
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Set the keyword
	 * @param keyword keyword
	 * @return fluent API
	 */
	public StagedConstructModel setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	/**
	 * Get the list of parts
	 * @return list of parts
	 */
	public List<StagedPartModel> getParts() {
		return parts;
	}

	/**
	 * Set the list of parts
	 * @param parts list of parts
	 * @return fluent API
	 */
	public StagedConstructModel setParts(List<StagedPartModel> parts) {
		this.parts = parts;
		return this;
	}
}
