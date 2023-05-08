package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;

/**
 * Response for a list of referenced pages
 */
@XmlRootElement
public class ReferencedPagesListResponse extends LegacyPageListResponse {

	/**
	 * Number of using pages, the user is not allowed to see
	 */
	private int withoutPermission = 0;

	/**
	 * Total number of using pages
	 */
	private int total = 0;

	/**
	 * Create an instance
	 * @param message
	 * @param responseInfo
	 * @param pages
	 * @param total
	 * @param withoutPermission
	 */
	public ReferencedPagesListResponse(Message message, ResponseInfo responseInfo, List<Page> pages, int total, int withoutPermission) {
		super(message, responseInfo);
		setPages(pages);
		setTotal(total);
		setWithoutPermission(withoutPermission);

	}

	/**
	 * Number of pages without permission
	 * @return number of pages without permission
	 */
	public int getWithoutPermission() {
		return withoutPermission;
	}

	/**
	 * Total number of pages
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
