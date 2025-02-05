package com.gentics.contentnode.rest.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of ContentRepositories
 */
@XmlRootElement
public class ContentRepositoryListResponse extends AbstractListResponse<ContentRepositoryModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3246336999712984412L;

	/**
	 * ContentRepositories in the list
	 * @return list of ContentRepositories
	 */
	@Override
	public List<ContentRepositoryModel> getItems() {
		return super.getItems();
	}

	/**
	 * Set the ContentRepositories
	 * @param items list of ContentRepositories
	 */
	@Override
	public void setItems(List<ContentRepositoryModel> items) {
		super.setItems(items);
	}
}
