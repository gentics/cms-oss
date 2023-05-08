package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST Model of a datasource entry
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasourceEntryModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4803521280051396968L;

	/**
	 * Internal Id
	 */
	private Integer id;

	/**
	 * Global ID
	 */
	private String globalId;

	/**
	 * Datasource Entry ID
	 */
	private Integer dsId;

	/**
	 * Key of the datasource entry
	 */
	private String key;

	/**
	 * Value of the datasource entry
	 */
	private String value;

	/**
	 * Local ID
	 * @return ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the local ID
	 * @param id ID
	 * @return fluent API
	 */
	public DatasourceEntryModel setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Global ID
	 * @return global ID
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global ID
	 * @param globalId ID
	 * @return fluent API
	 */
	public DatasourceEntryModel setGlobalId(String globalId) {
		this.globalId = globalId;
		return this;
	}

	/**
	 * Entry ID
	 * @return entry ID
	 */
	public Integer getDsId() {
		return dsId;
	}

	/**
	 * Set the entry ID
	 * @param dsId entry ID
	 * @return fluent API
	 */
	public DatasourceEntryModel setDsId(Integer dsId) {
		this.dsId = dsId;
		return this;
	}

	/**
	 * Entry key
	 * @return key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the entry key
	 * @param key key
	 * @return fluent API
	 */
	public DatasourceEntryModel setKey(String key) {
		this.key = key;
		return this;
	}

	/**
	 * Entry value
	 * @return value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the entry value
	 * @param value value
	 * @return fluent API
	 */
	public DatasourceEntryModel setValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public String toString() {
		return String.format("dsId: %d, key: %s, value: %s", dsId, key, value);
	}
}
