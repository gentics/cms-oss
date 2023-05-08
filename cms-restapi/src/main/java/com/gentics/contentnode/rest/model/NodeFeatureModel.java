package com.gentics.contentnode.rest.model;

/**
 * Model of a node specific feature
 */
public class NodeFeatureModel {
	private NodeFeature id;

	private String name;

	private String description;

	/**
	 * Feature ID
	 * @return feature ID
	 */
	public NodeFeature getId() {
		return id;
	}

	/**
	 * Set feature ID
	 * @param id ID
	 * @return fluent API
	 */
	public NodeFeatureModel setId(NodeFeature id) {
		this.id = id;
		return this;
	}

	/**
	 * Feature name (translated)
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set feature name
	 * @param name name
	 * @return fluent API
	 */
	public NodeFeatureModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Feature description (translated)
	 * @return feature description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set feature description
	 * @param description description
	 * @return fluent API
	 */
	public NodeFeatureModel setDescription(String description) {
		this.description = description;
		return this;
	}
}
