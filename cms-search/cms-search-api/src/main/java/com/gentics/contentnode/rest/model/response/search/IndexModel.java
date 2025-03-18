package com.gentics.contentnode.rest.model.response.search;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * REST Model of a search index
 */
@XmlRootElement
public class IndexModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private String name;

	private boolean found;

	private boolean settingsValid;

	private boolean mappingValid;

	private int indexed;

	private int objects;

	private int queued;

	private int pending;

	/**
	 * Index name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set name
	 * @param name name
	 * @return fluent API
	 */
	public IndexModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * True when the index exists
	 * @return existence flag
	 */
	public boolean isFound() {
		return found;
	}

	/**
	 * Set existence flag
	 * @param found flag
	 * @return fluent API
	 */
	public IndexModel setFound(boolean found) {
		this.found = found;
		return this;
	}

	/**
	 * True when the index settings are valid
	 * @return settings validity flag
	 */
	public boolean isSettingsValid() {
		return settingsValid;
	}

	/**
	 * Set settings validity flag
	 * @param settingsValid flag
	 * @return fluent API
	 */
	public IndexModel setSettingsValid(boolean settingsValid) {
		this.settingsValid = settingsValid;
		return this;
	}

	/**
	 * True when the index mapping is valid
	 * @return mapping validity flag
	 */
	public boolean isMappingValid() {
		return mappingValid;
	}

	/**
	 * Set mapping validity flag
	 * @param mappingValid flag
	 * @return fluent API
	 */
	public IndexModel setMappingValid(boolean mappingValid) {
		this.mappingValid = mappingValid;
		return this;
	}

	/**
	 * Count of indexed objects
	 * @return index count
	 */
	public int getIndexed() {
		return indexed;
	}

	/**
	 * Set index count
	 * @param indexed count
	 * @return fluent API
	 */
	public IndexModel setIndexed(int indexed) {
		this.indexed = indexed;
		return this;
	}

	/**
	 * Count of objects that must be indexed
	 * @return object count
	 */
	public int getObjects() {
		return objects;
	}

	/**
	 * Set object count
	 * @param objects count
	 * @return fluent API
	 */
	public IndexModel setObjects(int objects) {
		this.objects = objects;
		return this;
	}

	/**
	 * Number of documents queued to be indexed
	 * @return queue size
	 */
	public int getQueued() {
		return queued;
	}

	/**
	 * Set queued documents
	 * @param queued count
	 * @return fluent API
	 */
	public IndexModel setQueued(int queued) {
		this.queued = queued;
		return this;
	}

	/**
	 * Get pending documents
	 * @return pending documents
	 */
	public int getPending() {
		return pending;
	}

	/**
	 * Set pending documents
	 * @param pending documents
	 * @return fluent API
	 */
	public IndexModel setPending(int pending) {
		this.pending = pending;
		return this;
	}
}
