package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;

/**
 * Response containing a ContentRepository Fragment
 */
@XmlRootElement
public class ContentRepositoryFragmentResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8663242910289737727L;

	private ContentRepositoryFragmentModel contentRepositoryFragment;

	/**
	 * Create empty instance
	 */
	public ContentRepositoryFragmentResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public ContentRepositoryFragmentResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with response info and cr fragment
	 * @param contentRepositoryFragment cr
	 * @param responseInfo response info
	 */
	public ContentRepositoryFragmentResponse(ContentRepositoryFragmentModel contentRepositoryFragment, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setContentRepositoryFragment(contentRepositoryFragment);
	}

	/**
	 * ContentRepository Fragment
	 * @return the CR Fragment
	 */
	public ContentRepositoryFragmentModel getContentRepositoryFragment() {
		return contentRepositoryFragment;
	}

	/**
	 * Set the ContentRepository Fragment
	 * @param contentRepositoryFragment CR Fragment
	 */
	public void setContentRepositoryFragment(ContentRepositoryFragmentModel contentRepositoryFragment) {
		this.contentRepositoryFragment = contentRepositoryFragment;
	}
}
