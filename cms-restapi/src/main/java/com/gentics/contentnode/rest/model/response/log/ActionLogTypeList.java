package com.gentics.contentnode.rest.model.response.log;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Response containing a list of logged object types
 */
 @XmlRootElement
public class ActionLogTypeList extends AbstractListResponse<ActionLogType> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4455500828141511537L;
}
