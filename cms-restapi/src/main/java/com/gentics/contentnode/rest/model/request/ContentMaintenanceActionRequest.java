package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.ContentMaintenanceAction;
import com.gentics.contentnode.rest.model.ContentMaintenanceType;

/**
 * REST Model of a content maintenance action request
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentMaintenanceActionRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7004147530234190551L;

	protected ContentMaintenanceAction action;

	protected Set<ContentMaintenanceType> types;

	protected Boolean clearPublishCache;

	protected Set<String> attributes;

	protected Set<Integer> nodes;

	protected Set<Integer> contentRepositories;

	protected Integer start;

	protected Integer end;

	/**
	 * Maintenance action
	 * @return action
	 */
	public ContentMaintenanceAction getAction() {
		return action;
	}

	/**
	 * Set the action
	 * @param action action
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setAction(ContentMaintenanceAction action) {
		this.action = action;
		return this;
	}

	/**
	 * Object types for restricting the action
	 * @return type set
	 */
	public Set<ContentMaintenanceType> getTypes() {
		return types;
	}

	/**
	 * Set the type restriction
	 * @param types type set
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setTypes(Set<ContentMaintenanceType> types) {
		this.types = types;
		return this;
	}

	/**
	 * True to clear the publish cache for dirted objects
	 * @return flag
	 */
	public Boolean getClearPublishCache() {
		return clearPublishCache;
	}

	/**
	 * Set clear publish flag
	 * @param clearPublishCache flag
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setClearPublishCache(Boolean clearPublishCache) {
		this.clearPublishCache = clearPublishCache;
		return this;
	}

	/**
	 * Attributes for restricted dirting (only the dirted attributes will be updated)
	 * @return attribute set
	 */
	public Set<String> getAttributes() {
		return attributes;
	}

	/**
	 * Set attribute restriction
	 * @param attributes attribute set
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setAttributes(Set<String> attributes) {
		this.attributes = attributes;
		return this;
	}

	/**
	 * Node IDs for node restriction
	 * @return node ID set
	 */
	public Set<Integer> getNodes() {
		return nodes;
	}

	/**
	 * Set nodes restriction
	 * @param nodes node ID set
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setNodes(Set<Integer> nodes) {
		this.nodes = nodes;
		return this;
	}

	/**
	 * ContentRepository IDs for restriction
	 * @return CR ID set
	 */
	public Set<Integer> getContentRepositories() {
		return contentRepositories;
	}

	/**
	 * Set CR restriction
	 * @param contentRepositories CR ID set
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setContentRepositories(Set<Integer> contentRepositories) {
		this.contentRepositories = contentRepositories;
		return this;
	}

	/**
	 * Start timestamp for cdate restriction
	 * @return timestamp
	 */
	public Integer getStart() {
		return start;
	}

	/**
	 * Set the start timestamp
	 * @param start timestamp
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setStart(Integer start) {
		this.start = start;
		return this;
	}

	/**
	 * End timestamp for cdate restriction
	 * @return timestamp
	 */
	public Integer getEnd() {
		return end;
	}

	/**
	 * Set end timestamp
	 * @param end timestamp
	 * @return fluent API
	 */
	public ContentMaintenanceActionRequest setEnd(Integer end) {
		this.end = end;
		return this;
	}
}
