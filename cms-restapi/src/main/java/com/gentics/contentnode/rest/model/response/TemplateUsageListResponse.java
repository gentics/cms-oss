package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Template;

/**
 * Response for a page usage response (templates).
 */
@XmlRootElement
public class TemplateUsageListResponse extends TemplateListResponse {

	/**
	 * Number of using templates, the user is not allowed to see
	 */
	private int withoutPermission = 0;

	/**
	 * Total number of using templates
	 */
	private int total = 0;

	/**
	 * Empty constructor
	 */
	public TemplateUsageListResponse() {}

	/**
	 * Create a new instance
	 * @param message message
	 * @param responseInfo response info
	 * @param templates templates
	 * @param total total number of templates
	 * @param withoutPermission number of templates without permission
	 */
	public TemplateUsageListResponse(Message message, ResponseInfo responseInfo, List<Template> templates, int total, int withoutPermission) {
		super(message, responseInfo);
		setTemplates(templates);
		setTotal(total);
		setWithoutPermission(withoutPermission);
	}

	/**
	 * Get the number of templates without permission
	 * @return number of templates without permission
	 */
	public int getWithoutPermission() {
		return withoutPermission;
	}

	/**
	 * Get the total number of templates
	 * @return total number of templates
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set the number of templates without permission
	 * @param withoutPermission number of templates without permission
	 */
	public void setWithoutPermission(int withoutPermission) {
		this.withoutPermission = withoutPermission;
	}

	/**
	 * Set the total number of templates
	 * @param total total number of templates
	 */
	public void setTotal(int total) {
		this.total = total;
	}
}
