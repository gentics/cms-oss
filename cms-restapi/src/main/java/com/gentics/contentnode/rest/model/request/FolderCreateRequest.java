/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: FolderCreateRequest.java,v 1.3 2010-10-19 11:24:56 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request object for a request to create a folder
 * @author norbert
 */
@XmlRootElement
public class FolderCreateRequest {
	
	/**
	 * the folder's mother
	 */
	private String motherId;
    
	/**
	 * folder name
	 */
	private String name;

	/**
	 * Publish directory
	 */
	private String publishDir;

	/**
	 * Description of the folder
	 */
	private String description;

	/**
	 * whether a startpage shall be created
	 */
	private boolean startpage = false;

	/**
	 * Template id of the startpage template
	 */
	private Integer templateId;

	/**
	 * Language of the created startpage
	 */
	private String language;

	/**
	 * When true, creating the folder will fail if a folder with that name already exists in the mother folder.
	 * If false (default), the folder will be created with the name made unique by postfixing it with a number
	 */
	private boolean failOnDuplicate = false;
    
	/**
	 * The id of the node we want to create the folder in.
	 */
	private Integer nodeId;

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
	 * @return the publishDir
	 */
	public String getPublishDir() {
		return publishDir;
	}

	/**
	 * @param publishDir the publishDir to set
	 */
	public void setPublishDir(String publishDir) {
		this.publishDir = publishDir;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the failOnDuplicate
	 */
	public boolean isFailOnDuplicate() {
		return failOnDuplicate;
	}

	/**
	 * @param failOnDuplicate the failOnDuplicate to set
	 */
	public void setFailOnDuplicate(boolean failOnDuplicate) {
		this.failOnDuplicate = failOnDuplicate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMotherId(String motherId) {
		this.motherId = motherId;
	}

	public String getMotherId() {
		return motherId;
	}

	public boolean isStartpage() {
		return startpage;
	}

	public void setStartpage(boolean startpage) {
		this.startpage = startpage;
	}

	public void setTemplateId(Integer templateId) {
		this.templateId = templateId;
	}

	public Integer getTemplateId() {
		return templateId;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Constructor used by JAXB
	 */
	public FolderCreateRequest() {}

	public Integer getNodeId() {
		return nodeId;
	}

	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
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
}
