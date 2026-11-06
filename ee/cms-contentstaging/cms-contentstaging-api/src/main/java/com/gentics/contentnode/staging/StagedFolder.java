package com.gentics.contentnode.staging;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST API folder entity for Content Staging.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedFolder extends StagedLocalizableObject {

	public static final String TYPE_NAME = "folder";

	private static final long serialVersionUID = 4211397828533288505L;

	/**
	 * publish directory of the Folder
	 */
	private String publishDir;

	/**
	 * Description of the folder
	 */
	private String description;

	/**
	 * Objecttags in the folder (objecttags)
	 */
	private Map<String, StagedObjectTag> objectTags;

	/**
	 * Translated names
	 */
	private Map<String, String> nameI18n;

	/**
	 * Translated descriptions
	 */
	private Map<String, String> descriptionI18n;

	/**
	 * Translated publish directories
	 */
	private Map<String, String> publishDirI18n;

	/**
	 * IDs of assigned templates
	 */
	private Set<String> templateIds = new TreeSet<>();

	/**
	 * Clone the origin.
	 * 
	 * @param origin
	 */
	@XmlTransient
	@JsonIgnore
    public void clone(StagedFolder origin) {
		super.clone(origin);
		this.publishDir = origin.publishDir;
		this.description = origin.description;
		this.objectTags = new TreeMap<>(origin.objectTags);
		this.nameI18n = new HashMap<>(origin.nameI18n);
		this.descriptionI18n = new HashMap<>(origin.descriptionI18n);
		this.publishDirI18n = new HashMap<>(origin.publishDirI18n);
		this.templateIds = origin.templateIds;
	}

	/**
	 * Publish directory of the folder
	 * @return the publishDir
	 */
	public String getPublishDir() {
		return publishDir;
	}

	/**
	 * Description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param publishDir the pubDir to set
	 */
	public void setPublishDir(String publishDir) {
		this.publishDir = publishDir;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Map of object tags of the folder
	 * @return the tags
	 */
	public Map<String, StagedObjectTag> getObjectTags() {
		return objectTags;
	}

	/**
	 * @param objectTags the tags to set
	 */
	public void setObjectTags(Map<String, StagedObjectTag> objectTags) {
		// the tags shall be stored in a treemap, so that they will be ordered in natural ordering of the keys (names)
		this.objectTags = objectTags != null ? new TreeMap<>(objectTags) : null;
	}

	/**
	 * Map of translated names (keys are the language codes)
	 * @return name map
	 */
	public Map<String, String> getNameI18n() {
		return nameI18n;
	}

	/**
	 * Set translated names
	 * @param nameI18n map of translations
	 */
	public void setNameI18n(Map<String, String> nameI18n) {
		this.nameI18n = nameI18n;
	}

	/**
	 * Map of translated descriptions (keys are the language codes)
	 * @return description map
	 */
	public Map<String, String> getDescriptionI18n() {
		return descriptionI18n;
	}

	/**
	 * Set translated descriptions
	 * @param descriptionI18n map of translations
	 */
	public void setDescriptionI18n(Map<String, String> descriptionI18n) {
		this.descriptionI18n = descriptionI18n;
	}

	/**
	 * Map of translated publish directories (keys are the language codes)
	 * @return publish directory map
	 */
	public Map<String, String> getPublishDirI18n() {
		return publishDirI18n;
	}

	/**
	 * Set translated publish directories
	 * @param publishDirI18n map of translations
	 */
	public void setPublishDirI18n(Map<String, String> publishDirI18n) {
		this.publishDirI18n = publishDirI18n;
	}

	/**
	 * IDs of assigned templates
	 * @return template IDs
	 */
	public Set<String> getTemplateIds() {
		return templateIds;
	}

	/**
	 * Set the IDs of assigned templates
	 * @param templateIds
	 */
	public void setTemplateIds(Set<String> templateIds) {
		this.templateIds = templateIds;
	}

	@Override
	@XmlTransient
	@JsonIgnore
    public String getTypeName() {
		return TYPE_NAME;
	}
}
