package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.TemplateInNode;

/**
 * Response containing a list of templates assigned to nodes
 */
@XmlRootElement
public class TemplateInNodeResponse extends AbstractListResponse<TemplateInNode> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4260126166451343569L;

	/**
	 * Create empty instance
	 */
	public TemplateInNodeResponse() {
		super();
	}

	/**
	 * Create instance
	 * @param message message
	 * @param responseInfo response info
	 */
	public TemplateInNodeResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

}
