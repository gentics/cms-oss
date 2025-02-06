package com.gentics.contentnode.staging;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.Tag.Type;

/**
 * REST entity for the tag, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedTag extends StagingBase {

	private static final long serialVersionUID = 3812298425674622879L;

	/**
	 * Name of the contenttag
	 */
	private String name;

	/**
	 * Construct of the contenttag
	 */
	private StagedReference construct;

	/**
	 * Flag whether the tag is active or not
	 */
	private Boolean active;

	/**
	 * Properties of the contenttag (representing the values in GCN)
	 */
	private Map<String, StagedProperty> properties;

	/**
	 * Type of the tag
	 */
	private Type type;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StagedReference getConstruct() {
		return construct;
	}

	public void setConstruct(StagedReference construct) {
		this.construct = construct;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Map<String, StagedProperty> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, StagedProperty> properties) {
		this.properties = properties;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public String getTypeName() {
		return "tag";
	}
}
