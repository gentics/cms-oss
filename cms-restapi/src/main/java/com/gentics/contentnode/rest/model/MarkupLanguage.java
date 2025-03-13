/*
 * @author norbert
 * @date 08.02.2011
 * @version $Id: MarkupLanguage.java,v 1.1.2.1 2011-02-08 14:14:40 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Markup language object
 */
@XmlRootElement
public class MarkupLanguage implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4573253898615447348L;

	/**
	 * if of the markuplanguage entry
	 */
	private Integer id;

	/**
	 * name of the markup language
	 */
	private String name;

	/**
	 * extension for the markup language
	 */
	private String extension;

	/**
	 * contenttype of the markup language
	 */
	private String contentType;

	/**
	 * Optional feature
	 */
	private String feature;

	/**
	 * Exclude flag
	 */
	private boolean excludeFromPublishing;

	/**
	 * Empty constructor
	 */
	public MarkupLanguage() {}

	/**
	 * ID of the markup language
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
	 * Extension
	 * @return the extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * ContentType
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Set the ID
	 * @param id the id to set
	 * @return fluent API
	 */
	public MarkupLanguage setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Set the name
	 * @param name the name to set
	 * @return fluent API
	 */
	public MarkupLanguage setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Extension
	 * @param extension the extension to set
	 * @return fluent API
	 */
	public MarkupLanguage setExtension(String extension) {
		this.extension = extension;
		return this;
	}

	/**
	 * Set the content type
	 * @param contentType the contentType to set
	 * @return fluent API
	 */
	public MarkupLanguage setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	/**
	 * Optional feature, this markup language is bound to
	 * @return feature
	 */
	public String getFeature() {
		return feature;
	}

	/**
	 * Set the feature name
	 * @param feature name
	 * @return fluent API
	 */
	public MarkupLanguage setFeature(String feature) {
		this.feature = feature;
		return this;
	}

	/**
	 * Flag, whether pages created with template that use this markup language will generally be excluded from publishing
	 * @return excludeFromPublishing flag
	 */
	public boolean isExcludeFromPublishing() {
		return excludeFromPublishing;
	}

	/**
	 * Set the excludeFromPublishing flag
	 * @param excludeFromPublishing flag
	 * @return fluent API
	 */
	public MarkupLanguage setExcludeFromPublishing(boolean excludeFromPublishing) {
		this.excludeFromPublishing = excludeFromPublishing;
		return this;
	}

	@Override
	public String toString() {
		return String.format("Markup Language '%s'", name);
	}
}
