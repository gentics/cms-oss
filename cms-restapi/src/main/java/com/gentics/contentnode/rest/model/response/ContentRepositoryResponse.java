package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentRepositoryModel;

/**
 * Response containing a ContentRepository
 */
@XmlRootElement
public class ContentRepositoryResponse extends GenericResponse {
	private ContentRepositoryModel contentRepository;

	/**
	 * Create empty instance
	 */
	public ContentRepositoryResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public ContentRepositoryResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with response info and cr
	 * @param contentRepository cr
	 * @param responseInfo response info
	 */
	public ContentRepositoryResponse(ContentRepositoryModel contentRepository, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setContentRepository(contentRepository);
	}

	/**
	 * ContentRepository
	 * @return the CR
	 */
	public ContentRepositoryModel getContentRepository() {
		return contentRepository;
	}

	/**
	 * Set the ContentRepository
	 * @param contentRepository CR
	 */
	public void setContentRepository(ContentRepositoryModel contentRepository) {
		this.contentRepository = contentRepository;
	}
}
