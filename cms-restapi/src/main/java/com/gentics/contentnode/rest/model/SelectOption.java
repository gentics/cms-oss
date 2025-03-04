/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: SelectOption.java,v 1.1 2010-04-28 15:44:30 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SelectOption object representing a datasource value in GCN
 * @author norbert
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class SelectOption implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6593093782761795747L;

	/**
	 * Id of the select option
	 */
	private Integer id;

	/**
	 * Key of the select option
	 */
	private String key;

	/**
	 * Value of the select option
	 */
	private String value;

	/**
	 * Constructor used by JAXB
	 */
	public SelectOption() {}

	/**
	 * ID
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Key
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Value
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param id the id to set
	 * @return fluent API
	 */
	public SelectOption setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * @param key the key to set
	 * @return fluent API
	 */
	public SelectOption setKey(String key) {
		this.key = key;
		return this;
	}

	/**
	 * @param value the value to set
	 * @return fluent API
	 */
	public SelectOption setValue(String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SelectOption) {
			SelectOption other = (SelectOption) obj;
			return Objects.equals(id, other.id) && Objects.equals(key, other.key) && Objects.equals(value, other.value);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, value);
	}
}
