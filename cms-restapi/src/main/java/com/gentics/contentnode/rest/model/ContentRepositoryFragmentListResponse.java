package com.gentics.contentnode.rest.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of ContentRepository Fragments
 */
@XmlRootElement
public class ContentRepositoryFragmentListResponse extends AbstractListResponse<ContentRepositoryFragmentModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1791881370084826852L;

	/**
	 * ContentRepository Fragments in the list
	 * @return list of ContentRepository Fragments
	 */
	@Override
	public List<ContentRepositoryFragmentModel> getItems() {
		return super.getItems();
	}

	/**
	 * Set the ContentRepository Fragments
	 * @param items list of ContentRepository Fragments
	 */
	@Override
	public void setItems(List<ContentRepositoryFragmentModel> items) {
		super.setItems(items);
	}
}
