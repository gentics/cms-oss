package com.gentics.contentnode.rest.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of Portal Implementations
 */
@XmlRootElement
public class PortalImplementationListResponse extends AbstractListResponse<PortalImplementationModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3518472265590237612L;

	/**
	 * Portal Implementations in the list
	 * @return list of Portal Implementations
	 */
	@Override
	public List<PortalImplementationModel> getItems() {
		return super.getItems();
	}

	/**
	 * Set the Portal Implementations
	 * @param items list of Portal Implementations
	 */
	@Override
	public void setItems(List<PortalImplementationModel> items) {
		super.setItems(items);
	}
}
