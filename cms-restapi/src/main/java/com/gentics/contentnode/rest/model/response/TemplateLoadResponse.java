package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Template;

/**
* Response for a template load request.
*/
@XmlRootElement
public class TemplateLoadResponse extends GenericResponse {

	/**
	 * Template object that contains the loaded template
	 */
	private Template template;

	/**
	 * Empty constructor needed by JAXB
	 */
	public TemplateLoadResponse() {}

	/**
	 * Creates a TemplateLoadResponse with the provided single message and ResponseInfo.
	 * 
	 * @param message
	 *            The messages that should be displayed to the user
	 * @param response
	 *            ResponseInfo with the status of the response
	 * @param template
	 *            The template that should be returned
	 */
	public TemplateLoadResponse(Message message, ResponseInfo responseInfo, Template template) {
		super(message, responseInfo);
		this.template = template;
	}

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

}
