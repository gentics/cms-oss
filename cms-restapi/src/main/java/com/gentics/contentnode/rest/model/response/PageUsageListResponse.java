package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;

/**
 * Response for a page usage request.
 */
@XmlRootElement
public class PageUsageListResponse extends LegacyPageListResponse {

	/**
	 * Number of using pages, the user is not allowed to see
	 */
	private int withoutPermission = 0;

	/**
	 * Total number of using pages
	 */
	private int total = 0;

	/**
	 * Empty constructor
	 */
	public PageUsageListResponse() {}

	/**
	 * Create a new instance
	 * @param message message
	 * @param responseInfo response info
	 * @param pages pages
	 * @param total total number of pages
	 * @param withoutPermission number of pages without permission
	 */
	public PageUsageListResponse(Message message, ResponseInfo responseInfo, List<Page> pages, int total, int withoutPermission) {
		super(message, responseInfo);
		setPages(pages);
		setTotal(total);
		setWithoutPermission(withoutPermission);
	}

	/**
	 * Get the number of pages without permission
	 * @return number of pages without permission
	 */
	public int getWithoutPermission() {
		return withoutPermission;
	}

	/**
	 * Get the total number of pages
	 * @return total number of pages
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set the number of pages without permission
	 * @param withoutPermission number of pages without permission
	 */
	public void setWithoutPermission(int withoutPermission) {
		this.withoutPermission = withoutPermission;
	}

	/**
	 * Set the total number of pages
	 * @param total total number of pages
	 */
	public void setTotal(int total) {
		this.total = total;
	}
}
