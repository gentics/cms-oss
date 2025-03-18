/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: FileCreateRequest.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Request to create a file from a URL
 */
@XmlRootElement
public class FileCreateRequest {
	protected boolean overwriteExisting;

	protected int folderId;

	protected int nodeId;

	protected String name;

	protected String description;

	protected String sourceURL;

	protected String niceURL;

	protected Set<String> alternateURLs;

	protected Map<String, String> properties;

	/**
	 * Create empty instance
	 */
	public FileCreateRequest() {
	}

	/**
	 * True to overwrite existing files with the same name in the folder
	 * @return true to overwrite
	 */
	public boolean isOverwriteExisting() {
		return overwriteExisting;
	}

	/**
	 * Get flag for overwriting existing files
	 * @param overwriteExisting true to overwrite
	 */
	public void setOverwriteExisting(boolean overwriteExisting) {
		this.overwriteExisting = overwriteExisting;
	}

	/**
	 * Target folder ID
	 * @return folder ID
	 */
	public int getFolderId() {
		return folderId;
	}

	/**
	 * Set the target folder ID
	 * @param folderId folder ID
	 */
	public void setFolderId(int folderId) {
		this.folderId = folderId;
	}

	/**
	 * Target node ID for uploading files in channels
	 * @return node ID
	 */
	public int getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID
	 * @param nodeId node ID
	 */
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Name of the file
	 * @return filename
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the filename
	 * @param name filename
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Description of the file
	 * @return file description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the file description
	 * @param description file description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Source URL of the file
	 * @return source URL
	 */
	public String getSourceURL() {
		return sourceURL;
	}

	/**
	 * Set the source URL
	 * @param sourceURL source URL
	 */
	public void setSourceURL(String sourceURL) {
		this.sourceURL = sourceURL;
	}

	/**
	 * Nice URL of the file
	 * @return nice URL
	 */
	public String getNiceURL() {
		if (niceURL == null) {
			niceURL = "";
		}

		return niceURL;
	}

	/**
	 * Set the nice URL
	 * @param niceURL nice URL
	 */
	public void setNiceURL(String niceURL) {
		this.niceURL = niceURL;
	}

	/**
	 * Alternate URLs of the file
	 * @return alternate URLs
	 */
	public Set<String> getAlternateURLs() {
		if (alternateURLs == null) {
			alternateURLs = new HashSet<>();
		}

		return alternateURLs;
	}

	/**
	 * Set the alternate URLs.
	 * @param alternateURLs alternate URLs
	 */
	public void setAlternateURLs(Set<String> alternateURLs) {
		this.alternateURLs = alternateURLs;
	}

	/**
	 * The additional properties of the file
	 * @return additional properties
	 */
	public Map<String, String> getProperties() {
		if (properties == null) {
			properties = new HashMap<>();
		}

		return properties;
	}

	/**
	 * Set the additional properties
	 * @param properties additional properties
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}
