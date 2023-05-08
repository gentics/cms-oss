package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.MarkupLanguage;

/**
 * Response containing a list of markup languages
 */
@XmlRootElement
public class MarkupLanguageListResponse extends AbstractListResponse<MarkupLanguage> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4216525077060382159L;

	/**
	 * Create empty instance
	 */
	public MarkupLanguageListResponse() {
		super();
	}

	/**
	 * Create instance
	 * @param message message
	 * @param responseInfo response info
	 */
	public MarkupLanguageListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}
}
