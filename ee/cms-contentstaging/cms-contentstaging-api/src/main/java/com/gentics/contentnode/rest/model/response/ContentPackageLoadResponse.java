package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;

/**
 * Response of Content Staging package.
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageLoadResponse extends GenericJobStatusResponse {

	private static final long serialVersionUID = 2511529973008918155L;

	private StagedContentPackage contentPackage;

	/**
	 * JAXB constructor
	 */
	public ContentPackageLoadResponse() {
		super();
	}

	/**
	 * Convenience constructor.
	 * 
	 * @param message
	 * @param responseInfo
	 */
	public ContentPackageLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Package
	 * 
	 * @return package
	 */
	public StagedContentPackage getContentPackage() {
		return contentPackage;
	}

	/**
	 * Set content staging package.
	 * 
	 * @param contentPackage
	 */
	public void setContentPackage(StagedContentPackage contentPackage) {
		this.contentPackage = contentPackage;
	}
}
