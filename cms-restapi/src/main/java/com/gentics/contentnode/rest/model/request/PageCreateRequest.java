/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: PageCreateRequest.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import java.util.SortedSet;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Page create request
 * @author norbert
 */
@XmlRootElement
public class PageCreateRequest {

	private String folderId;

	private Integer templateId;

	private Integer variantId;

	private Integer variantChannelId;

	private String language;

	private Integer nodeId;

	private String pageName;

	private String description;

	private Integer priority;

	private Integer contentSetId;

	private String fileName;

	private String niceUrl;

	/**
	 * Alternate URLs
	 */
	private SortedSet<String> alternateUrls;

	private boolean forceExtension = false;

	private Boolean failOnDuplicate;

	/**
	 * Constructor for JAXB
	 */
	public PageCreateRequest() {}

	/**
	 * The ID of the folder, where the page shall be created. This may either be
	 * the local or the global folder id.
	 *
	 * @return the folderId
	 */
	public String getFolderId() {
		return folderId;
	}

	/**
	 * Id of the template the created page shall use
	 *
	 * @return the templateId
	 */
	public Integer getTemplateId() {
		return templateId;
	}

	/**
	 * Id of the page, this page shall be a variant of
	 *
	 * @return the page id to create a variant from
	 */
	public Integer getVariantId() {
		return variantId;
	}

	/**
	 * Language of the created page
	 *
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @param folderId the folderId to set
	 * @return fluent API
	 */
	public PageCreateRequest setFolderId(String folderId) {
		this.folderId = folderId;
		return this;
	}

	/**
	 * @param templateId the templateId to set
	 * @return fluent API
	 */
	public PageCreateRequest setTemplateId(Integer templateId) {
		this.templateId = templateId;
		return this;
	}

	/**
	 * @param variantId
	 * @return fluent API
	 */
	public PageCreateRequest setVariantId(Integer variantId) {
		this.variantId = variantId;
		return this;
	}

	/**
	 * @param language the language to set
	 * @return fluent API
	 */
	public PageCreateRequest setLanguage(String language) {
		this.language = language;
		return this;
	}

	/**
	 * Set the node id of the page
	 *
	 * @param nodeId
	 * @return fluent API
	 */
	public PageCreateRequest setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	/**
	 * Id of the node we want to create page in.
	 *
	 * @return the node id of the page
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Name of the page
	 *
	 * @return the page name
	 */
	public String getPageName() {
		return pageName;
	}

	/**
	 * Set the pagename
	 *
	 * @param pageName
	 * @return fluent API
	 */
	public PageCreateRequest setPageName(String pageName) {
		this.pageName = pageName;
		return this;
	}

	/**
	 * Filename of the page
	 *
	 * @return the filename
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Set the filename
	 *
	 * @param fileName
	 * @return fluent API
	 */
	public PageCreateRequest setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	/**
	 * Nice URL of the page
	 * @return Nice URL
	 */
	public String getNiceUrl() {
		return niceUrl;
	}

	/**
	 * Set the nice URL
	 * @param niceUrl nice URL
	 * @return fluent API
	 */
	public PageCreateRequest setNiceUrl(String niceUrl) {
		this.niceUrl = niceUrl;
		return this;
	}

	/**
	 * Alternate URLs (in alphabetical order)
	 * @return sorted alternate URLs
	 */
	public SortedSet<String> getAlternateUrls() {
		return alternateUrls;
	}

	/**
	 * Set the alternate URLs
	 * @param alternateUrls alternate URLs
	 * @return fluent API
	 */
	public PageCreateRequest setAlternateUrls(SortedSet<String> alternateUrls) {
		this.alternateUrls = alternateUrls;
		return this;
	}

	/**
	 * Flag for forcing creating of page with the given filename, even if the extension does not match
	 * the template's extension.
	 * If this flag is false, and the proposed filename does not have the correct extension, the extension
	 * will be appended. If the flag is true, no extension will be appended.
	 *
	 * @return force filename extension
	 */
	public boolean isForceExtension() {
		return forceExtension;
	}

	/**
	 * Set whether the 
	 *
	 * @param forceExtension
	 * @return fluent API
	 */
	public PageCreateRequest setForceExtension(boolean forceExtension) {
		this.forceExtension = forceExtension;
		return this;
	}

	/**
	 * Description of the page
	 *
	 * @return the description of the new page
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description of the page
	 *
	 * @param description
	 * @return fluent API
	 */
	public PageCreateRequest setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Priority of the page
	 *
	 * @return the priority of the new page
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * Set the priority of the page
	 *
	 * @param priority the page priotiry
	 * @return fluent API
	 */
	public PageCreateRequest setPriority(Integer priority) {
		this.priority = priority;
		return this;
	}

	/**
	 * Contentset ID of the page
	 *
	 * @return the contentset id of the new page
	 */
	public Integer getContentSetId() {
		return contentSetId;
	}

	/**
	 * Set the contentSetId of the page
	 *
	 * @param contentSetId
	 * @return fluent API
	 */
	public PageCreateRequest setContentSetId(Integer contentSetId) {
		this.contentSetId = contentSetId;
		return this;
	}

	/**
	 * True if creating the page with a duplicate name will fail. If false
	 * (default) the name will be made unique before saving.
	 *
	 * @return true or false
	 */
	public Boolean getFailOnDuplicate() {
		return failOnDuplicate;
	}

	/**
	 * Set whether creating shall fail on duplicate names
	 *
	 * @param failOnDuplicate true to fail on duplicate names
	 * @return fluent API
	 */
	public PageCreateRequest setFailOnDuplicate(Boolean failOnDuplicate) {
		this.failOnDuplicate = failOnDuplicate;
		return this;
	}

	/**
	 * The channel of the source page of the variant. If a variant is created,
	 * the source page and its translation are taken from this channel. If not
	 * specified, the channel in which the page specified by variantId was
	 * created is used instead.
	 *
	 * @return the channel id of the source page
	 */
	public Integer getVariantChannelId() {
		return variantChannelId;
	}

	/**
	 * Set the source channel for the page to create
	 *
	 * @param variantChannelId
	 * @return fluent API
	 */
	public PageCreateRequest setVariantChannelId(Integer variantChannelId) {
		this.variantChannelId = variantChannelId;
		return this;
	}
}
