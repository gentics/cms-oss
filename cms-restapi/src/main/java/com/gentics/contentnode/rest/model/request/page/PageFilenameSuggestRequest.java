package com.gentics.contentnode.rest.model.request.page;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.webcohesion.enunciate.metadata.DocumentationExample;

/**
 * Request object for suggesting a filename
 */
@XmlRootElement
public class PageFilenameSuggestRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private int folderId;

	private Integer nodeId;

	private int templateId;

	private String language;

	private String pageName;

	private String fileName;

	/**
	 * ID of the folder of the page
	 * @return folder ID
	 */
	@DocumentationExample("25")
	public int getFolderId() {
		return folderId;
	}

	/**
	 * Set the folder ID
	 * @param folderId folder ID
	 */
	public void setFolderId(int folderId) {
		this.folderId = folderId;
	}

	/**
	 * ID of the node/channel of the folder
	 * @return node ID
	 */
	@DocumentationExample("1")
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * ID of the page's template
	 * @return template ID
	 */
	@DocumentationExample("2")
	public int getTemplateId() {
		return templateId;
	}

	/**
	 * Set the template ID
	 * @param templateId template ID
	 */
	public void setTemplateId(int templateId) {
		this.templateId = templateId;
	}

	/**
	 * Optional language code
	 * @return language code
	 */
	@DocumentationExample("en")
	public String getLanguage() {
		return language;
	}

	/**
	 * Set language code
	 * @param language code
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Optional page name. If no fileName is given, the suggested fileName will be constructed from the pageName.
	 * @return page name
	 */
	@DocumentationExample("My new page")
	public String getPageName() {
		return pageName;
	}

	/**
	 * Set page name
	 * @param pageName page name
	 */
	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	/**
	 * Optional fileName. If given, the suggested fileName will be a sanitized version of the given fileName.
	 * @return filename
	 */
	@DocumentationExample(value = "")
	public String getFileName() {
		return fileName;
	}

	/**
	 * Set the filename
	 * @param fileName filename
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
