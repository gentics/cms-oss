/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: Tag.java,v 1.2.2.1.4.1 2011-03-08 12:30:15 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Tag object representing a contenttag in the GCN
 * @author norbert
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag implements Serializable {

	/**
	 * Possible tag types
	 */
	@XmlType(name = "TagType")
	public static enum Type {
		CONTENTTAG, TEMPLATETAG, OBJECTTAG
	}

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 492174451031083412L;

	/**
	 * Id of the contenttag
	 */
	private Integer id;

	/**
	 * Name of the contenttag
	 */
	private String name;

	/**
	 * Construct id of the contenttag
	 */
	private Integer constructId;

	/**
	 * Construct
	 */
	private Construct construct;

	/**
	 * Flag whether the tag is active or not
	 */
	private Boolean active;

	/**
	 * Properties of the contenttag (representing the values in GCN)
	 */
	private Map<String, Property> properties;

	/**
	 * Type of the tag
	 */
	private Type type;

	/**
	 * Constructor used by JAXB
	 */
	public Tag() {}

	/**
	 * ID
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Name
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Construct ID
	 * @return the constructId
	 */
	public Integer getConstructId() {
		return constructId;
	}

	/**
	 * Embedded construct
	 * @return construct
	 */
	public Construct getConstruct() {
		return construct;
	}

	/**
	 * True when the tag is active
	 * @return the active
	 */
	public Boolean getActive() {
		return active;
	}

	/**
	 * Tag properties
	 * @return the properties
	 */
	public Map<String, Property> getProperties() {
		return properties;
	}

	/**
	 *Tag type
	 * @return tag type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param constructId the constructId to set
	 */
	public void setConstructId(Integer constructId) {
		this.constructId = constructId;
	}

	/**
	 * Set construct
	 * @param construct construct
	 */
	public void setConstruct(Construct construct) {
		this.construct = construct;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Map<String, Property> properties) {
		this.properties = properties;
	}

	/**
	 * Set the tag type
	 * @param type tag type
	 */
	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return String.format("%s %s (%d)", type, name, id);
	};
}
