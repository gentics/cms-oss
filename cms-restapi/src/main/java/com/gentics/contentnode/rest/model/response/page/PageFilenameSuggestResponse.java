package com.gentics.contentnode.rest.model.response.page;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.webcohesion.enunciate.metadata.DocumentationExample;

/**
 * Response containing the suggested fileName
 */
@XmlRootElement
public class PageFilenameSuggestResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private String fileName;

	/**
	 * Create instance
	 */
	public PageFilenameSuggestResponse() {
		super(null, ResponseInfo.ok(""));
	}

	/**
	 * Suggested fileName
	 * @return fileName
	 */
	@DocumentationExample("my-new-page.en.html")
	public String getFileName() {
		return fileName;
	}

	/**
	 * Set the suggested fileName
	 * @param fileName fileName
	 * @return fluent API
	 */
	public PageFilenameSuggestResponse setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}
}
