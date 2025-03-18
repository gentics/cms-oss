package com.gentics.contentnode.rest.model.response.log;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of logged actions
 */
@XmlRootElement
public class ActionModelList extends AbstractListResponse<ActionModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3514658460410150146L;
}
