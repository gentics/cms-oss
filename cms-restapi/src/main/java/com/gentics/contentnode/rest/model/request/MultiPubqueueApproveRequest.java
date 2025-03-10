package com.gentics.contentnode.rest.model.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to approve multiple pages in queue
 */
@XmlRootElement
public class MultiPubqueueApproveRequest {
	private List<Integer> ids = new ArrayList<>();

	/**
	 * Page IDs for pages to approve
	 * @return
	 */
	public List<Integer> getIds() {
		return ids;
	}

	/**
	 * Set page IDs
	 * @param ids page IDs
	 */
	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}
}
